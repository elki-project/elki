/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.parallel3d;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.parallel3d.OpenGL3DParallelCoordinates.Instance.Shared;
import de.lmu.ifi.dbs.elki.visualization.parallel3d.layout.Layout;
import de.lmu.ifi.dbs.elki.visualization.parallel3d.layout.Layout.Node;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import net.jafama.FastMath;

/**
 * Renderer for 3D parallel plots.
 * <p>
 * The tricky part here is the vertex buffer layout. We are drawing lines, so we
 * need two vertices for each macro edge (edge between axes in the plot). We
 * furthermore need the following properties: we need to draw edges sorted by
 * depth to allow alpha and smoothing to work, and we need to be able to have
 * different colors for clusters. An efficient batch therefore will consist of
 * one edge-color combination. The input data comes in color-object ordering, so
 * we need to seek through the edges when writing the buffer.
 * <p>
 * In total, we have 2 * obj.size * edges.size vertices.
 * <p>
 * Where obj.size = sum(col.sizes)
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek:<br>
 * Interactive Data Mining with 3D-Parallel-Coordinate-Trees.<br>
 * Proc. 2013 ACM Int. Conf. on Management of Data (SIGMOD 2013)
 * <p>
 * TODO: generalize to non-numeric features and scales.
 *
 * @author Erich Schubert
 * @since 0.6.0
 * @param <O> Object type
 */
@Reference(authors = "Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek", //
    title = "Interactive Data Mining with 3D-Parallel-Coordinate-Trees", //
    booktitle = "Proc. 2013 ACM Int. Conf. on Management of Data (SIGMOD 2013)", //
    url = "https://doi.org/10.1145/2463676.2463696", //
    bibkey = "DBLP:conf/sigmod/AchtertKSZ13")
public class Parallel3DRenderer<O extends NumberVector> {
  /**
   * Logging class.
   */
  private static final Logging LOG = Logging.getLogger(Parallel3DRenderer.class);

  /**
   * Shared data.
   */
  Shared<O> shared;

  /**
   * Prerendered textures.
   */
  private int[] textures;

  /**
   * Number of completely rendered textures.
   */
  private int completedTextures = 0;

  /**
   * Depth indexes of axes.
   */
  private int[] dindex;

  /**
   * Color table.
   */
  float[] colors;

  /**
   * Axes sorting array.
   */
  DoubleIntPair[] axes;

  /**
   * Vertex buffer.
   */
  int[] vbi = new int[] { -1 };

  /**
   * Framebuffer for render-to-texture
   */
  int[] frameBufferID = new int[] { -1 };

  /**
   * Constructor.
   * 
   * @param shared Shared data.
   */
  protected Parallel3DRenderer(Shared<O> shared) {
    super();
    this.shared = shared;
    this.dindex = new int[shared.dim];
    axes = new DoubleIntPair[shared.dim];
    for(int i = 0; i < shared.dim; i++) {
      axes[i] = new DoubleIntPair(0.0, 0);
    }
  }

  protected int prepare(GL2 gl) {
    if(completedTextures < 0) {
      if(textures != null) {
        gl.glDeleteTextures(textures.length, textures, 0);
        textures = null;
      }
      completedTextures = 0;
    }
    if(completedTextures >= shared.layout.edges.size()) {
      return 0;
    }
    if(!LOG.isDebugging()) {
      renderTexture(gl, completedTextures);
    }
    else {
      long start = System.nanoTime();
      renderTexture(gl, completedTextures);
      long end = System.nanoTime();
      LOG.debug("Time to render texture: " + (end - start) / 1e6 + " ms.");
    }
    return (completedTextures < shared.layout.edges.size()) ? 1 : 2;
  }

