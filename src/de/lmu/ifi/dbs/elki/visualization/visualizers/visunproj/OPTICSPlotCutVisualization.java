package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.util.Collection;

import org.apache.batik.util.SVG12Constants;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSCut;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;

/**
 * Visualizes a cut in an OPTICS Plot to select an Epsilon value and generate a
 * new clustering result
 * 
 * @author Heidi Kolb
 * 
 * @apiviz.uses ClusterOrderResult oneway - 1 visualizes
 * @apiviz.uses OPTICSPlot oneway - 1 visualizes
 * 
 * @param <D> distance type
 */
public class OPTICSPlotCutVisualization<D extends Distance<D>> extends AbstractVisualization implements DragableArea.DragListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICS Cut";

  /**
   * Our concerned curve
   */
  ClusterOrderResult<D> order;

  /**
   * The actual plot
   */
  private OPTICSPlot<D> opticsplot;

  /**
   * CSS-Styles
   */
  protected static final String CSS_LINE = "opticsPlotLine";

  /**
   * CSS-Styles
   */
  protected final static String CSS_EPSILON = "opticsPlotEpsilonValue";

  /**
   * The current epsilon value.
   */
  private double epsilon = 0.0;

  /**
   * The height of the plot
   */
  private double plotHeight;

  /**
   * Sensitive (clickable) area
   */
  private DragableArea eventarea = null;

  /**
   * The label element
   */
  private Element elemText = null;

  /**
   * The line element
   */
  private Element elementLine = null;

  /**
   * The drag handle element
   */
  private Element elementPoint = null;

  /**
   * Constructor.
   * 
   * @param task Task
   */
  public OPTICSPlotCutVisualization(VisualizationTask task) {
    super(task);
    this.order = task.getResult();
    this.opticsplot = OPTICSPlot.plotForClusterOrder(this.order, context);
    this.plotHeight = StyleLibrary.SCALE / opticsplot.getRatio();

    synchronizedRedraw();
  }

  @Override
  protected void redraw() {
    incrementalRedraw();
  }

  @Override
  protected void incrementalRedraw() {
    addCSSClasses();

    final double scale = StyleLibrary.SCALE;

    if(layer == null) {
      layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
      final double sizex = scale;
      final double sizey = scale * task.getHeight() / task.getWidth();
      final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
      final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), sizex, sizey, margin);
      SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
      SVGUtil.setAtt(layer, SVGConstants.SVG_NAME_ATTRIBUTE, "cut layer");
    }

    // TODO make the number of digits configurable
    final String label = (epsilon != 0.0) ? FormatUtil.format(epsilon, 4) : "";
    // compute absolute y-value of bar
    final double yAct = plotHeight - getYFromEpsilon(epsilon);

    if(elemText == null) {
      elemText = svgp.svgText(StyleLibrary.SCALE * 1.05, yAct, label);
      SVGUtil.setAtt(elemText, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_EPSILON);
      layer.appendChild(elemText);
    }
    else {
      elemText.setTextContent(label);
      SVGUtil.setAtt(elemText, SVGConstants.SVG_Y_ATTRIBUTE, yAct);
    }

    // line and handle
    if(elementLine == null) {
      elementLine = svgp.svgLine(0, yAct, StyleLibrary.SCALE * 1.05, yAct);
      SVGUtil.addCSSClass(elementLine, CSS_LINE);
      layer.appendChild(elementLine);
    }
    else {
      SVGUtil.setAtt(elementLine, SVG12Constants.SVG_Y1_ATTRIBUTE, yAct);
      SVGUtil.setAtt(elementLine, SVG12Constants.SVG_Y2_ATTRIBUTE, yAct);
    }
    if(elementPoint == null) {
      elementPoint = svgp.svgCircle(StyleLibrary.SCALE * 1.05, yAct, StyleLibrary.SCALE * 0.004);
      SVGUtil.addCSSClass(elementPoint, CSS_LINE);
      layer.appendChild(elementPoint);
    }
    else {
      SVGUtil.setAtt(elementPoint, SVG12Constants.SVG_CY_ATTRIBUTE, yAct);
    }

    if(eventarea == null) {
      eventarea = new DragableArea(svgp, StyleLibrary.SCALE, 0, StyleLibrary.SCALE * 0.1, plotHeight, this);
      layer.appendChild(eventarea.getElement());
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    eventarea.destroy();
  }

  /**
   * Get epsilon from y-value
   * 
   * @param y y-Value
   * @return epsilon
   */
  protected double getEpsilonFromY(double y) {
    if(y < 0) {
      y = 0;
    }
    if(y > plotHeight) {
      y = plotHeight;
    }
    return opticsplot.getScale().getUnscaled(y / plotHeight);
  }

  /**
   * Get y-value from epsilon
   * 
   * @param epsilon epsilon
   * @return y-Value
   */
  protected double getYFromEpsilon(double epsilon) {
    double y = opticsplot.getScale().getScaled(epsilon) * plotHeight;
    if(y < 0) {
      y = 0;
    }
    if(y > plotHeight) {
      y = plotHeight;
    }
    return y;
  }

  @Override
  public boolean startDrag(SVGPoint start, @SuppressWarnings("unused") Event evt) {
    epsilon = getEpsilonFromY(plotHeight - start.getY());
    // opvis.unsetEpsilonExcept(this);
    synchronizedRedraw();
    return true;
  }

  @Override
  public boolean duringDrag(@SuppressWarnings("unused") SVGPoint start, SVGPoint end, @SuppressWarnings("unused") Event evt, boolean inside) {
    if(inside) {
      epsilon = getEpsilonFromY(plotHeight - end.getY());
    }
    // opvis.unsetEpsilonExcept(this);
    synchronizedRedraw();
    return true;
  }

  @Override
  public boolean endDrag(@SuppressWarnings("unused") SVGPoint start, SVGPoint end, @SuppressWarnings("unused") Event evt, boolean inside) {
    if(inside) {
      epsilon = getEpsilonFromY(plotHeight - end.getY());
      // opvis.unsetEpsilonExcept(this);

      // FIXME: replace an existing optics cut result!
      Clustering<Model> cl = OPTICSCut.makeOPTICSCut(order, opticsplot.getDistanceAdapter(), epsilon);
      order.addChildResult(cl);
    }
    context.resultChanged(this.task);
    // synchronizedRedraw();
    return true;
  }

  /**
   * Reset the epsilon value.
   */
  public void unsetEpsilon() {
    epsilon = 0.0;
  }

  /**
   * Adds the required CSS-Classes
   */
  private void addCSSClasses() {
    // Class for the epsilon-value
    if(!svgp.getCSSClassManager().contains(CSS_EPSILON)) {
      final CSSClass label = new CSSClass(svgp, CSS_EPSILON);
      label.setStatement(SVGConstants.CSS_FILL_PROPERTY, context.getStyleLibrary().getTextColor(StyleLibrary.AXIS_LABEL));
      label.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, context.getStyleLibrary().getFontFamily(StyleLibrary.AXIS_LABEL));
      label.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, context.getStyleLibrary().getTextSize(StyleLibrary.AXIS_LABEL));
      svgp.addCSSClassOrLogError(label);
    }
    // Class for the epsilon cut line
    if(!svgp.getCSSClassManager().contains(CSS_LINE)) {
      final CSSClass lcls = new CSSClass(svgp, CSS_LINE);
      lcls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, context.getStyleLibrary().getColor(StyleLibrary.PLOT));
      lcls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, 0.5 * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
      svgp.addCSSClassOrLogError(lcls);
    }
  }

  /**
   * Factory class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses OPTICSPlotCutVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    public Factory() {
      super();
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Collection<OPTICSPlot<?>> plots = ResultUtil.filterResults(result, OPTICSPlot.class);
      for(OPTICSPlot<?> plot : plots) {
        ClusterOrderResult<?> co = plot.getClusterOrder();
        final VisualizationTask task = new VisualizationTask(NAME, co, null, this, plot);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
        baseResult.getHierarchy().add(plot, task);
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new OPTICSPlotCutVisualization<DoubleDistance>(task);
    }

    @Override
    public Class<? extends Projection> getProjectionType() {
      return null;
    }
  }
}