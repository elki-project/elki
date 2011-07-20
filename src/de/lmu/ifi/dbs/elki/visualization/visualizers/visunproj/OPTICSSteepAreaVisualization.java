package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.awt.Color;
import java.util.Collection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICSXi;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICSXi.SteepAreaResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSDistanceAdapter;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;

/**
 * Visualize the steep areas found in an OPTICS plot
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses 
 *              de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICSXi.SteepAreaResult
 */
public class OPTICSSteepAreaVisualization<D extends Distance<D>> extends AbstractVisualization {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICS Steep Areas";

  /**
   * CSS class for markers
   */
  protected static final String CSS_STEEP_UP = "opticsSteepUp";

  /**
   * CSS class for markers
   */
  protected static final String CSS_STEEP_DOWN = "opticsSteepDown";

  /**
   * Our cluster order
   */
  private ClusterOrderResult<D> co;

  /**
   * Our clustering
   */
  OPTICSXi.SteepAreaResult areas;

  /**
   * The plot
   */
  private OPTICSPlot<D> opticsplot;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public OPTICSSteepAreaVisualization(VisualizationTask task) {
    super(task);
    this.co = task.getResult();
    this.areas = findSteepAreaResult(this.co);
    this.opticsplot = OPTICSPlot.plotForClusterOrder(this.co, context);
    context.addResultListener(this);
    incrementalRedraw();
  }

  /**
   * Find the OPTICS clustering child of a cluster order.
   * 
   * @param co Cluster order
   * @return OPTICS clustering
   */
  protected static OPTICSXi.SteepAreaResult findSteepAreaResult(ClusterOrderResult<?> co) {
    for(Result r : co.getHierarchy().getChildren(co)) {
      if(OPTICSXi.SteepAreaResult.class.isInstance(r)) {
        return (OPTICSXi.SteepAreaResult) r;
      }
    }
    return null;
  }

  @Override
  protected void redraw() {
    final double scale = StyleLibrary.SCALE;
    final double sizex = scale;
    final double sizey = scale * task.getHeight() / task.getWidth();
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), sizex, sizey, margin);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    addCSSClasses();

    final double plotwidth = scale;
    final double plotheight = scale / opticsplot.getRatio();

    OPTICSDistanceAdapter<D> adapter = opticsplot.getDistanceAdapter();
    for(OPTICSXi.SteepArea area : areas) {
      final int st = area.getStartIndex();
      final int en = area.getEndIndex();
      // Note: make sure we are using doubles!
      final double x1 = (st + .25) / this.co.getClusterOrder().size();
      final double x2 = (en + .75) / this.co.getClusterOrder().size();
      final double d1 = adapter.getDoubleForEntry(this.co.getClusterOrder().get(st));
      final double d2 = adapter.getDoubleForEntry(this.co.getClusterOrder().get(en));
      final double y1 = (!Double.isInfinite(d1) && !Double.isNaN(d1)) ? (1. - opticsplot.getScale().getScaled(d1)) : 0.;
      final double y2 = (!Double.isInfinite(d2) && !Double.isNaN(d2)) ? (1. - opticsplot.getScale().getScaled(d2)) : 0.;
      Element e = svgp.svgLine(plotwidth * x1, plotheight * y1, plotwidth * x2, plotheight * y2);
      if(area instanceof OPTICSXi.SteepDownArea) {
        SVGUtil.addCSSClass(e, CSS_STEEP_DOWN);
      }
      else {
        SVGUtil.addCSSClass(e, CSS_STEEP_UP);
      }
      layer.appendChild(e);
    }
  }

  /**
   * Adds the required CSS-Classes
   */
  private void addCSSClasses() {
    // Class for the markers
    if(!svgp.getCSSClassManager().contains(CSS_STEEP_DOWN)) {
      final CSSClass cls = new CSSClass(this, CSS_STEEP_DOWN);
      Color color = SVGUtil.stringToColor(context.getStyleLibrary().getColor(StyleLibrary.PLOT));
      if(color == null) {
        color = Color.BLACK;
      }
      color = new Color((int) (color.getRed() * 0.8), (int) (color.getGreen() * 0.8 + 0.2 * 256), (int) (color.getBlue() * 0.8));
      cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGUtil.colorToString(color));
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
      svgp.addCSSClassOrLogError(cls);
    }
    if(!svgp.getCSSClassManager().contains(CSS_STEEP_UP)) {
      final CSSClass cls = new CSSClass(this, CSS_STEEP_UP);
      Color color = SVGUtil.stringToColor(context.getStyleLibrary().getColor(StyleLibrary.PLOT));
      if(color == null) {
        color = Color.BLACK;
      }
      color = new Color((int) (color.getRed() * 0.8 + 0.2 * 256), (int) (color.getGreen() * 0.8), (int) (color.getBlue() * 0.8));
      cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGUtil.colorToString(color));
      cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
      svgp.addCSSClassOrLogError(cls);
    }
  }

  @Override
  public void resultChanged(Result current) {
    if(current instanceof SelectionResult || current == co || current == opticsplot) {
      synchronizedRedraw();
      return;
    }
    super.resultChanged(current);
  }

  /**
   * Factory class for OPTICS plot selections.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses OPTICSPlotSelectionVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Collection<OPTICSPlot<?>> plots = ResultUtil.filterResults(result, OPTICSPlot.class);
      for(OPTICSPlot<?> plot : plots) {
        ClusterOrderResult<?> co = plot.getClusterOrder();
        final SteepAreaResult steep = findSteepAreaResult(co);
        if(steep != null) {
          final VisualizationTask task = new VisualizationTask(NAME, co, null, this, plot);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
          task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
          baseResult.getHierarchy().add(steep, task);
        }
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new OPTICSSteepAreaVisualization<DoubleDistance>(task);
    }

    @Override
    public boolean allowThumbnails(@SuppressWarnings("unused") VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }

    @Override
    public Class<? extends Projection> getProjectionType() {
      return null;
    }
  }
}