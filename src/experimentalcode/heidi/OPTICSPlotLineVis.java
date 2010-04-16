package experimentalcode.heidi;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.AnyMap;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
/**
 * Visualize a line in a OPTICS Plot to select an Epsilon value
 * and generates a new result with the new epsilon
 * 
 * @author 
 * 
 * @param <D> distance type
 */
public class OPTICSPlotLineVis<D extends NumberDistance<D, ?>> extends AbstractVisualizer{
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String CSS_MARKER = "opticsPlotMarker";

  /**
   * CSS-Styles
   */
  public static final String CSS_RANGEMARKER = "opticsPlotRangeMarker";

  public static final String CSS_EVENTRECT = "opticsPlotEventrect";

  public static final String CSS_LINE = "opticsPlotLine";

  private final static String CSS_EPSILON = "opticsPlotEpsilonValue";

  /**
   * Curves to visualize
   */
  ArrayList<ClusterOrderResult<D>> cos;

  /**
   * OpticsPlotVis
   */
  OPTICSPlotVis<D> opvis;

  /**
   * flag if mouseDown (to move line)
   */
  protected boolean mouseDown;

  /**
   * The SVGPlot
   */
  private SVGPlot svgp;

  List<ClusterOrderEntry<D>> order;
  Double imgratio;
  LinearScale linscale;
  double scale;
  int plotInd;
  OPTICSPlot<D> opticsplot;
  public Element ltag;
  
  public void init(OPTICSPlotVis<D> opvis, SVGPlot svgp,VisualizerContext context, List<ClusterOrderEntry<D>> order, int plotInd) {
    this.opvis = opvis;
    this.order = order;
    this.svgp = svgp;  
    ltag = svgp.svgElement(SVGConstants.SVG_G_TAG);
    this.plotInd = plotInd;
    opticsplot = opvis.opvisualizer.getOpticsplots().get(plotInd);
    imgratio = 1. / (Double) opticsplot.getRatio();
    linscale = opticsplot.getScale();
    scale = StyleLibrary.SCALE;
  }
  /**
   * Creates an SVG-Element for the Line to select Epsilon-value
   * 
   * @param y Y-Value (relative Value to upper point of  layer)
   * @return SVG-Element
   */

  protected Element visualize(double yAct) {

    double space = scale * OPTICSPlotVis.SPACEFACTOR;
    double yValueLayer = opvis.getYValueOfPlot(plotInd);

    Double heightPlot = scale * imgratio;
    
    if(yAct < 0 + space/2) {
      yAct = space/2;
    }
    if(yAct> (scale * imgratio) + space/2) {
      yAct = (scale * imgratio) + space/2;
    }
    // absolute value
    yAct = yAct + yValueLayer;
    Double scY = (((plotInd * heightPlot) + (scale * imgratio) - yAct-space/2) / (scale * imgratio)) * (linscale.getMax() - linscale.getMin()) + linscale.getMin();

    final Element ltagLine = svgp.svgRect(0, yAct, scale * 1.08, scale * 0.0004);
    SVGUtil.addCSSClass(ltagLine, CSS_LINE);
    final Element ltagPoint = svgp.svgCircle(scale * 1.08, yAct, scale * 0.004);
    SVGUtil.addCSSClass(ltagPoint, CSS_LINE);
    Element ltagText = svgp.svgText(scale * 1.10, yAct, scY.toString());
    SVGUtil.setAtt(ltagText, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_EPSILON);
    final Element ltagEventRect = svgp.svgRect(scale * 1.03,  yValueLayer, scale * 0.2, heightPlot+space);
    SVGUtil.addCSSClass(ltagEventRect, CSS_EVENTRECT);

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

  private void addEventTagLine(OPTICSPlotVis<D> opvisualizer, SVGPlot svgp, Element ltagEventRect) {
    EventTarget targ = (EventTarget) ltagEventRect;


    OPTICSPlotLineHandler oplhandler = new OPTICSPlotLineHandler(this, order, svgp, ltagEventRect, ltag, plotInd);
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEDOWN, oplhandler, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEMOVE, oplhandler, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEUP, oplhandler, false);
  }
  protected void handleLineMousedown(Event evt, SVGPoint cPt, int plotInd) {
    double yValueLayer = opvis.getYValueOfPlot(plotInd);
    opvis.updateLines(cPt.getY()-yValueLayer);
    mouseDown = true;
  }

  protected void handleLineMousemove(Event evt, SVGPoint cPt, int plotInd) {
    if(mouseDown) {
      double yValueLayer = opvis.getYValueOfPlot(plotInd);
      opvis.updateLines(cPt.getY()-yValueLayer);    }
  }

  protected void handleLineMouseup(Event evt, SVGPoint cPt, int plotInd) {
    if(mouseDown) {
      double yValueLayer = opvis.getYValueOfPlot(plotInd);
      opvis.updateLines(cPt.getY()-yValueLayer);
      mouseDown = false;
    }
    float y = cPt.getY();
    double yValue = 0.0;

    double scale = StyleLibrary.SCALE;

    Double epsilon = (((plotInd * yValue) + (scale * imgratio - y) / (scale * imgratio)) * (linscale.getMax() - linscale.getMin()) + linscale.getMin());

     // Holds a list of clusters found.
    List<List<Integer>> resultList = new ArrayList<List<Integer>>();

    // Holds a set of noise.    
    Set<Integer> noise = new HashSet<Integer>();

    DoubleDistance lastDist = new DoubleDistance();
    DoubleDistance actDist = new DoubleDistance();
    actDist.infiniteDistance();
    List<Integer> res = new ArrayList<Integer>();

    for(int j = 0; j < order.size(); j++) {
      lastDist = actDist;
      actDist = (DoubleDistance) order.get(j).getReachability();

      if(actDist.getValue() > epsilon) {
        if(!res.isEmpty()) {
          resultList.add(res);
          res = new ArrayList<Integer>();
        }
        noise.add(order.get(j).getID());
      }
      else {
        if(lastDist.getValue() > epsilon) {
          res.add(order.get(j - 1).getID());
          noise.remove(order.get(j - 1).getID());
        }
        res.add(order.get(j).getID());
      }
    }
    if(!res.isEmpty()) {
      resultList.add(res);
    }
//    logger.warning("resultList: " + resultList.toString());
//    logger.warning("noise: " + noise.toString());

    Clustering<Model> cl = newResult(resultList, noise);
    opvis.opvisualizer.onClusteringUpdated(cl);
  }

  private Clustering<Model> newResult(List<List<Integer>> resultList, Set<Integer> noise) {

    Clustering<Model> result = new Clustering<Model>();
    for(List<Integer> res : resultList) {
      DatabaseObjectGroup group = new DatabaseObjectGroupCollection<List<Integer>>(res);
      Cluster<Model> c = new Cluster<Model>(group, ClusterModel.CLUSTER);
      result.addCluster(c);
    }

    DatabaseObjectGroup group = new DatabaseObjectGroupCollection<Set<Integer>>(noise);
    Cluster<Model> n = new Cluster<Model>(group, true, ClusterModel.CLUSTER);
    result.addCluster(n);

    return result;
  }
  
}
