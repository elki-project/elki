package experimentalcode.heidi.optics;

import java.util.ArrayList;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
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
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
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
public class OPTICSPlotLineVis<D extends Distance<D>> extends AbstractVisualizer<DatabaseObject> implements DragableArea.DragListener {
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

  private DragableArea eventarea;

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

    // TODO: are the event areas destroyed properly?
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

    // line and handle
    final Element ltagLine = svgp.svgLine(0, yAct, StyleLibrary.SCALE + space / 2, yAct);
    SVGUtil.addCSSClass(ltagLine, OPTICSPlotVisualizerFactory.CSS_LINE);
    final Element ltagPoint = svgp.svgCircle(StyleLibrary.SCALE + space / 2, yAct, StyleLibrary.SCALE * 0.004);
    SVGUtil.addCSSClass(ltagPoint, OPTICSPlotVisualizerFactory.CSS_LINE);

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
      eventarea = new DragableArea(svgp, StyleLibrary.SCALE, 0, space, plotHeight, this);
      ltag.appendChild(eventarea.getElement());
    }
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

  @Override
  public boolean startDrag(SVGPoint start, @SuppressWarnings("unused") Event evt) {
    epsilon = getEpsilonFromY(plotHeight - start.getY());
    opvis.unsetEpsilonExcept(this);
    return true;
  }

  @Override
  public boolean duringDrag(@SuppressWarnings("unused") SVGPoint start, SVGPoint end, @SuppressWarnings("unused") Event evt, @SuppressWarnings("unused") boolean inside) {
    epsilon = getEpsilonFromY(plotHeight - end.getY());
    opvis.unsetEpsilonExcept(this);
    return true;
  }

  @Override
  public boolean endDrag(@SuppressWarnings("unused") SVGPoint start, SVGPoint end, @SuppressWarnings("unused") Event evt, boolean inside) {
    if(inside) {
      epsilon = getEpsilonFromY(plotHeight - end.getY());
      opvis.unsetEpsilonExcept(this);
  
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
    return true;
  }

  /**
   * Reset the epsilon value.
   */
  public void unsetEpsilon() {
    epsilon = 0.0;
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
}