  protected void drawParallelPlot(GLAutoDrawable drawable, GL2 gl) {
    // Sort axes by sq. distance from camera, front-to-back:
    sortAxes();
    // Sort edges by the maximum (foreground) index.
    IntIntPair[] edgesort = sortEdges(dindex);

    if(textures != null) {
      gl.glShadeModel(GL2.GL_FLAT);
      // Render spider web:
      gl.glLineWidth(shared.settings.linewidth); // outside glBegin!
      gl.glBegin(GL.GL_LINES);
      gl.glColor4f(0f, 0f, 0f, 1f);
      for(Layout.Edge edge : shared.layout.edges) {
        Node n1 = shared.layout.getNode(edge.dim1),
            n2 = shared.layout.getNode(edge.dim2);
        gl.glVertex3d(n1.getX(), n1.getY(), 0f);
        gl.glVertex3d(n2.getX(), n2.getY(), 0f);
      }
      gl.glEnd();
      // Draw axes and 3DPC:
      for(int i = 0; i < shared.dim; i++) {
        final int d = axes[i].second;
        final Node node1 = shared.layout.getNode(d);
        // Draw edge textures
        for(IntIntPair pair : edgesort) {
          // Not yet available?
          if(pair.second >= completedTextures) {
            continue;
          }
          // Other axis must have a smaller index.
          if(pair.first >= i) {
            continue;
          }
          Layout.Edge edge = shared.layout.edges.get(pair.second);
          // Must involve the current axis.
          if(edge.dim1 != d && edge.dim2 != d) {
            continue;
          }
          int od = axes[pair.first].second;

          gl.glEnable(GL.GL_TEXTURE_2D);
          gl.glColor4f(1f, 1f, 1f, 1f);
          final Node node2 = shared.layout.getNode(od);

          gl.glBindTexture(GL.GL_TEXTURE_2D, textures[pair.second]);
          gl.glBegin(GL2.GL_QUADS);
          gl.glTexCoord2d((edge.dim1 == d) ? 0f : 1f, 0f);
          gl.glVertex3d(node1.getX(), node1.getY(), 0f);
          gl.glTexCoord2d((edge.dim1 == d) ? 0f : 1f, 1f);
          gl.glVertex3d(node1.getX(), node1.getY(), 1f);
          gl.glTexCoord2d((edge.dim1 != d) ? 0f : 1f, 1f);
          gl.glVertex3d(node2.getX(), node2.getY(), 1f);
          gl.glTexCoord2d((edge.dim1 != d) ? 0f : 1f, 0f);
          gl.glVertex3d(node2.getX(), node2.getY(), 0f);
          gl.glEnd();
          gl.glDisable(GL.GL_TEXTURE_2D);
        }
        // Draw axis
        gl.glLineWidth(shared.settings.linewidth); // outside glBegin!
        gl.glBegin(GL.GL_LINES);
        gl.glColor4f(0f, 0f, 0f, 1f);
        gl.glVertex3d(node1.getX(), node1.getY(), 0f);
        gl.glVertex3d(node1.getX(), node1.getY(), 1f);
        gl.glEnd();
        // Draw ticks.
        LinearScale scale = shared.proj.getAxisScale(d);
        gl.glPointSize(shared.settings.linewidth * 2f);
        gl.glBegin(GL.GL_POINTS);
        for(double tick = scale.getMin(); tick <= scale.getMax() + scale.getRes() / 10; tick += scale.getRes()) {
          gl.glVertex3d(node1.getX(), node1.getY(), scale.getScaled(tick));
        }
        gl.glEnd();
      }
    }
    // Render labels
    renderLabels(gl, edgesort);
  }

