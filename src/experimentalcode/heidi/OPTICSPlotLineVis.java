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
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

public class OPTICSPlotLineVis<D extends NumberDistance<D, ?>> extends AbstractVisualizer{
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String CSS_MARKER = "opticsPlotMarker";

  /**
   * CSS-Styles
   */
  public static final String CSS_RANGEMARKER = "opticsPlotRangeMarker";

  public static final String CSS_EVENTRECTLINE = "opticsPlotEventrectLine";

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
  List<Double> imgratios;
  Double imgratio;
  LinearScale linscale;
  Double sizey;
  double yUp;
  double yBottom;
  double yAct;
  double scale;
  int plotInd;
  public Element ltag;
  
  public void init(OPTICSPlotVis<D> opvis, SVGPlot svgp,VisualizerContext context, List<ClusterOrderEntry<D>> order, double yBottom, double yUp,List<Double> imgratios, Double imgratio, LinearScale linscale, int plotInd) {
    this.opvis = opvis;
    this.order = order;
    this.svgp = svgp;  
    this.imgratios= imgratios;
    this.imgratio=imgratio;
    this.linscale=linscale;
    ltag = svgp.svgElement(SVGConstants.SVG_G_TAG);
    this.yBottom = yBottom;
    this.yUp = yUp;
    this.plotInd =plotInd;
    scale = StyleLibrary.SCALE;
    addCSSClasses(svgp);
  }
  /**
   * Adds the required CSS-Classes
   * 
   * @param svgp SVG-Plot
   */
  private void addCSSClasses(SVGPlot svgp) {
    // Class for the line
    if(!svgp.getCSSClassManager().contains(CSS_LINE)) {
      final CSSClass lcls = new CSSClass(this, CSS_LINE);
      lcls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
      lcls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "1");
      try {
        svgp.getCSSClassManager().addClass(lcls);
      }
      catch(CSSNamingConflict e) {
        de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
      }
    }
    // Class for the rectangle to add eventListeners
    if(!svgp.getCSSClassManager().contains(CSS_EVENTRECTLINE)) {
      final CSSClass ecls = new CSSClass(this, CSS_EVENTRECTLINE);
      ecls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, "0.05");
      try {
        svgp.getCSSClassManager().addClass(ecls);
      }
      catch(CSSNamingConflict e) {
        de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
      }
    }
    // Class for the epsilon-value
    //TODO: Hierher
//    if(!svgp.getCSSClassManager().contains(CSS_EPSILON)) {
//      final CSSClass label = new CSSClass(svgp, CSS_EPSILON);
//      label.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, 1);
//      label.setStatement(SVGConstants.CSS_FILL_PROPERTY, context.getStyleLibrary().getTextColor(StyleLibrary.AXIS_LABEL));
//      label.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, context.getStyleLibrary().getFontFamily(StyleLibrary.AXIS_LABEL));
//      label.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, context.getStyleLibrary().getTextSize(StyleLibrary.AXIS_LABEL));
//
//      try {
//        svgp.getCSSClassManager().addClass(label);
//      }
//      catch(CSSNamingConflict e) {
//        de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
//      }
//    }
  }
  /**
   * Creates an SVG-Element for the Line to select Epsilon-value

   * @param ltag SVG-Tag to add Line
   * @param svgp SVG-Plot
   * @param y Y-Value
   * @param plotInd Index of the ClusterOrderResult
   * @param scale The scale
   * @return SVG-Element
   */

  protected Element drawLine(double yAct) {
    LinearScale xscale = linscale;
    //TODO: unterschiedliche Höhen der plots beachten
    double plotHeight = yBottom-yUp;

//    Double scY = (linscale.getMax() - linscale.getMin()) + linscale.getMin();
    Double scY = (((plotInd * plotHeight) + (scale * imgratio) - yAct) / (scale * imgratio)) * (xscale.getMax() - xscale.getMin()) + xscale.getMin();
 
//    if(scY < 0.0) {
//      // logger.warning("scY: " + scY);
//      scY = 0.0;
//    }
//    if(yAct - (plotInd * plotHeight) > scale * imgratio) {
//      yAct = plotInd * plotHeight + scale * imgratio;
//    }


    final Element ltagLine = svgp.svgRect(0, yAct, scale * 1.08, scale * 0.0004);
    SVGUtil.addCSSClass(ltagLine, CSS_LINE);
    final Element ltagPoint = svgp.svgCircle(scale * 1.08, yAct, scale * 0.004);
    SVGUtil.addCSSClass(ltagPoint, CSS_LINE);
    Element ltagText = svgp.svgText(scale * 1.10, yAct, scY.toString());
    SVGUtil.setAtt(ltagText, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_EPSILON);
    final Element ltagEventRect = svgp.svgRect(scale * 1.03,  yUp - plotHeight*(OPTICSPlotVis.SPACEFACTOR/2), scale * 0.2, plotHeight*OPTICSPlotVis.SPACEFACTOR);
    SVGUtil.addCSSClass(ltagEventRect, CSS_EVENTRECTLINE);

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
    drawLine(cPt.getY());
    mouseDown = true;
  }

  protected void handleLineMousemove(Event evt, SVGPoint cPt, int plotInd) {
    if(mouseDown) {
      drawLine(cPt.getY());
    }
  }

  protected void handleLineMouseup(Event evt, SVGPoint cPt, int plotInd) {
    if(mouseDown) {
      drawLine(cPt.getY());
      mouseDown = false;
    }
    float y = cPt.getY();
    double yValue = 0.0;
    for (int j=0; j<plotInd; j++){
      yValue += (scale * imgratios.get(j)*1.4);
    }
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
