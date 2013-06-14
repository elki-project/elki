package experimentalcode.erich.jogl;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;

import com.jogamp.opengl.util.awt.TextRenderer;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.ScalesResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.projections.ProjectionParallel;
import de.lmu.ifi.dbs.elki.visualization.projections.SimpleParallel;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.PropertiesBasedStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleResult;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import experimentalcode.erich.jogl.Simple1DOFCamera.CameraListener;
import experimentalcode.shared.parallelcoord.layout.Layout;
import experimentalcode.shared.parallelcoord.layout.Layout.Node;
import experimentalcode.shared.parallelcoord.layout.Layouter3DPC;
import experimentalcode.shared.parallelcoord.layout.SimpleCircularMSTLayout;

/**
 * Simple JOGL2 based parallel coordinates visualization.
 * 
 * @author Erich Schubert
 * 
 *         TODO: Improve generics of Layout3DPC.
 */
public class OpenGL3DParallelCoordinates implements ResultHandler {
  /**
   * Logging class.
   */
  private static final Logging LOG = Logging.getLogger(ResultHandler.class);

  Settings settings = new Settings();

  /**
   * Constructor.
   * 
   * @param layout Layout
   */
  public OpenGL3DParallelCoordinates(Layouter3DPC<? super NumberVector<?>> layout) {
    settings.layout = layout;
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    StyleResult style = getStyleResult(baseResult);
    List<Relation<?>> rels = ResultUtil.getRelations(newResult);
    for (Relation<?> rel : rels) {
      if (!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Relation<? extends NumberVector<?>> vrel = (Relation<? extends NumberVector<?>>) rel;
      ScalesResult scales = ResultUtil.getScalesResult(vrel);
      ProjectionParallel proj = new SimpleParallel(scales.getScales());
      new Instance(vrel, proj, settings, style).run();
    }
  }

  /**
   * Hack: Get/Create the style result.
   * 
   * @return Style result
   */
  public StyleResult getStyleResult(HierarchicalResult result) {
    ArrayList<StyleResult> styles = ResultUtil.filterResults(result, StyleResult.class);
    if (styles.size() > 0) {
      return styles.get(0);
    }
    StyleResult styleresult = new StyleResult();
    styleresult.setStyleLibrary(new PropertiesBasedStyleLibrary());
    ResultUtil.ensureClusteringResult(ResultUtil.findDatabase(result), result);
    List<Clustering<? extends Model>> clusterings = ResultUtil.getClusteringResults(result);
    if (clusterings.size() > 0) {
      styleresult.setStylingPolicy(new ClusterStylingPolicy(clusterings.get(0), styleresult.getStyleLibrary()));
      result.getHierarchy().add(result, styleresult);
      return styleresult;
    } else {
      throw new AbortException("No clustering result generated?!?");
    }
  }

  /**
   * Class keeping the visualizer settings.
   * 
   * @author Erich Schubert
   */
  public static class Settings {
    /**
     * Layouting method.
     */
    public Layouter3DPC<? super NumberVector<?>> layout;

    /**
     * Line width.
     */
    public float linewidth = 2f;

    /**
     * Texture width.
     */
    public int texwidth = 256;

    /**
     * Texture height.
     */
    public int texheight = 1024;
  }

  /**
   * Visualizer instance.
   * 
   * @author Erich Schubert
   */
  public static class Instance implements GLEventListener {
    /**
     * Flag to enable debug rendering.
     */
    private static final boolean DEBUG = false;

    /**
     * Relation to viualize
     */
    Relation<? extends NumberVector<?>> rel;

    /**
     * Projection
     */
    ProjectionParallel proj;

    /**
     * Frame
     */
    JFrame frame = null;

    /**
     * GLU utility class.
     */
    GLU glu;

    /**
     * Settings
     */
    Settings settings;

    /**
     * Style result
     */
    StyleResult style;

    /**
     * Axis labels
     */
    String[] labels;

    /**
     * Layout
     */
    private Layout layout;

    private Parallel3DRenderer prenderer;

    /**
     * Text renderer
     */
    TextRenderer textrenderer;

    /**
     * The OpenGL canvas
     */
    GLCanvas canvas;

    /**
     * Camera handling class
     */
    Simple1DOFCamera camera;

    /**
     * Arcball controller.
     */
    Arcball1DOFAdapter arcball;

    /**
     * Constructor.
     * 
     * @param rel Relation
     * @param proj Projection
     * @param settings Settings
     * @param style Style result
     */
    public Instance(Relation<? extends NumberVector<?>> rel, ProjectionParallel proj, Settings settings, StyleResult style) {
      super();
      this.rel = rel;
      this.proj = proj;
      this.settings = settings;
      this.style = style;
      this.prenderer = new Parallel3DRenderer();

      GLProfile glp = GLProfile.getDefault();
      GLCapabilities caps = new GLCapabilities(glp);
      // Increase color depth.
      // caps.setBlueBits(16);
      // caps.setRedBits(16);
      // caps.setGreenBits(16);
      caps.setDoubleBuffered(true);
      canvas = new GLCanvas(caps);
      canvas.addGLEventListener(this);

      frame = new JFrame("EKLI OpenGL Visualization");
      frame.setSize(600, 600);
      frame.add(canvas);
    }

    public void run() {
      layout = settings.layout.layout(rel.getDatabase(), rel);

      assert (frame != null);
      frame.setVisible(true);
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
          stop();
        }
      });
    }

    public void stop() {
      frame = null;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
      GL2 gl = drawable.getGL().getGL2();
      if (DEBUG) {
        gl = new DebugGL2(gl);
        drawable.setGL(gl);
      }
      // As we aren't really rendering models, but just drawing,
      // We do not need to set up a lot.
      gl.glClearColor(1f, 1f, 1f, 1f);
      gl.glDisable(GL.GL_DEPTH_TEST);
      gl.glDisable(GL.GL_CULL_FACE);

      glu = new GLU();
      camera = new Simple1DOFCamera(glu);
      camera.addCameraListener(new CameraListener() {
        @Override
        public void cameraChanged() {
          canvas.display();
        }
      });

      // Setup arcball:
      arcball = new Arcball1DOFAdapter(camera);
      canvas.addMouseListener(arcball);
      canvas.addMouseMotionListener(arcball);
      canvas.addMouseWheelListener(arcball);

      prenderer.initLabels();
      long start = System.nanoTime();
      prenderer.renderTextures(gl);
      long end = System.nanoTime();
      LOG.warning("Time to render textures: " + (end - start) / 1e6 + " ms.");
      textrenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      camera.setRatio(width / (double) height);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
      GL2 gl = drawable.getGL().getGL2();
      gl.glClear(GL.GL_COLOR_BUFFER_BIT /* | GL.GL_DEPTH_BUFFER_BIT */);

      camera.apply(gl);

      final int dim = RelationUtil.dimensionality(rel);
      prenderer.drawParallelPlot(drawable, dim, gl);

      if (DEBUG) {
        arcball.debugRender(gl);
      }
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
      GL gl = drawable.getGL();
      prenderer.dispose(gl);
    }

    /**
     * Renderer for 3D parallel plots.
     * 
     * The tricky part here is the vertex buffer layout. We are drawing lines,
     * so we need two vertices for each macro edge (edge between axes in the
     * plot). We furthermore need the following properties: we need to draw
     * edges sorted by depth to allow alpha and smoothing to work, and we need
     * to be able to have different colors for clusters. An efficient batch
     * therefore will consist of one edge-color combination. The input data
     * comes in color-object ordering, so we need to seek through the edges when
     * writing the buffer.
     * 
     * In total, we have 2 * obj.size * edges.size vertices.
     * 
     * Where obj.size = sum(col.sizes)
     * 
     * @author Erich Schubert
     */
    class Parallel3DRenderer {
      /**
       * Prerendered textures.
       */
      private int[] textures;

      void initLabels() {
        int dim = RelationUtil.dimensionality(rel);
        // Labels:
        labels = new String[dim];
        for (int i = 0; i < dim; i++) {
          labels[i] = RelationUtil.getColumnLabel(rel, i);
        }
      }

      void renderTextures(GL2 gl) {
        final StylingPolicy sp = style.getStylingPolicy();
        final ColorLibrary cols = style.getStyleLibrary().getColorSet(StyleLibrary.PLOT);

        // Setup color table:
        float[] colors;
        int lines = 0;
        if (sp instanceof ClassStylingPolicy) {
          ClassStylingPolicy csp = (ClassStylingPolicy) sp;
          final int maxStyle = csp.getMaxStyle();
          colors = new float[maxStyle * 3];
          for (int c = 0, s = csp.getMinStyle(); s < maxStyle; c += 3, s++) {
            Color col = SVGUtil.stringToColor(cols.getColor(s));
            colors[c + 0] = col.getRed() / 255.f;
            colors[c + 1] = col.getGreen() / 255.f;
            colors[c + 2] = col.getBlue() / 255.f;
            lines = Math.max(lines, csp.classSize(s));
          }
        } else {
          // Render in black.
          colors = new float[] { 0f, 0f, 0f };
          lines = rel.size();
        }

        // Setup buffer IDs:
        int[] vbi = new int[1];
        gl.glGenBuffers(1, vbi, 0);
        // Buffer for coordinates.
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbi[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, lines * 2 * 2 * ByteArrayUtil.SIZE_FLOAT, null, GL2.GL_DYNAMIC_DRAW);

        // Generate textures:
        textures = new int[layout.edges.size()];
        gl.glGenTextures(textures.length, textures, 0);

        // Get a framebuffer:
        int[] frameBufferID = new int[1];
        gl.glGenFramebuffers(1, frameBufferID, 0);

        gl.glPushAttrib(GL2.GL_TEXTURE_BIT | GL2.GL_VIEWPORT_BIT);
        gl.glPushMatrix();
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferID[0]);

        for (int edge = 0; edge < textures.length; edge++) {
          Layout.Edge e = layout.edges.get(edge);

          gl.glBindTexture(GL.GL_TEXTURE_2D, textures[edge]);
          gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
          gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
          gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
          gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
          gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);

          // Reserve texture image data:
          gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL2.GL_RGBA16, //
              settings.texwidth, settings.texheight, 0, // Size
              GL2.GL_RGBA, GL2.GL_FLOAT, null);
          gl.glGenerateMipmap(GL.GL_TEXTURE_2D); // Allocate mipmaps!
          gl.glViewport(0, 0, settings.texwidth, settings.texheight);

          // Attach 2D texture to this FBO
          gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, //
              GL.GL_TEXTURE_2D, textures[edge], 0);

          if (gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER) != GL2.GL_FRAMEBUFFER_COMPLETE) {
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

          gl.glLineWidth(settings.linewidth);

          if (sp instanceof ClassStylingPolicy) {
            ClassStylingPolicy csp = (ClassStylingPolicy) sp;
            final int maxStyle = csp.getMaxStyle();
            for (int c = 0, s = csp.getMinStyle(); s < maxStyle; c += 3, s++) {
              ByteBuffer vbytebuffer = gl.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2.GL_WRITE_ONLY);
              FloatBuffer vertices = vbytebuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
              int p = 0;
              for (DBIDIter it = csp.iterateClass(s); it.valid(); it.advance(), p++) {
                final NumberVector<?> vec = rel.get(it);
                final float v1 = (float) proj.fastProjectDataToRenderSpace(vec.doubleValue(e.dim1), e.dim1);
                final float v2 = (float) proj.fastProjectDataToRenderSpace(rel.get(it).doubleValue(e.dim2), e.dim2);
                vertices.put(0.f);
                vertices.put(v1);
                vertices.put(1.f);
                vertices.put(v2);
              }
              assert (p == csp.classSize(s));
              vertices.flip();
              gl.glUnmapBuffer(GL.GL_ARRAY_BUFFER);

              gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbi[0]);
              gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
              gl.glVertexPointer(2, GL.GL_FLOAT, 0, 0);

              gl.glColor3f(colors[c], colors[c + 1], colors[c + 2]);
              gl.glDrawArrays(GL.GL_LINES, 0, p);

              gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
            }
          } else {
            ByteBuffer vbytebuffer = gl.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2.GL_WRITE_ONLY);
            FloatBuffer vertices = vbytebuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
            int p = 0;
            for (DBIDIter it = rel.iterDBIDs(); it.valid(); it.advance(), p++) {
              final NumberVector<?> vec = rel.get(it);
              final float v1 = (float) proj.fastProjectDataToRenderSpace(vec.doubleValue(e.dim1), e.dim1);
              final float v2 = (float) proj.fastProjectDataToRenderSpace(rel.get(it).doubleValue(e.dim2), e.dim2);
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

          gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_BASE_LEVEL, 0);
          gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAX_LEVEL, 2);
          gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
          gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
          gl.glHint(GL.GL_GENERATE_MIPMAP_HINT, GL.GL_NICEST);
          gl.glGenerateMipmap(GL.GL_TEXTURE_2D);
          gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

          if (!gl.glIsTexture(textures[0])) {
            LOG.warning("Generating texture failed!");
          }
        }
        // Switch back to the default framebuffer.
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
        gl.glPopMatrix();
        gl.glPopAttrib();

        // Free vertex buffer
        gl.glDeleteBuffers(vbi.length, vbi, 0);

        // Reset background color.
        gl.glClearColor(1f, 1f, 1f, 1f);
      }

      public void dispose(GL gl) {
        if (textures != null) {
          gl.glDeleteTextures(textures.length, textures, 0);
          textures = null;
        }
      }

      protected void drawParallelPlot(GLAutoDrawable drawable, final int dim, GL2 gl) {
        if (textures != null) {
          gl.glPushMatrix();
          // Simple Z sorting for edge groups
          DoubleIntPair[] depth = new DoubleIntPair[layout.edges.size()];
          {
            double[] buf = new double[3];
            double[] z = new double[dim];
            for (int d = 0; d < dim; d++) {
              camera.project(layout.getNode(d).getX(), layout.getNode(d).getY(), 0, buf);
              z[d] = buf[2];
            }
            int e = 0;
            for (Layout.Edge edge : layout.edges) {
              depth[e] = new DoubleIntPair(-(z[edge.dim1] + z[edge.dim2]), e);
              e++;
            }
            Arrays.sort(depth);
          }
          gl.glShadeModel(GL2.GL_FLAT);
          // Render spider web:
          gl.glBegin(GL.GL_LINES);
          gl.glLineWidth(settings.linewidth);
          gl.glColor4f(0f, 0f, 0f, 1f);
          for (Layout.Edge edge : layout.edges) {
            Node n1 = layout.getNode(edge.dim1), n2 = layout.getNode(edge.dim2);
            gl.glVertex3d(n1.getX(), n1.getY(), 0f);
            gl.glVertex3d(n2.getX(), n2.getY(), 0f);
          }
          gl.glEnd();
          gl.glEnable(GL.GL_TEXTURE_2D);
          gl.glColor4f(1f, 1f, 1f, 1f);
          for (DoubleIntPair pair : depth) {
            Layout.Edge edge = layout.edges.get(pair.second);
            final Node node1 = layout.getNode(edge.dim1);
            final Node node2 = layout.getNode(edge.dim2);

            gl.glBindTexture(GL.GL_TEXTURE_2D, textures[pair.second]);
            gl.glBegin(GL2.GL_QUADS);
            gl.glTexCoord2d(0f, 0f);
            gl.glVertex3d(node1.getX(), node1.getY(), 0f);
            gl.glTexCoord2d(0f, 1f);
            gl.glVertex3d(node1.getX(), node1.getY(), 1f);
            gl.glTexCoord2d(1f, 1f);
            gl.glVertex3d(node2.getX(), node2.getY(), 1f);
            gl.glTexCoord2d(1f, 0f);
            gl.glVertex3d(node2.getX(), node2.getY(), 0f);
            gl.glEnd();
          }
          gl.glDisable(GL.GL_TEXTURE_2D);
          gl.glPopMatrix();
        }
        // Render labels
        {
          gl.glPushMatrix();
          // TODO: use 2d rendering, and {@link SimpleCamera.project}?
          textrenderer.begin3DRendering();
          // UNDO the camera rotation. This will mess up text orientation!
          gl.glRotatef((float) MathUtil.rad2deg(camera.getRotationZ()), 0.f, 0.f, 1.f);
          // Rotate to have the text face the camera direction, which looks +Y
          // While the text will be visible from +Z and +Y is baseline.
          gl.glRotatef(90.f, 1.f, 0.f, 0.f);
          // HalfPI: 180 degree extra rotation, for text orientation.
          double cos = Math.cos(camera.getRotationZ()), sin = Math.sin(camera.getRotationZ());

          textrenderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);
          float defaultscale = .01f / dim;
          float targetwidth = 0.2f; // TODO: div depth?
          for (int i = 0; i < dim; i++) {
            Rectangle2D b = textrenderer.getBounds(labels[i]);
            float scale = defaultscale;
            if (b.getWidth() * scale > targetwidth) {
              scale = targetwidth / (float) b.getWidth();
            }
            float w = (float) b.getWidth() * scale;
            // Rotate manually, in x-z plane
            float x = (float) (cos * layout.getNode(i).getX() + sin * layout.getNode(i).getY());
            float y = (float) (-sin * layout.getNode(i).getX() + cos * layout.getNode(i).getY());
            textrenderer.draw3D(labels[i], (x - w * .5f), 1.01f, -y, scale);
          }
          textrenderer.end3DRendering();
          gl.glPopMatrix();
        }
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Option for layouting method
     */
    public static final OptionID LAYOUT_ID = new OptionID("parallel3d.layout", "Layouting method for 3DPC.");

    /**
     * Similarity measure
     */
    Layouter3DPC<? super NumberVector<?>> layout;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<Layouter3DPC<? super NumberVector<?>>> layoutP = new ObjectParameter<>(LAYOUT_ID, Layouter3DPC.class, SimpleCircularMSTLayout.class);
      if (config.grab(layoutP)) {
        layout = layoutP.instantiateClass(config);
      }
    }

    @Override
    protected OpenGL3DParallelCoordinates makeInstance() {
      return new OpenGL3DParallelCoordinates(layout);
    }
  }
}
