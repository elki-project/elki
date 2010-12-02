package experimentalcode.heidi.optics;

import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSCut;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.AbstractUnprojectedVisFactory;

/**
 * Visualizes a cut in an OPTICS Plot to select an Epsilon value and generate a
 * new clustering result
 * 
 * @author Heidi Kolb
 * 
 * @param <D> distance type
 */
public class OPTICSPlotCutVisualization<D extends Distance<D>> extends AbstractVisualization<DatabaseObject> implements DragableArea.DragListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICS Cut";

  /**
   * OpticsPlotVisualizer
   */
  //private OPTICSPlotVisualization<D> opvis;

  /**
   * Our concerned curve
   */
  ClusterOrderResult<D> order;

  /**
   * The actual plot
   */
  private OPTICSPlot<D> opticsplot;

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
  private DragableArea eventarea;

  /**
   * Constructor.
   * 
   * @param task Task
   */
  public OPTICSPlotCutVisualization(VisualizationTask task) {
    super(task, VisFactory.LEVEL_INTERACTIVE);
    this.order = task.getResult();
    this.opticsplot = OPTICSPlot.plotForClusterOrder(this.order, context);

    //this.opvis = opvis;
    this.layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    //this.opticsplot = opticsplot;
    plotHeight = StyleLibrary.SCALE / opticsplot.getRatio();
    // TODO: are the event areas destroyed properly?
  }

  @Override
  protected void redraw() {
    double scale = StyleLibrary.SCALE;
    double space = scale * OPTICSPlotVisualization.SPACEFACTOR;
    // compute absolute y-value
    final double yAct;
    final Element ltagText;
    if(epsilon != 0.) {
      yAct = plotHeight - getYFromEpsilon(epsilon);
      // TODO make the number of digits configurable
      ltagText = svgp.svgText(StyleLibrary.SCALE + space * 0.6, yAct, FormatUtil.format(epsilon, 4));
    }
    else {
      yAct = plotHeight - getYFromEpsilon(epsilon);
      ltagText = svgp.svgText(StyleLibrary.SCALE + space * 0.6, yAct, " ");
    }
    SVGUtil.setAtt(ltagText, SVGConstants.SVG_CLASS_ATTRIBUTE, OPTICSPlotVisualization.CSS_EPSILON);

    // line and handle
    final Element ltagLine = svgp.svgLine(0, yAct, StyleLibrary.SCALE + space / 2, yAct);
    SVGUtil.addCSSClass(ltagLine, OPTICSPlotVisualization.CSS_LINE);
    final Element ltagPoint = svgp.svgCircle(StyleLibrary.SCALE + space / 2, yAct, StyleLibrary.SCALE * 0.004);
    SVGUtil.addCSSClass(ltagPoint, OPTICSPlotVisualization.CSS_LINE);

    if(layer.hasChildNodes()) {
      NodeList nodes = layer.getChildNodes();
      layer.replaceChild(ltagLine, nodes.item(0));
      layer.replaceChild(ltagPoint, nodes.item(1));
      layer.replaceChild(ltagText, nodes.item(2));
    }
    else {
      layer.appendChild(ltagLine);
      layer.appendChild(ltagPoint);
      layer.appendChild(ltagText);
      eventarea = new DragableArea(svgp, StyleLibrary.SCALE, 0, space, plotHeight, this);
      layer.appendChild(eventarea.getElement());
    }
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
    return true;
  }

  @Override
  public boolean duringDrag(@SuppressWarnings("unused") SVGPoint start, SVGPoint end, @SuppressWarnings("unused") Event evt, @SuppressWarnings("unused") boolean inside) {
    epsilon = getEpsilonFromY(plotHeight - end.getY());
    // opvis.unsetEpsilonExcept(this);
    return true;
  }

  @Override
  public boolean endDrag(@SuppressWarnings("unused") SVGPoint start, SVGPoint end, @SuppressWarnings("unused") Event evt, boolean inside) {
    if(inside) {
      epsilon = getEpsilonFromY(plotHeight - end.getY());
      // opvis.unsetEpsilonExcept(this);

      // FIXME: replace an existing optics cut result!
      Clustering<Model> cl = OPTICSCut.makeOPTICSCut(order, opticsplot.getDistanceAdapter(), epsilon);
      order.addDerivedResult(cl);
    }
    return true;
  }

  /**
   * Reset the epsilon value.
   */
  public void unsetEpsilon() {
    epsilon = 0.0;
  }

  /**
   * Factory class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has OPTICSPlotCutVisualization
   */
  public static class Factory extends AbstractUnprojectedVisFactory<DatabaseObject> {
    public Factory() {
      super(NAME);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends DatabaseObject> context, AnyResult result) {
      List<ClusterOrderResult<DoubleDistance>> cos = ResultUtil.filterResults(result, ClusterOrderResult.class);
      for(ClusterOrderResult<DoubleDistance> co : cos) {
        context.addVisualizer(co, this);
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new OPTICSPlotCutVisualization<DoubleDistance>(task);
    }
  }
}