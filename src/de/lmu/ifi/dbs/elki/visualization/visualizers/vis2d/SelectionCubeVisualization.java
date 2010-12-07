package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.RangeSelection;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
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
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.SelectionChangedEvent;

/**
 * Visualizer for generating an SVG-Element containing a cube as marker
 * representing the selected range for each dimension
 * 
 * @author Heidi Kolb
 * 
 * @apiviz.has SelectionResult oneway - - visualizes
 * @apiviz.has RangeSelection oneway - - visualizes
 * @apiviz.uses SVGHyperCube
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class SelectionCubeVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> implements ContextChangeListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Selection Range";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
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
   * Fill parameter.
   */
  protected boolean nofill = false;

  /**
   * The result we process
   */
  private SelectionResult result;

  public SelectionCubeVisualization(VisualizationTask task, boolean nofill) {
    super(task, VisFactory.LEVEL_DATA - 1);
    this.result = task.getResult();
    this.nofill = nofill;
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

  /**
   * Factory for visualizers to generate an SVG-Element containing a cube as
   * marker representing the selected range for each dimension
   * 
   * @author Heidi Kolb
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses SelectionCubeVisualization oneway - - «create»
   * 
   * @param <NV> vector type
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends P2DVisFactory<NV> {
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
    public Factory(Parameterization config) {
      super(NAME);
      config = config.descend(this);
      if(config.grab(NOFILL_FLAG)) {
        nofill = NOFILL_FLAG.getValue();
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new SelectionCubeVisualization<NV>(task, nofill);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, Result result) {
      final ArrayList<SelectionResult> selectionResults = ResultUtil.filterResults(result, SelectionResult.class);
      for(SelectionResult selres : selectionResults) {
        context.addVisualizer(selres, new VisualizationTask(context, selres, this));
      }
    }
  }
}