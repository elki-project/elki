package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.RangeSelection;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGHyperCube;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.SelectionChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ProjectedThumbnail;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;

/**
 * Factory for visualizers to generate an SVG-Element containing a cube as
 * marker representing the selected range for each dimension
 * 
 * @author Heidi Kolb
 * 
 * @apiviz.has SelectionCubeVisualization oneway - - produces
 * @apiviz.uses SVGHyperCube
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class SelectionCubeVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Selection Range";

  /**
   * OptionID for {@link #NOFILL_FLAG}.
   */
  public static final OptionID NOFILL_ID = OptionID.getOrCreateOptionID("selectionrange.nofill", "Use wireframe style for selection ranges.");

  /**
   * Flag for half-transparent filling of selection cubes.
   * 
   * <p>
   * Key: {@code -selectionrange.nofill}
   * </p>
   */
  private final Flag NOFILL_FLAG = new Flag(NOFILL_ID);

  /**
   * Fill parameter.
   */
  protected boolean nofill = false;

  /**
   * Constructor, Parameterizable style
   */
  public SelectionCubeVisualizer(Parameterization config) {
    super(NAME);
    config = config.descend(this);
    if(config.grab(NOFILL_FLAG)) {
      nofill = NOFILL_FLAG.getValue();
    }
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   */
  @Override
  public void init(VisualizerContext<? extends NV> context) {
    super.init(context);
  }

  @Override
  public Visualization visualize(SVGPlot svgp, Projection2D proj, double width, double height) {
    return new SelectionCubeVisualization(context, svgp, proj, width, height);
  }

  @Override
  public Visualization makeThumbnail(SVGPlot svgp, Projection2D proj, double width, double height, int tresolution) {
    return new ProjectedThumbnail<NV, Projection2D>(this, context, svgp, proj, width, height, tresolution, ThumbnailVisualization.ON_DATA | ThumbnailVisualization.ON_SELECTION);
  }

  /**
   * Visualizer for generating an SVG-Element containing a cube as marker
   * representing the selected range for each dimension
   * 
   * @author Heidi Kolb
   * 
   * @apiviz.has de.lmu.ifi.dbs.elki.result.RangeSelection oneway - - visualizes
   */
  public class SelectionCubeVisualization extends Projection2DVisualization<NV> implements ContextChangeListener {
    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String MARKER = "selectionCubeMarker";

    /**
     * CSS class for the filled cube
     */
    public static final String CSS_CUBE = "selectionCube";

    /**
     * CSS class for the cube frame
     */
    public static final String CSS_CUBEFRAME = "selectionCubeFrame";

    /**
     * The actual visualization instance, for a single projection
     * 
     * @param context The context
     * @param svgp The SVGPlot
     * @param proj The Projection
     * @param width The width
     * @param height The height
     */
    public SelectionCubeVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, Projection2D proj, double width, double height) {
      super(context, svgp, proj, width, height, Visualizer.LEVEL_DATA - 1);
      addCSSClasses(svgp);
      context.addContextChangeListener(this);
      incrementalRedraw();
    }

    /**
     * Adds the required CSS-Classes
     * 
     * @param svgp SVG-Plot
     */
    private void addCSSClasses(SVGPlot svgp) {
      final StyleLibrary style = context.getStyleLibrary();
      // Class for the cube
      if(!svgp.getCSSClassManager().contains(CSS_CUBE)) {
        CSSClass cls = new CSSClass(this, CSS_CUBE);
        cls.setStatement(SVGConstants.CSS_STROKE_VALUE, style.getColor(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_STROKE_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT));
        cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        if(nofill) {
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        }
        else {
          cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getColor(StyleLibrary.SELECTION));
          cls.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
        }
        svgp.addCSSClassOrLogError(cls);
      }
      // Class for the cube frame
      if(!svgp.getCSSClassManager().contains(CSS_CUBEFRAME)) {
        CSSClass cls = new CSSClass(this, CSS_CUBEFRAME);
        cls.setStatement(SVGConstants.CSS_STROKE_VALUE, style.getColor(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_STROKE_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.SELECTION));

        svgp.addCSSClassOrLogError(cls);
      }
    }

    /**
     * Generates a cube and a frame depending on the selection stored in the
     * context
     * 
     * @param svgp The plot
     * @param proj The projection
     */
    private void setSVGRect(SVGPlot svgp, Projection2D proj) {
      DBIDSelection selContext = context.getSelection();
      if(selContext instanceof RangeSelection) {
        DoubleDoublePair[] ranges = ((RangeSelection) selContext).getRanges();
        int dim = DatabaseUtil.dimensionality(context.getDatabase());

        double[] min = new double[dim];
        double[] max = new double[dim];
        for(int d = 0; d < dim; d++) {
          if(ranges != null && ranges[d] != null) {
            min[d] = ranges[d].first;
            max[d] = ranges[d].second;
          }
          else {
            min[d] = proj.getScale(d).getMin();
            max[d] = proj.getScale(d).getMax();
          }
        }
        if(nofill) {
          Element r = SVGHyperCube.drawFrame(svgp, proj, new Vector(min), new Vector(max));
          SVGUtil.setCSSClass(r, CSS_CUBEFRAME);
          layer.appendChild(r);
        }
        else {
          Element r = SVGHyperCube.drawFilled(svgp, CSS_CUBE, proj, new Vector(min), new Vector(max));
          layer.appendChild(r);
        }

      }
    }

    @Override
    protected boolean testRedraw(ContextChangedEvent e) {
      return super.testRedraw(e) || (e instanceof SelectionChangedEvent);
    }

    @Override
    protected void redraw() {
      DBIDSelection selContext = context.getSelection();
      if(selContext != null && selContext instanceof RangeSelection) {
        setSVGRect(svgp, proj);
      }
    }
  }
}