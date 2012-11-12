package experimentalcode.erich.jogl;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;
import javax.swing.event.MouseInputAdapter;

import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.CovarianceDimensionSimilarity;
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.DimensionSimilarity;
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.DimensionSimilarityMatrix;
import de.lmu.ifi.dbs.elki.math.geometry.PrimsMinimumSpanningTree;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.ScalesResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
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
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Simple JOGL2 based parallel coordinates visualization.
 * 
 * @author Erich Schubert
 * 
 * TODO: FPSAnimator is currently unused. Remove.
 */
public class OpenGL3DParallelCoordinates implements ResultHandler {
  /**
   * Logging class.
   */
  private static final Logging LOG = Logging.getLogger(ResultHandler.class);

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    StyleResult style = getStyleResult(baseResult);
    Iterator<Relation<? extends NumberVector<?>>> iter = VisualizerUtil.iterateVectorFieldRepresentations(newResult);
    while (iter.hasNext()) {
      Relation<? extends NumberVector<?>> rel = iter.next();
      ScalesResult scales = ResultUtil.getScalesResult(rel);
      ProjectionParallel proj = new SimpleParallel(scales.getScales());
      new Instance(rel, proj, new Settings(), style).run();
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
     * Alpha effect: 1=solid, 0=full alpha effect
     * 
     * Note: since OpenGL usually works with 8 bit color depth, this puts a
     * strong limitation on alpha: computation is not in floats, but has 1/256
     * steps for blending. This in particular affects sloped lines!
     */
    double alpha = .5;

    /**
     * Line width
     */
    public float linewidth = 2f;

    /**
     * Similarity measure
     */
    DimensionSimilarity<NumberVector<?>> sim = CovarianceDimensionSimilarity.STATIC;
  }

  public static void computePositions(Node rootnode, int depth, double aoff, double awid, Node[] nodes) {
    rootnode.x = Math.sin(aoff + awid / 2) * depth * .5;
    rootnode.y = Math.cos(aoff + awid / 2) * depth * .5;
    {
      double cpos = aoff;
      double cwid = (awid / (rootnode.fanout));
      for (Node c : rootnode.children) {
        computePositions(c, depth + 1, cpos, cwid * c.fanout, nodes);
        cpos += cwid * c.fanout;
      }
    }
    nodes[rootnode.dim] = rootnode;
  }

  public static Node buildTree(int[] msg, int cur, int parent) {
    // Count the number of children:
    int c = 0;
    for (int i = 0; i < msg.length; i += 2) {
      if (msg[i] == cur && msg[i + 1] != parent) {
        c++;
      }
      if (msg[i + 1] == cur && msg[i] != parent) {
        c++;
      }
    }
    // Build children:
    int fanout = 0;
    Node[] chi = (c > 0) ? new Node[c] : Node.EMPTY;
    for (int i = 0; i < msg.length; i += 2) {
      if (msg[i] == cur && msg[i + 1] != parent) {
        c--;
        chi[c] = buildTree(msg, msg[i + 1], cur);
        fanout += chi[c].fanout;
      }
      if (msg[i + 1] == cur && msg[i] != parent) {
        c--;
        chi[c] = buildTree(msg, msg[i], cur);
        fanout += chi[c].fanout;
      }
    }
    assert (c == 0);
    Node n = new Node(cur, chi, Math.max(1, fanout));
    return n;
  }

  /**
   * Find the "optimal" root of a spanning tree. Optimal in the sense of: one of
   * the most central nodes.
   * 
   * This uses a simple message passing approach. Every node that has only one
   * unset neighbor will emit a message to this neighbor. The node last to emit
   * wins.
   * 
   * @param msg Minimum spanning graph.
   * @return
   */
  public static int findOptimalRoot(int[] msg) {
    final int size = (msg.length >> 1) + 1;

    int[] depth = new int[size];
    int[] missing = new int[size];

    // We shouldn't need more iterations in any case ever.
    int root = -1;
    for (int i = 1; i < size; i++) {
      boolean active = false;
      for (int e = 0; e < msg.length; e += 2) {
        if (depth[msg[e]] == 0) {
          missing[msg[e + 1]]++;
        }
        if (depth[msg[e + 1]] == 0) {
          missing[msg[e]]++;
        }
      }
      for (int n = 0; n < size; n++) {
        if (depth[n] == 0 && missing[n] <= 1) {
          depth[n] = i;
          root = n;
          active = true;
        }
      }
      if (!active) {
        break;
      }
      Arrays.fill(missing, 0); // Clean up.
    }
    return root;
  }

