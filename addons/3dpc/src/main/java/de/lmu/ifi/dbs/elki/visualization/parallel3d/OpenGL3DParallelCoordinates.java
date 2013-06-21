package de.lmu.ifi.dbs.elki.visualization.parallel3d;

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

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
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
import javax.swing.SwingUtilities;

import com.jogamp.opengl.util.awt.TextRenderer;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.DimensionSimilarity;
import de.lmu.ifi.dbs.elki.math.dimensionsimilarity.DimensionSimilarityMatrix;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.ScalesResult;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.visualization.parallel3d.layout.Layout;
import de.lmu.ifi.dbs.elki.visualization.parallel3d.layout.Layouter3DPC;
import de.lmu.ifi.dbs.elki.visualization.parallel3d.layout.SimilarityBasedLayouter3DPC;
import de.lmu.ifi.dbs.elki.visualization.parallel3d.layout.SimpleCircularMSTLayout3DPC;
import de.lmu.ifi.dbs.elki.visualization.parallel3d.util.Arcball1DOFAdapter;
import de.lmu.ifi.dbs.elki.visualization.parallel3d.util.Simple1DOFCamera;
import de.lmu.ifi.dbs.elki.visualization.parallel3d.util.Simple1DOFCamera.CameraListener;
import de.lmu.ifi.dbs.elki.visualization.parallel3d.util.SimpleMenuOverlay;
import de.lmu.ifi.dbs.elki.visualization.parallel3d.util.SimpleMessageOverlay;
import de.lmu.ifi.dbs.elki.visualization.projections.ProjectionParallel;
import de.lmu.ifi.dbs.elki.visualization.projections.SimpleParallel;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.PropertiesBasedStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleResult;

