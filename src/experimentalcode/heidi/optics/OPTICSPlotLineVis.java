package experimentalcode.heidi.optics;

import java.util.ArrayList;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualizes a cut in an OPTICS Plot to select an Epsilon value and generate a
 * new clustering result
 * 
 * @author Heidi Kolb
 * 
 * @param <D> distance type
 */
public class OPTICSPlotLineVis<D extends Distance<D>> extends AbstractVisualizer<DatabaseObject> implements EventListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICSPlotLineVis";

  /**
   * Curves to visualize
   */
  ArrayList<ClusterOrderResult<D>> cos;

  /**
   * OpticsPlotVisualizer
   */
  private OPTICSPlotVisualizer<D> opvis;

  /**
   * Flag if mouseDown (to move cut)
   */
  protected boolean mouseDown;

  /**
   * The SVGPlot
   */
  private SVGPlot svgp;

  /**
   * The concerned curve
   */
  List<ClusterOrderEntry<D>> order;

  /**
   * The actual plot
   */
  private OPTICSPlot<D> opticsplot;

  /**
   * SVG-Element for the cut
   */
  private Element ltag;

  /**
   * The current epsilon value.
   */
  private double epsilon = 0.0;

  /**
   * The height of the plot
   */
  private double plotHeight;

  /**
   * Constructor
   */
  public OPTICSPlotLineVis() {
    super(NAME);
  }

  /**
   * Initializes the Visualizer
   * 
   * @param opvis The OPTICSPlotVisualizer
   * @param svgp The SVGPlot
   * @param context The Context
   * @param order The curve
   */
  public void init(OPTICSPlotVisualizer<D> opvis, OPTICSPlot<D> opticsplot, SVGPlot svgp, VisualizerContext<? extends DatabaseObject> context, List<ClusterOrderEntry<D>> order) {
    super.init(context);
    this.opvis = opvis;
    this.order = order;
    this.svgp = svgp;
    ltag = svgp.svgElement(SVGConstants.SVG_G_TAG);
    this.opticsplot = opticsplot;
    plotHeight = StyleLibrary.SCALE / opticsplot.getRatio();
  }

  /**
   * Creates an SVG-Element for the cut
   * 
   * @param epsilon Epsilon Value
   * @param plotInd Index of the Plot
   * @return SVG-Element
   */
  protected Element visualize() {
    double scale = StyleLibrary.SCALE;
    double space = scale * OPTICSPlotVisualizer.SPACEFACTOR;
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
    SVGUtil.setAtt(ltagText, SVGConstants.SVG_CLASS_ATTRIBUTE, OPTICSPlotVisualizerFactory.CSS_EPSILON);

    final Element ltagLine = svgp.svgLine(0, yAct, StyleLibrary.SCALE + space / 2, yAct);
    SVGUtil.addCSSClass(ltagLine, OPTICSPlotVisualizerFactory.CSS_LINE);
    final Element ltagPoint = svgp.svgCircle(StyleLibrary.SCALE + space / 2, yAct, StyleLibrary.SCALE * 0.004);
    SVGUtil.addCSSClass(ltagPoint, OPTICSPlotVisualizerFactory.CSS_LINE);

    final Element ltagEventRect = svgp.svgRect(StyleLibrary.SCALE, 0, space, plotHeight);
    SVGUtil.addCSSClass(ltagEventRect, OPTICSPlotVisualizerFactory.CSS_EVENTRECT);

    if(ltag.hasChildNodes()) {
      NodeList nodes = ltag.getChildNodes();
      ltag.replaceChild(ltagLine, nodes.item(0));
      ltag.replaceChild(ltagPoint, nodes.item(1));
      ltag.replaceChild(ltagText, nodes.item(2));
    }
    else {
      ltag.appendChild(ltagLine);
      ltag.appendChild(ltagPoint);
      ltag.appendChild(ltagText);
      ltag.appendChild(ltagEventRect);
    }
    addEventTagLine(opvis, svgp, ltagEventRect);
    return ltag;
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

  /**
   * Add a handler to the element for the cut
   * 
   * @param opvisualizer OPTICSPlotVisualizer
   * @param svgp The SVGPlot
   * @param ltagEventRect The element to add a handler
   */
  private void addEventTagLine(OPTICSPlotVisualizer<D> opvisualizer, SVGPlot svgp, Element ltagEventRect) {
    EventTarget targ = (EventTarget) ltagEventRect;
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEDOWN, this, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEMOVE, this, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEUP, this, false);
  }

  /**
   * Handle Mousedown. <br>
   * Move cut to the mouse position
   * 
   * @param evt Event
   * @param cPt Point in element coordinates
   */
  protected void handleLineMousedown(Event evt, SVGPoint cPt) {
    epsilon = getEpsilonFromY(plotHeight - cPt.getY());
    opvis.unsetEpsilonExcept(this);
    mouseDown = true;
  }

  /**
   * Handle Mousemove. <br>
   * Move cut to the actual mouse position (if mouseDown)
   * 
   * @param evt Event
   * @param cPt Point in element coordinates
   */
  protected void handleLineMousemove(Event evt, SVGPoint cPt) {
    if(mouseDown) {
      epsilon = getEpsilonFromY(plotHeight - cPt.getY());
      opvis.unsetEpsilonExcept(this);
    }
  }

  /**
   * Reset the epsilon value.
   */
  public void unsetEpsilon() {
    epsilon = 0.0;
  }

  /**
   * 
   * Handle Mouseup.<br>
   * If mousedown: move cut to the mouse position, build new clustering
   * 
   * @param evt Event
   * @param cPt Point in element coordinates
   */
  protected void handleLineMouseup(Event evt, SVGPoint cPt) {
    if(mouseDown) {
      epsilon = getEpsilonFromY(plotHeight - cPt.getY());

      opvis.unsetEpsilonExcept(this);
      mouseDown = false;

      // Holds a list of clusters found.
      List<ModifiableDBIDs> resultList = new ArrayList<ModifiableDBIDs>();

      // Holds a set of noise.
      ModifiableDBIDs noise = DBIDUtil.newHashSet();

      double lastDist = Double.MAX_VALUE;
      double actDist = Double.MAX_VALUE;
      ModifiableDBIDs res = DBIDUtil.newHashSet();

      for(int j = 0; j < order.size(); j++) {
        lastDist = actDist;
        actDist = opticsplot.getDistanceAdapter().getDoubleForEntry(order.get(j));

        if(actDist > epsilon) {
          if(!res.isEmpty()) {
            resultList.add(res);
            res = DBIDUtil.newHashSet();
          }
          noise.add(order.get(j).getID());
        }
        else {
          if(lastDist > epsilon) {
            res.add(order.get(j - 1).getID());
            noise.remove(order.get(j - 1).getID());
          }
          res.add(order.get(j).getID());
        }
      }
      if(!res.isEmpty()) {
        resultList.add(res);
      }
      Clustering<Model> cl = newResult(resultList, noise);
      opvis.opvisualizer.onClusteringUpdated(cl);
    }
  }

  /**
   * Build a new clustering
   * 
   * @param resultList List of DBIDs for each Cluster
   * @param noise DBIDs being noise
   * @return new clustering
   */
  private Clustering<Model> newResult(List<ModifiableDBIDs> resultList, ModifiableDBIDs noise) {
    Clustering<Model> result = new Clustering<Model>();
    for(ModifiableDBIDs res : resultList) {
      Cluster<Model> c = new Cluster<Model>(res, ClusterModel.CLUSTER);
      result.addCluster(c);
    }
    Cluster<Model> n = new Cluster<Model>(noise, true, ClusterModel.CLUSTER);
    result.addCluster(n);
    return result;
  }

  @Override
  public void handleEvent(Event evt) {
    SVGPoint cPt = SVGUtil.elementCoordinatesFromEvent(this.svgp.getDocument(), this.ltag, evt);

    if(evt.getType().equals(SVGConstants.SVG_EVENT_MOUSEDOWN)) {
      handleLineMousedown(evt, cPt);
    }
    else if(evt.getType().equals(SVGConstants.SVG_EVENT_MOUSEMOVE)) {
      handleLineMousemove(evt, cPt);
    }
    else if(evt.getType().equals(SVGConstants.SVG_EVENT_MOUSEUP)) {
      handleLineMouseup(evt, cPt);
    }
  }
}