  /**
   * Minimalistic representation of the tree.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Node {
    /**
     * Empty array.
     */
    public static final Node[] EMPTY = new Node[0];

    /**
     * Constructor.
     * 
     * @param dim Node number
     * @param children Children
     * @param fanout Fanout
     */
    public Node(int dim, Node[] children, int fanout) {
      this.dim = dim;
      this.children = children;
      this.fanout = fanout;
    }

    /**
     * Dimension represented by this node.
     */
    public int dim;

    /**
     * Weight (fanout needed)
     */
    public int fanout;

    /**
     * Position in plot
     */
    public double x, y;

    /**
     * Child nodes.
     */
    public Node[] children;
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
     * Node hashmap.
     */
    Node[] nodes;

    private Node rootnode;

    private int[] edges;

    /**
     * JOGL animator
     */
    private FPSAnimator animator;

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
      caps.setDoubleBuffered(true);
      canvas = new GLCanvas(caps);
      canvas.addGLEventListener(this);
      arcball = new Arcball1DOFAdapter();
      canvas.addMouseListener(arcball);
      canvas.addMouseMotionListener(arcball);
      canvas.addMouseWheelListener(arcball);
      animator = new FPSAnimator(canvas, 20);
      animator.add(canvas);

      frame = new JFrame("EKLI OpenGL Visualization");
      frame.setSize(600, 600);
      frame.add(canvas);
    }

    public void run() {
      layoutParallel();

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

      prenderer.setupVertexBuffer(gl);
      textrenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));

      animator.start();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      camera.ratio = width / (double) height;
    }

    private void layoutParallel() {
      int dim = RelationUtil.dimensionality(rel);
      DimensionSimilarityMatrix mat = DimensionSimilarityMatrix.make(dim);
      settings.sim.computeDimensionSimilarites(rel, rel.getDBIDs(), mat);
      // Minimum spanning tree (as graph)
      edges = PrimsMinimumSpanningTree.processDense(mat, DimensionSimilarityMatrix.PRIM_ADAPTER);
      int root = findOptimalRoot(edges);
      rootnode = buildTree(edges, root, -1);
      nodes = new Node[dim];
      computePositions(rootnode, 0, 0, MathUtil.TWOPI, nodes);
    }

    /**
     * Class for a simple camera. Restricted: always looks at 0,0,0 from a
     * position defined by rotationX, distance and height.
     * 
     * For rotationX = 0, the camera will be at y=distance, x=0, so that the
     * default view will have the usual X/Y plane on the ground.
     * 
     * @author Erich Schubert
     */
    public static class Simple1DOFCamera {
      /**
       * Rotation on X axis.
       */
      private double rotationZ = 0.;

      /**
       * Distance
       */
      public double distance = 4;

      /**
       * Height
       */
      public double height = 2;

      /**
       * Screen ratio
       */
      public double ratio = 1.0;

      /**
       * GLU viewport storage
       */
      private int[] viewp = new int[4];

      /**
       * GLU model view matrix
       */
      private double[] modelview = new double[16];

      /**
       * GLU projection matrix
       */
      private double[] projection = new double[16];

      /**
       * GLU utility
       */
      private GLU glu;

      /**
       * Cache the Z rotation cosine
       */
      private double cosZ;

      /**
       * Cache the Z rotation sine
       */
      private double sinZ;

      /**
       * Constructor.
       * 
       * @param glu GLU utility class
       */
      public Simple1DOFCamera(GLU glu) {
        super();
        this.glu = glu;
        viewp = new int[4];
        modelview = new double[16];
        projection = new double[16];
        // Initial angle:
        rotationZ = 0;
        cosZ = 1.0;
        sinZ = 0.0;
      }

      /**
       * Copy constructor, for freezing a camera position.
       */
      public Simple1DOFCamera(Simple1DOFCamera other) {
        super();
        this.rotationZ = other.rotationZ;
        this.distance = other.distance;
        this.height = other.height;
        this.ratio = other.ratio;
        this.viewp = other.viewp.clone();
        this.modelview = other.modelview.clone();
        this.projection = other.projection.clone();
        this.glu = other.glu;
      }

      /**
       * Get the Z rotation in radians.
       * 
       * @return Z rotation angle (radians)
       */
      public double getRotationZ() {
        return rotationZ;
      }

      /**
       * Set the z rotation angle in radians.
       * 
       * @param rotationZ Z rotation angle.
       */
      public void setRotationZ(double rotationZ) {
        this.rotationZ = rotationZ;
        this.cosZ = Math.cos(rotationZ);
        this.sinZ = Math.sin(rotationZ);
      }

      /**
       * Apply the camera to a GL context.
       * 
       * @param gl GL context.
       */
      public void apply(GL2 gl) {
        // 3D projection
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        // Perspective.
        glu.gluPerspective(35, ratio, 1, 1000);
        glu.gluLookAt(distance * sinZ, distance * -cosZ, height, // pos
            0, 0, 0, // center
            0, 0, 1 // up
        );
        // Change back to model view matrix.
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        // Store the matrixes for reference.
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewp, 0);
        gl.glGetDoublev(GLMatrixFunc.GL_MODELVIEW_MATRIX, modelview, 0);
        gl.glGetDoublev(GLMatrixFunc.GL_PROJECTION_MATRIX, projection, 0);
      }

      /**
       * Unproject a screen coordinate (at depth 0) to 3D model coordinates.
       * 
       * @param x X
       * @param y Y
       * @return model coordinates
       */
      public double[] unproject(double x, double y, double z) {
        double[] out = new double[3];
        unproject(x, y, z, out);
        return out;
      }

      /**
       * Unproject a screen coordinate (at depth 0) to 3D model coordinates.
       * 
       * @param x X
       * @param y Y
       * @param Out output buffer
       */
      public void unproject(double x, double y, double z, double[] out) {
        glu.gluUnProject(x, y, z, modelview, 0, projection, 0, viewp, 0, out, 0);
      }

      /**
       * Project a coordinate
       * 
       * @param vec Input vector buffer
       * @param Out output buffer
       */
      public void project(double x, double y, double z, double[] out) {
        glu.gluProject(x, y, z, modelview, 0, projection, 0, viewp, 0, out, 0);
      }
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
      animator.stop();
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
       * Vertex buffer IDs
       */
      int[] vbi = null;

      /**
       * Number of quads in buffer
       */
      int lines = -1;

      /**
       * Sizes of the individual clusters
       */
      int[] sizes;

      /**
       * Colors
       */
      float[] colors;

      private int size;

      private void setupVertexBuffer(GL2 gl) {
        final int dim = RelationUtil.dimensionality(rel);
        size = rel.size();
        final float sz = (float) (1 / StyleLibrary.SCALE);

        int lines = (edges.length >> 1) * size;

        // Setup buffer IDs:
        int[] vbi = new int[1];
        gl.glGenBuffers(1, vbi, 0);
        // Vertexes
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbi[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, lines * 2 * 3 * ByteArrayUtil.SIZE_FLOAT, null, GL2.GL_DYNAMIC_DRAW);
        ByteBuffer vbytebuffer = gl.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2.GL_WRITE_ONLY);
        FloatBuffer vertices = vbytebuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();

        StylingPolicy sp = style.getStylingPolicy();
        ColorLibrary cols = style.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
        if (sp instanceof ClassStylingPolicy) {
          ClassStylingPolicy csp = (ClassStylingPolicy) sp;
          final int maxStyle = csp.getMaxStyle();
          TIntArrayList sizes = new TIntArrayList();
          TFloatArrayList colors = new TFloatArrayList();

          int csum = 0; //
          for (int s = csp.getMinStyle(); s < maxStyle; s++) {
            // Count the number of instances in the style
            int c = 0;
            for (DBIDIter it = csp.iterateClass(s); it.valid(); it.advance(), c++, csum++) {
              int coff = (csum << 2) + (csum << 1); // * 6
              double[] vec = proj.fastProjectDataToRenderSpace(rel.get(it));
              for (int i = 0; i < edges.length; i += 2, coff += (size << 2) + (size << 1)) {
                // Seek to appropriate position.
                // See buffer layout discussed above.
                vertices.position(coff);
                final int d0 = edges[i], d1 = edges[i + 1];
                vertices.put((float) nodes[d0].x);
                vertices.put((float) nodes[d0].y);
                vertices.put(1.f - sz * (float) vec[d0]);
                vertices.put((float) nodes[d1].x);
                vertices.put((float) nodes[d1].y);
                vertices.put(1.f - sz * (float) vec[d1]);
              }
            }
            // Skip empty classes
            if (c == 0) {
              continue;
            }
            sizes.add(c);
            Color col = SVGUtil.stringToColor(cols.getColor(s));
            colors.add(col.getRed() / 255.f);
            colors.add(col.getGreen() / 255.f);
            colors.add(col.getBlue() / 255.f);
          }
          this.sizes = sizes.toArray();
          this.colors = colors.toArray();
        } else {
          // TODO: single color.
        }

        vertices.flip();
        gl.glUnmapBuffer(GL.GL_ARRAY_BUFFER);
        this.lines = lines;
        this.vbi = vbi; // Store

        // Labels:
        labels = new String[dim];
        for (int i = 0; i < dim; i++) {
          labels[i] = RelationUtil.getColumnLabel(rel, i);
        }
      }

      public void dispose(GL gl) {
        // Free vertex buffers
        if (vbi != null) {
          gl.glDeleteBuffers(vbi.length, vbi, 0);
        }
      }

      protected void drawParallelPlot(GLAutoDrawable drawable, final int dim, GL2 gl) {
        gl.glPushMatrix();
        // gl.glRotatef((float) MathUtil.rad2deg(rotation), 0.f, 0.f, 1.f);
        // Enable shading
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_BLEND);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glLineWidth(settings.linewidth);

        // Bind vertex buffer.
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbi[0]);
        // Use 2D coordinates
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL.GL_FLOAT, 3 * ByteArrayUtil.SIZE_FLOAT, 0);

        // See buffer layout above!

        // Simple Z sorting for edge groups
        DoubleIntPair[] depth = new DoubleIntPair[edges.length >> 1];
        {
          double[] buf = new double[3];
          double[] z = new double[dim];
          for (int d = 0; d < dim; d++) {
            camera.project(nodes[d].x, nodes[d].y, 0, buf);
            z[d] = buf[2];
          }
          for (int e = 0, e2 = 0; e2 < edges.length; e++, e2 += 2) {
            depth[e] = new DoubleIntPair(-(z[edges[e2]] + z[edges[e2 + 1]]), e);
          }
          Arrays.sort(depth);
        }

        for (int e = 0; e < depth.length; e++) {
          final int eoff = depth[e].second * (size << 1);

          // Within each block, we go by colors:
          for (int i = 0, off = 0; i < sizes.length; i++) {
            // Setup color
            final float alpha = (float) (settings.alpha + (1 - settings.alpha) / Math.sqrt(rel.size()));
            gl.glColor4f(colors[i * 3], colors[i * 3 + 1], colors[i * 3 + 2], alpha);

            // Execute
            final int size2 = sizes[i] << 1;
            gl.glDrawArrays(GL2.GL_LINES, eoff + off, size2);
            off += size2;
          }
        }
        // Unbind buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glPopMatrix();

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
          float axisdist = 1; // (float) ratio / (dim - 1f);
          float defaultscale = .025f / dim;
          float targetwidth = 0.95f * axisdist;
          for (int i = 0; i < dim; i++) {
            Rectangle2D b = textrenderer.getBounds(labels[i]);
            float scale = defaultscale;
            if (b.getWidth() * scale > targetwidth) {
              scale = targetwidth / (float) b.getWidth();
            }
            float w = (float) b.getWidth() * scale;
            // Rotate manually, in x-z plane
            float x = (float) (cos * nodes[i].x + sin * nodes[i].y);
            float y = (float) (-sin * nodes[i].x + cos * nodes[i].y);
            textrenderer.draw3D(labels[i], (x - w * .5f), 1.01f, -y, scale);
          }
          textrenderer.end3DRendering();
          gl.glPopMatrix();
        }
      }
    }

    /**
     * Arcball style helper.
     * 
     * @author Erich Schubert
     */
    public class Arcball1DOFAdapter extends MouseInputAdapter {
      /**
       * Debug flag.
       */
      private static final boolean DEBUG = false;

      /**
       * Starting point of drag.
       */
      private double[] startvec = new double[3];

      /**
       * Ending point of drag.
       */
      private double[] endvec = new double[3];

      /**
       * Temp buffer we use for computations.
       */
      double[] tmp = new double[3];

      /**
       * Initial camera rotation
       */
      private double initialrot;

      /**
       * Starting angle for dragging.
       */
      double startangle;

      /**
       * Camera that was in use when the drag started.
       */
      private Simple1DOFCamera startcamera;

      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        int s = e.getWheelRotation();
        for (; s >= 1; s--) {
          camera.distance += .1;
        }
        for (; s <= -1; s++) {
          if (camera.distance > .15) {
            camera.distance -= .1;
          }
        }
        canvas.display();
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        if (animator.isPaused()) {
          animator.start();
        } else {
          animator.pause();
        }
      }

      @Override
      public void mousePressed(MouseEvent e) {
        // Start drag.
        startcamera = new Simple1DOFCamera(camera);

        Point startPoint = e.getPoint();
        mapMouseToPlane(startcamera, startPoint, startvec);
        initialrot = startcamera.getRotationZ();
        startangle = Math.atan2(startvec[1], startvec[0]);
      }

      /**
       * Map the coordinates. Note: vec will be overwritten!
       * 
       * @param camera Camera
       * @param point2d Input point
       * @param vec Output vector
       */
      private void mapMouseToPlane(Simple1DOFCamera camera, Point point2d, double[] vec) {
        double[] far = new double[3], near = new double[3];
        // Far plane
        camera.unproject(point2d.x, point2d.y, 0., far);
        // Near plane
        camera.unproject(point2d.x, point2d.y, 1., near);
        // Delta vector: far -= near.
        VMath.minusEquals(far, near);
        // Intersection with z=0 plane:
        // far.z - a * near.z = 0 -> a = far.z / near.z
        if (near[2] < 0 || near[2] > 0) {
          double a = far[2] / near[2];
          vec[0] = far[0] - a * near[0];
          vec[1] = far[1] - a * near[1];
          vec[2] = 0;
        }
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        mapMouseToPlane(startcamera, e.getPoint(), endvec);
        double upangle = Math.atan2(endvec[1], endvec[0]);
        camera.setRotationZ(initialrot + (upangle - startangle));
        canvas.display();
        // TODO: add full arcball support?
      }

      @SuppressWarnings("unused")
      public void debugRender(GL2 gl) {
        if (!DEBUG || (startcamera == null)) {
          return;
        }
        gl.glLineWidth(3f);
        gl.glColor4f(1.f, 0.f, 0.f, .66f);
        gl.glBegin(GL.GL_LINES);
        gl.glVertex3f(0.f, 0.f, 0.f);
        gl.glVertex3f((float) Math.cos(-initialrot + startangle) * 4.f, (float) -Math.sin(-initialrot + startangle) * 4.f, 0.f);
        gl.glVertex3f((float) Math.cos(-initialrot + startangle) * 1.f, (float) -Math.sin(-initialrot + startangle) * 1.f, 0.f);
        gl.glVertex3f((float) Math.cos(-initialrot + startangle) * 1.f, (float) -Math.sin(-initialrot + startangle) * 1.f, 1.f);
        gl.glEnd();
      }
    }
  }
}