/**
 * Simple JOGL2 based parallel coordinates visualization.
 * 
 * TODO: Improve generics of Layout3DPC.
 * 
 * TODO: Generalize to multiple relations and non-numeric feature vectors.
 * 
 * FIXME: proper depth-sorting of edges. It's not that simple, unfortunately.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
@Alias({ "3dpc", "3DPC" })
public class OpenGL3DParallelCoordinates<O extends NumberVector<?>> implements ResultHandler {
  /**
   * Logging class.
   */
  private static final Logging LOG = Logging.getLogger(OpenGL3DParallelCoordinates.class);

  /**
   * Settings
   */
  Settings<O> settings = new Settings<>();

  /**
   * Constructor.
   * 
   * @param layout Layout
   */
  public OpenGL3DParallelCoordinates(Layouter3DPC<? super O> layout) {
    settings.layout = layout;
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    boolean nonefound = true;
    StyleResult style = getStyleResult(baseResult);
    List<Relation<?>> rels = ResultUtil.getRelations(newResult);
    for (Relation<?> rel : rels) {
      if (!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Relation<? extends O> vrel = (Relation<? extends O>) rel;
      ScalesResult scales = ResultUtil.getScalesResult(vrel);
      ProjectionParallel proj = new SimpleParallel(scales.getScales());
      new Instance<>(vrel, proj, settings, style).run();
      nonefound = false;
    }
    if (nonefound && baseResult.equals(newResult)) {
      LOG.warning("3DPC did not find a number vector field relation to visualize!");
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
   * 
   * @param <O> Object type
   */
  public static class Settings<O> {
    /**
     * Similarity measure in use.
     */
    public DimensionSimilarity<? super O> sim;

    /**
     * Layouting method.
     */
    public Layouter3DPC<? super O> layout;

    /**
     * Line width.
     */
    public float linewidth = 2f;

    /**
     * Texture width.
     */
    public int texwidth = 1 << 8;

    /**
     * Texture height.
     */
    public int texheight = 1 << 10;

    /**
     * Number of additional mipmaps to generate.
     */
    public int mipmaps = 1;
  }

  /**
   * Visualizer instance.
   * 
   * @author Erich Schubert
   * 
   * @param <O> Object type
   */
  public static class Instance<O extends NumberVector<?>> implements GLEventListener {
    /**
     * Flag to enable debug rendering.
     */
    static final boolean DEBUG = false;

    /**
     * Frame
     */
    JFrame frame = null;

    /**
     * GLU utility class.
     */
    GLU glu;

    /**
     * 3D parallel coordinates renderer.
     */
    private Parallel3DRenderer<O> prenderer;

    /**
     * The OpenGL canvas
     */
    GLCanvas canvas;

    /**
     * Arcball controller.
     */
    Arcball1DOFAdapter arcball;

    /**
     * Menu overlay.
     */
    SimpleMenuOverlay menuOverlay;

    /**
     * Message overlay.
     */
    SimpleMessageOverlay messageOverlay;

    /**
     * Handler to open the menu.
     */
    MouseAdapter menuStarter;

    /**
     * Current state.
     */
    State state = State.PREPARATION;

    /**
     * States of the UI.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    protected static enum State { //
      PREPARATION, // Preparation phase
      EXPLORE, // Exploration phase (rotate etc.)
      MENU, // Menu open
    }

    /**
     * Shared data for visualization modules.
     * 
     * @author Erich Schubert
     * 
     * @param <O> Relation data type
     */
    protected static class Shared<O> {
      /**
       * Dimensionality.
       */
      int dim;

      /**
       * Relation to visualize
       */
      Relation<? extends O> rel;

      /**
       * Axis labels
       */
      String[] labels;

      /**
       * Projection
       */
      ProjectionParallel proj;

      /**
       * Style result
       */
      StyleResult style;

      /**
       * Layout
       */
      Layout layout;

      /**
       * Settings
       */
      Settings<O> settings;

      /**
       * Camera handling class
       */
      Simple1DOFCamera camera;

      /**
       * Text renderer
       */
      TextRenderer textrenderer;

      /**
       * Current similarity matrix.
       */
      DimensionSimilarityMatrix mat;
    };

    Shared<O> shared = new Shared<>();

    /**
     * Constructor.
     * 
     * @param rel Relation
     * @param proj Projection
     * @param settings Settings
     * @param style Style result
     */
    public Instance(Relation<? extends O> rel, ProjectionParallel proj, Settings<O> settings, StyleResult style) {
      super();

      this.shared.dim = RelationUtil.dimensionality(rel);
      this.shared.rel = rel;
      this.shared.proj = proj;
      this.shared.style = style;
      this.shared.settings = settings;
      // Labels:
      this.shared.labels = new String[this.shared.dim];
      {
        VectorFieldTypeInformation<? extends O> vrel = RelationUtil.assumeVectorField(rel);
        for (int i = 0; i < this.shared.dim; i++) {
          this.shared.labels[i] = vrel.getLabel(i);
        }
      }

      this.prenderer = new Parallel3DRenderer<>(shared);
      this.menuOverlay = new SimpleMenuOverlay() {
        @Override
        public void menuItemClicked(int item) {
          if (item >= 0) {
            LOG.debug("Relayout chosen: " + menuOverlay.getOptions().get(item));
            relayout(menuOverlay.getOptions().get(item));
          } else {
            closeMenu();
          }
        }
      };
      this.menuStarter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (State.EXPLORE.equals(state)) {
            if (e.getButton() == MouseEvent.BUTTON3) {
              switchState(State.MENU);
              // e.consume();
            }
          }
        }
      };
      this.messageOverlay = new SimpleMessageOverlay();

      // Init menu for SIGMOD demo. TODO: make more flexible.
      for (Class<?> clz : InspectionUtil.cachedFindAllImplementations(DimensionSimilarity.class)) {
        menuOverlay.getOptions().add(clz.getSimpleName());
      }

      GLProfile glp = GLProfile.getDefault();
      GLCapabilities caps = new GLCapabilities(glp);
      caps.setDoubleBuffered(true);
      canvas = new GLCanvas(caps);
      canvas.addGLEventListener(this);

      frame = new JFrame("ELKI 3D Parallel Coordinate Visualization");
      frame.setSize(600, 600);
      frame.add(canvas);
    }

    void initLabels() {
      // Labels:
      shared.labels = new String[shared.dim];
      for (int i = 0; i < shared.dim; i++) {
        shared.labels[i] = RelationUtil.getColumnLabel(shared.rel, i);
      }
    }

    @SuppressWarnings("unchecked")
    protected void relayout(String simname) {
      try {
        final Class<?> simc = InspectionUtil.findImplementation(DimensionSimilarity.class, simname);
        shared.settings.sim = ClassGenericsUtil.tryInstantiate(DimensionSimilarity.class, simc, new EmptyParameterization());
        if (!(shared.settings.layout instanceof SimilarityBasedLayouter3DPC)) {
          ListParameterization params = new ListParameterization();
          params.addParameter(SimilarityBasedLayouter3DPC.SIM_ID, shared.settings.sim);
          shared.settings.layout = ClassGenericsUtil.tryInstantiate(Layouter3DPC.class, SimpleCircularMSTLayout3DPC.class, params);
        }
        // Clear similarity matrix:
        shared.mat = null;
        switchState(State.PREPARATION);
        startLayoutThread();
      } catch (Exception e) {
        LOG.exception(e);
        return;
      }
    }

    private void startLayoutThread() {
      new Thread() {
        @Override
        public void run() {
          messageOverlay.setMessage("Computing axis similarities and layout...");
          if (shared.settings.sim != null && shared.settings.layout instanceof SimilarityBasedLayouter3DPC) {
            @SuppressWarnings("unchecked")
            final SimilarityBasedLayouter3DPC<O> layouter = (SimilarityBasedLayouter3DPC<O>) shared.settings.layout;
            if (shared.mat == null) {
              LOG.debug("Recomputing similarity matrix.");
              shared.mat = DimensionSimilarityMatrix.make(shared.dim);
              shared.settings.sim.computeDimensionSimilarites(shared.rel.getDatabase(), shared.rel, shared.rel.getDBIDs(), shared.mat);
            }
            LOG.debug("Recomputing layout using similarity matrix.");
            final Layout newlayout = layouter.layout(shared.dim, shared.mat);
            setLayout(newlayout);
          } else {
            LOG.debug("Recomputing layout.");
            final Layout newlayout = shared.settings.layout.layout(shared.rel.getDatabase(), shared.rel);
            setLayout(newlayout);
          }
        }
      }.start();
    }

    protected void closeMenu() {
      // TODO: which state to return to?
      state = State.EXPLORE;
    }

    public void run() {
      assert (frame != null);
      frame.setVisible(true);
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
          stop();
        }
      });
      startLayoutThread();
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
      shared.camera = new Simple1DOFCamera(glu);
      shared.camera.addCameraListener(new CameraListener() {
        @Override
        public void cameraChanged() {
          canvas.display();
        }
      });

      // Setup arcball:
      arcball = new Arcball1DOFAdapter(shared.camera);
      shared.textrenderer = new TextRenderer(new Font(Font.SANS_SERIF, Font.BOLD, 36));
      // Ensure listeners.
      switchState(state);
    }

    /**
     * Switch the current state.
     * 
     * @param newstate State to switch to.
     */
    void switchState(State newstate) {
      // Reset mouse listeners
      canvas.removeMouseListener(menuStarter);
      canvas.removeMouseListener(menuOverlay);
      canvas.removeMouseListener(arcball);
      canvas.removeMouseMotionListener(arcball);
      canvas.removeMouseWheelListener(arcball);
      switch(newstate) {
      case EXPLORE: {
        canvas.addMouseListener(menuStarter);
        canvas.addMouseListener(arcball);
        canvas.addMouseMotionListener(arcball);
        canvas.addMouseWheelListener(arcball);
        break;
      }
      case MENU: {
        canvas.addMouseListener(menuOverlay);
        break;
      }
      case PREPARATION: {
        // No listeners.
        break;
      }
      }
      if (state != newstate) {
        this.state = newstate;
        canvas.repaint();
      }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      shared.camera.setRatio(width / (double) height);
      messageOverlay.setSize(width, height);
      menuOverlay.setSize(width, height);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
      GL2 gl = drawable.getGL().getGL2();
      gl.glClear(GL.GL_COLOR_BUFFER_BIT /* | GL.GL_DEPTH_BUFFER_BIT */);

      if (shared.layout != null) {
        int res = prenderer.prepare(gl);
        if (res == 1) {
          // Request a repaint, to generate the next texture.
          canvas.repaint();
        }
        messageOverlay.setMessage("Texture rendering complete.");
        if (res == 2) {
          switchState(State.EXPLORE);
        }
      }

      shared.camera.apply(gl);
      if (shared.layout != null) {
        prenderer.drawParallelPlot(drawable, gl);
      }

      if (DEBUG) {
        arcball.debugRender(gl);
      }

      if (State.MENU.equals(state)) {
        menuOverlay.render(gl);
      }
      if (State.PREPARATION.equals(state)) {
        messageOverlay.render(gl);
      }
    }

    /**
     * Callback from layouting thread.
     * 
     * @param newlayout New layout.
     */
    protected void setLayout(final Layout newlayout) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          shared.layout = newlayout;
          prenderer.forgetTextures(null);
          messageOverlay.setMessage("Rendering Textures.");
          canvas.repaint();
        }
      });
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
      GL gl = drawable.getGL();
      prenderer.forgetTextures(gl);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <O> Object type
   */
  public static class Parameterizer<O extends NumberVector<?>> extends AbstractParameterizer {
    /**
     * Option for layouting method
     */
    public static final OptionID LAYOUT_ID = new OptionID("parallel3d.layout", "Layouting method for 3DPC.");

    /**
     * Similarity measure
     */
    Layouter3DPC<O> layout;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<Layouter3DPC<O>> layoutP = new ObjectParameter<>(LAYOUT_ID, Layouter3DPC.class, SimpleCircularMSTLayout3DPC.class);
      if (config.grab(layoutP)) {
        layout = layoutP.instantiateClass(config);
      }
    }

    @Override
    protected OpenGL3DParallelCoordinates<O> makeInstance() {
      return new OpenGL3DParallelCoordinates<>(layout);
    }
  }
}
