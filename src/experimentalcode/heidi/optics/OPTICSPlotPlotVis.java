package experimentalcode.heidi.optics;

import java.awt.event.KeyEvent;
import java.util.List;

import org.apache.batik.dom.events.DOMKeyEvent;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import experimentalcode.heidi.SelectionContext;

/**
 * Handle the marker in an OPTICS plot.
 * 
 * @author Heidi Kolb
 * 
 * @param <D> distance type
 */
public class OPTICSPlotPlotVis<D extends Distance<D>> extends AbstractVisualizer<DatabaseObject> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICSPlotPlotVis";

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
  private List<ClusterOrderEntry<D>> order;

  /**
   * The ratio
   */
  private double imgratio;

  /**
   * Index of the plot
   */
  private int plotInd;

  /**
   * The plot
   */
  private OPTICSPlot<D> opticsplot;

  /**
   * The layer
   */
  private Element layer;

  /**
   * Element for the events
   */
  private Element etag;

  /**
   * Element for the marker
   */
  private Element mtag;

  /**
   * Constructor
   */
  public OPTICSPlotPlotVis() {
    super(NAME);
  }

  /**
   * Initializes the Visualizer
   * 
   * @param opvis OPTICSPlotVisualizer
   * @param svgp The SVGPlot
   * @param context The Context
   * @param order The curve
   * @param plotInd Index of the plot
   */
  public void init(OPTICSPlotVisualizer<D> opvis, SVGPlot svgp, VisualizerContext<? extends DatabaseObject> context, List<ClusterOrderEntry<D>> order, int plotInd) {
    super.init(context);
    this.opvis = opvis;
    this.order = order;
    this.svgp = svgp;

    etag = svgp.svgElement(SVGConstants.SVG_G_TAG);
    mtag = svgp.svgElement(SVGConstants.SVG_G_TAG);
    layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    this.plotInd = plotInd;
    opticsplot = opvis.opvisualizer.getOpticsplots().get(plotInd);
    imgratio = 1. / opticsplot.getRatio();
  }

  /**
   * Creates an SVG-Element containing marker and a transparent Element for
   * events
   * 
   * @return SVG-Element
   */
  protected Element visualize() {
    double scale = StyleLibrary.SCALE;
    double space = scale * OPTICSPlotVisualizer.SPACEFACTOR;
    double yValueLayerUp = opvis.getYValueOfPlot(plotInd);
    double heightPlot = scale * imgratio;

    // rect greater than plot to mark ranges
    etag = svgp.svgRect(0 - space, yValueLayerUp, scale + space, heightPlot + space / 2);
    SVGUtil.addCSSClass(etag, OPTICSPlotVisualizerFactory.CSS_EVENTRECT);
    addEventTag(svgp, etag);

    addMarker();
    // mtag first, etag must be the top Element
    layer.appendChild(mtag);
    layer.appendChild(etag);
    return layer;
  }

  /**
   * Add a handler to the element for events
   * 
   * @param svgp The SVGPlot
   * @param etag The element to add a handler
   */
  private void addEventTag(SVGPlot svgp, Element etag) {
    EventTarget targ = (EventTarget) etag;
    OPTICSPlotHandler<D> ophandler = new OPTICSPlotHandler<D>(this, svgp, order, etag);
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEDOWN, ophandler, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEUP, ophandler, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEMOVE, ophandler, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_KEYDOWN, ophandler, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_KEYPRESS, ophandler, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_KEYUP, ophandler, false);
  }

  /**
   * Delete the children of the element
   * 
   * @param container SVG-Element
   */
  private void deleteChildren(Element container) {
    if(container.hasChildNodes()) {
      container = (Element) container.cloneNode(false);
    }
  }

  /**
   * Add marker for the selected IDs to mtag
   */
  public void addMarker() {
    deleteChildren(mtag);
    SelectionContext selContext = SelectionContext.getSelection(context);
    if(selContext != null) {
      ArrayModifiableDBIDs selection = selContext.getSelectedIds();

      for(int i = 0; i < selection.size(); i++) {
        DBID coeID = selection.get(i);
        int elementNr = -1;

        for(int j = 0; j < order.size(); j++) {
          DBID orderID = order.get(j).getID();
          if(coeID.equals(orderID)) {
            elementNr = j;
            break;
          }
        }
        double width = StyleLibrary.SCALE / order.size();
        double x1 = elementNr * width;
        Element marker = addMarkerRect(x1, width);
        SVGUtil.addCSSClass(marker, OPTICSPlotVisualizerFactory.CSS_MARKER);
        mtag.appendChild(marker);
      }
    }
  }

  /**
   * Create a rectangle as marker (Marker higher than plot!)
   * 
   * @param x1 X-Value for the marker
   * @param width Width of an entry
   * @return SVG-Element svg-rectangle
   */
  public Element addMarkerRect(double x1, double width) {
    double yValueLayer = opvis.getYValueOfPlot(plotInd);
    double space = StyleLibrary.SCALE * OPTICSPlotVisualizer.SPACEFACTOR;
    double heightPlot = StyleLibrary.SCALE * imgratio;
    return svgp.svgRect(x1, yValueLayer, width, heightPlot + space / 2);
  }
  /**
   * Handle Mousedown. 
   * Save the actual clusterOrder index
   * 
   * @param mouseActIndex Index of the clusterOrderEntry where the event occured
   */
  protected void handlePlotMouseDown(int mouseActIndex) {
    opvis.mouseDown = true;
    opvis.mouseDownIndex = mouseActIndex;
    if(mouseActIndex >= 0 && mouseActIndex < order.size()) {
      double width = StyleLibrary.SCALE / order.size();
      double x1 = mouseActIndex * width;
      Element marker = addMarkerRect(x1, width);
      SVGUtil.setCSSClass(marker, OPTICSPlotVisualizerFactory.CSS_RANGEMARKER);
      mtag.appendChild(marker);
    }
  }
  /**
   * Handle MouseUp. 
   * Update the selection
   * 
   * @param mouseActIndex Index of the clusterOrderEntry where the event occured
   */
  protected void handlePlotMouseUp(int mouseActIndex) {
    if(!opvis.keyStrgPressed && !opvis.keyShiftPressed) {
      SelectionContext selContext = SelectionContext.getSelection(context);
      if(selContext != null) {
        selContext.clearSelectedIds();
      }
    }
    if(opvis.mouseDownIndex != mouseActIndex) {
      // Range selected
      for(int i = Math.max(Math.min(opvis.mouseDownIndex, mouseActIndex), 0); i <= Math.min(Math.max(opvis.mouseDownIndex, mouseActIndex), order.size()); i++) {
        opvis.updateSelection(order, i);
      }
    }
    else {
      // one item selected
      if(mouseActIndex >= 0 && mouseActIndex < order.size()) {
        opvis.updateSelection(order, mouseActIndex);
      }
    }

    opvis.mouseDown = false;
    opvis.updateMarker();
  }
  /**
   * Handle Mousemove. 
   * Draw a rectangle between mouseDownIndex and actual Index
   * 
   * @param mouseActIndex Index of the clusterOrderEntry where the event occured
   */
  protected void handlePlotMouseMove(int mouseActIndex) {
    if(opvis.mouseDown) {
      if(mouseActIndex >= 0 || mouseActIndex <= order.size() || opvis.mouseDownIndex >= 0 || opvis.mouseDownIndex <= order.size()) {
        double width = StyleLibrary.SCALE / order.size();
        double x1;
        double x2;
        if(mouseActIndex < opvis.mouseDownIndex) {
          x1 = mouseActIndex * width;
          x2 = (opvis.mouseDownIndex * width) + width;
        }
        else {
          x1 = opvis.mouseDownIndex * width;
          x2 = mouseActIndex * width + width;
        }
        mtag.removeChild(mtag.getLastChild());
        Element marker = addMarkerRect(x1, x2 - x1);
        SVGUtil.setCSSClass(marker, OPTICSPlotVisualizerFactory.CSS_RANGEMARKER);
        mtag.appendChild(marker);
      }
    }
  }

  /**
   * Handle KeyDown
   * @param evt Event
   */
  protected void handlePlotKeyDown(Event evt) {
    DOMKeyEvent domke = (DOMKeyEvent) evt;
    int keyCode = domke.getKeyCode();
    if(keyCode == KeyEvent.VK_SHIFT) {
      opvis.keyShiftPressed = true;
      opvis.keyStrgPressed = false;
    }
    else if(keyCode == KeyEvent.VK_CONTROL) {
      opvis.keyStrgPressed = true;
      opvis.keyShiftPressed = false;
    }
    else {
      opvis.keyStrgPressed = false;
      opvis.keyShiftPressed = false;
    }
  }
  /**
   * Handle KeyUp
   * @param evt Event
   */
  protected void handlePlotKeyUp(Event evt) {
    DOMKeyEvent domke = (DOMKeyEvent) evt;
    int keyCode = domke.getKeyCode();
    if(keyCode == KeyEvent.VK_SHIFT) {
      opvis.keyShiftPressed = false;
    }
    if(keyCode == KeyEvent.VK_CONTROL) {
      opvis.keyStrgPressed = false;
    }
  }
}