  void renderTexture(GL2 gl, int edge) {
    assert (edge == completedTextures);
    // Setup color table:
    prepareColors(shared.stylepol);

    // Setup buffer IDs:
    if(vbi[0] < 0) {
      gl.glGenBuffers(1, vbi, 0);
      // Buffer for coordinates.
      gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbi[0]);
      gl.glBufferData(GL.GL_ARRAY_BUFFER, shared.rel.size() // Number of lines *
          * 2 // 2 Points *
          * 5 // 2 coordinates + 3 color
          * ByteArrayUtil.SIZE_FLOAT, null, GL2.GL_DYNAMIC_DRAW);
    }
    else {
      gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbi[0]);
    }

    // Generate textures:
    if(textures == null) {
      textures = new int[shared.layout.edges.size()];
      gl.glGenTextures(textures.length, textures, 0);
    }

    // Get a framebuffer:
    if(frameBufferID[0] < 0) {
      gl.glGenFramebuffers(1, frameBufferID, 0);
    }

    gl.glPushAttrib(GL2.GL_TEXTURE_BIT | GL2.GL_VIEWPORT_BIT);
    gl.glPushMatrix();
    gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferID[0]);

    {
      Layout.Edge e = shared.layout.edges.get(edge);

      gl.glBindTexture(GL.GL_TEXTURE_2D, textures[edge]);
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
      gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
      gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);

      // Reserve texture image data:
      gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL2.GL_RGBA16, //
          shared.settings.texwidth, shared.settings.texheight, 0, // Size
          GL2.GL_RGBA, GL2.GL_FLOAT, null);
      gl.glViewport(0, 0, shared.settings.texwidth, shared.settings.texheight);

      // Attach 2D texture to this FBO
      gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, //
          GL.GL_TEXTURE_2D, textures[edge], 0);

      if(gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER) != GL2.GL_FRAMEBUFFER_COMPLETE) {
        LOG.warning("glCheckFramebufferStatus: " + gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER));
      }

      gl.glDisable(GL2.GL_LIGHTING);
      gl.glDisable(GL.GL_CULL_FACE);
      gl.glDisable(GL.GL_DEPTH_TEST);
      gl.glMatrixMode(GL2.GL_PROJECTION);
      gl.glLoadIdentity();
      gl.glOrtho(0f, 1f, 0f, StyleLibrary.SCALE, -1, 1);
      gl.glMatrixMode(GL2.GL_MODELVIEW);
      gl.glLoadIdentity();

      gl.glClearColor(1f, 1f, 1f, .0f);
      gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

      gl.glShadeModel(GL2.GL_SMOOTH);
      gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
      gl.glEnable(GL.GL_BLEND);
      gl.glEnable(GL.GL_LINE_SMOOTH);

      gl.glLineWidth(shared.settings.linewidth);

      if(shared.stylepol instanceof ClassStylingPolicy) {
        ClassStylingPolicy csp = (ClassStylingPolicy) shared.stylepol;
        final int mincolor = csp.getMinStyle();
        ByteBuffer vbytebuffer = gl.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2.GL_WRITE_ONLY);
        FloatBuffer vertices = vbytebuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
        int p = 0;
        for(DBIDIter it = shared.rel.iterDBIDs(); it.valid(); it.advance(), p += 2) {
          final O vec = shared.rel.get(it);
          final int c = (csp.getStyleForDBID(it) - mincolor) * 3;
          final float v1 = (float) shared.proj.fastProjectDataToRenderSpace(vec.doubleValue(e.dim1), e.dim1);
          final float v2 = (float) shared.proj.fastProjectDataToRenderSpace(vec.doubleValue(e.dim2), e.dim2);
          vertices.put(0.f);
          vertices.put(v1);
          vertices.put(colors[c]);
          vertices.put(colors[c + 1]);
          vertices.put(colors[c + 2]);
          vertices.put(1.f);
          vertices.put(v2);
          vertices.put(colors[c]);
          vertices.put(colors[c + 1]);
          vertices.put(colors[c + 2]);
        }
        vertices.flip();
        gl.glUnmapBuffer(GL.GL_ARRAY_BUFFER);

        gl.glColor4f(1f, 1f, 1f, 1f);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbi[0]);
        gl.glVertexPointer(2, GL.GL_FLOAT, 5 * ByteArrayUtil.SIZE_FLOAT, 0);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glColorPointer(3, GL.GL_FLOAT, 5 * ByteArrayUtil.SIZE_FLOAT, 2 * ByteArrayUtil.SIZE_FLOAT);
        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
        gl.glDrawArrays(GL.GL_LINES, 0, p);

        gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
      }
      else {
        ByteBuffer vbytebuffer = gl.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2.GL_WRITE_ONLY);
        FloatBuffer vertices = vbytebuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
        int p = 0;
        for(DBIDIter it = shared.rel.iterDBIDs(); it.valid(); it.advance(), p += 2) {
          final O vec = shared.rel.get(it);
          final float v1 = (float) shared.proj.fastProjectDataToRenderSpace(vec.doubleValue(e.dim1), e.dim1);
          final float v2 = (float) shared.proj.fastProjectDataToRenderSpace(vec.doubleValue(e.dim2), e.dim2);
          vertices.put(0.f);
          vertices.put(v1);
          vertices.put(1.f);
          vertices.put(v2);
        }
        vertices.flip();
        gl.glUnmapBuffer(GL.GL_ARRAY_BUFFER);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbi[0]);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glVertexPointer(2, GL.GL_FLOAT, 0, 0);

        gl.glColor3f(colors[0], colors[1], colors[2]);
        gl.glDrawArrays(GL.GL_LINES, 0, p);

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
      }

      if(shared.settings.mipmaps > 0) {
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_BASE_LEVEL, 0);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAX_LEVEL, shared.settings.mipmaps);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
        gl.glHint(GL.GL_GENERATE_MIPMAP_HINT, GL.GL_NICEST);
        gl.glGenerateMipmap(GL.GL_TEXTURE_2D);
      }
      gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

      if(!gl.glIsTexture(textures[0])) {
        LOG.warning("Generating texture failed!");
      }
    }
    // Switch back to the default framebuffer.
    gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
    gl.glPopMatrix();
    gl.glPopAttrib();

    ++completedTextures;
    if(completedTextures == shared.layout.edges.size()) {
      // Free vertex buffer
      gl.glDeleteBuffers(vbi.length, vbi, 0);
      vbi[0] = -1;
      // Free framebuffer
      gl.glDeleteFramebuffers(1, frameBufferID, 0);
      frameBufferID[0] = -1;
    }
  }

  private void prepareColors(final StylingPolicy sp) {
    if(colors == null) {
      final ColorLibrary cols = shared.stylelib.getColorSet(StyleLibrary.PLOT);
      if(sp instanceof ClassStylingPolicy) {
        ClassStylingPolicy csp = (ClassStylingPolicy) sp;
        final int maxStyle = csp.getMaxStyle();
        colors = new float[maxStyle * 3];
        for(int c = 0, s = csp.getMinStyle(); s < maxStyle; c += 3, s++) {
          Color col = SVGUtil.stringToColor(cols.getColor(s));
          colors[c + 0] = col.getRed() / 255.f;
          colors[c + 1] = col.getGreen() / 255.f;
          colors[c + 2] = col.getBlue() / 255.f;
        }
      }
      else {
        // Render in black.
        colors = new float[] { 0f, 0f, 0f };
      }
    }
  }

  protected void forgetTextures(GL gl) {
    if(gl == null) {
      completedTextures = -1;
    }
    else {
      if(textures != null) {
        gl.glDeleteTextures(textures.length, textures, 0);
        textures = null;
      }
      completedTextures = 0;
    }
  }

  /**
   * Depth-sort the axes.
   */
  private void sortAxes() {
    for(int d = 0; d < shared.dim; d++) {
      double dist = shared.camera.squaredDistanceFromCamera(shared.layout.getNode(d).getX(), shared.layout.getNode(d).getY());
      axes[d].first = -dist;
      axes[d].second = d;
    }
    Arrays.sort(axes);
    for(int i = 0; i < shared.dim; i++) {
      dindex[axes[i].second] = i;
    }
  }

  /**
   * Sort the edges for rendering.
   * 
   * FIXME: THIS STILL HAS ERRORS SOMETIME!
   * 
   * @param dindex depth index of axes.
   * @return Sorted array of (minaxis, edgeid)
   */
  private IntIntPair[] sortEdges(int[] dindex) {
    IntIntPair[] edgesort = new IntIntPair[shared.layout.edges.size()];
    int e = 0;
    for(Layout.Edge edge : shared.layout.edges) {
      int i1 = dindex[edge.dim1], i2 = dindex[edge.dim2];
      edgesort[e] = new IntIntPair(Math.min(i1, i2), e);
      e++;
    }
    Arrays.sort(edgesort);
    return edgesort;
  }

  private void renderLabels(GL2 gl, IntIntPair[] edgesort) {
    shared.textrenderer.begin3DRendering();
    // UNDO the camera rotation. This will mess up text orientation!
    gl.glRotatef((float) MathUtil.rad2deg(shared.camera.getRotationZ()), 0.f, 0.f, 1.f);
    // Rotate to have the text face the camera direction, which looks +Y
    // While the text will be visible from +Z and +Y is baseline.
    gl.glRotatef(90.f, 1.f, 0.f, 0.f);
    // HalfPI: 180 degree extra rotation, for text orientation.
    double cos = FastMath.cos(shared.camera.getRotationZ()),
        sin = FastMath.sin(shared.camera.getRotationZ());

    shared.textrenderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);
    float defaultscale = .01f / (float) FastMath.sqrt(shared.dim);
    final float targetwidth = .2f; // TODO: div depth?
    final float minratio = 8.f; // Assume all text is at least this width
    for(int i = 0; i < shared.dim; i++) {
      if(shared.labels[i] != null) {
        Rectangle2D b = shared.textrenderer.getBounds(shared.labels[i]);
        float scale = defaultscale;
        if(Math.max(b.getWidth(), b.getHeight() * minratio) * scale > targetwidth) {
          scale = targetwidth / (float) Math.max(b.getWidth(), b.getHeight() * minratio);
        }
        float w = (float) b.getWidth() * scale;
        // Rotate manually, in x-z plane
        float x = (float) (cos * shared.layout.getNode(i).getX() + sin * shared.layout.getNode(i).getY());
        float y = (float) (-sin * shared.layout.getNode(i).getX() + cos * shared.layout.getNode(i).getY());
        shared.textrenderer.draw3D(shared.labels[i], (x - w * .5f), 1.01f, -y, scale);
      }
    }

    // Show depth indexes on debug:
    if(OpenGL3DParallelCoordinates.Instance.DEBUG) {
      shared.textrenderer.setColor(1f, 0f, 0f, 1f);
      for(IntIntPair pair : edgesort) {
        Layout.Edge edge = shared.layout.edges.get(pair.second);
        final Node node1 = shared.layout.getNode(edge.dim1);
        final Node node2 = shared.layout.getNode(edge.dim2);
        final double mx = 0.5 * (node1.getX() + node2.getX());
        final double my = 0.5 * (node1.getY() + node2.getY());
        // Rotate manually, in x-z plane
        float x = (float) (cos * mx + sin * my);
        float y = (float) (-sin * mx + cos * my);
        shared.textrenderer.draw3D(Integer.toString(pair.first), (x - defaultscale * .5f), 1.01f, -y, .5f * defaultscale);
      }
    }

    shared.textrenderer.end3DRendering();
  }
}
