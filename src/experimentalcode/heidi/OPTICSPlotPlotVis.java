package experimentalcode.heidi;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import org.apache.batik.dom.events.DOMKeyEvent;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Handle the marked items in an OPTICS plots.
 * 
 * @author
 * 
 * @param <D> distance type
 */
public class OPTICSPlotPlotVis<D extends NumberDistance<D, ?>> extends AbstractVisualizer {

  /**
   * OpticsPlotVis
   */
  private OPTICSPlotVis<D> opvis;

  /**
   * The SVGPlot
   */
  private SVGPlot svgp;

  /**
   * 
   */
  private List<ClusterOrderEntry<D>> order;

  private Double imgratio;

  private int plotInd;

  private OPTICSPlot<D> opticsplot;

  private Element layer;

  private Element etag;

  private Element mtag;

  public void init(OPTICSPlotVis<D> opvis, SVGPlot svgp, VisualizerContext context, List<ClusterOrderEntry<D>> order, int plotInd) {
    this.opvis = opvis;
    this.order = order;
    this.svgp = svgp;
    etag = svgp.svgElement(SVGConstants.SVG_G_TAG);
    mtag = svgp.svgElement(SVGConstants.SVG_G_TAG);
    layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    this.plotInd = plotInd;
    opticsplot = opvis.opvisualizer.getOpticsplots().get(plotInd);
    imgratio = 1. / (Double) opticsplot.getRatio();
  }

  /**
   * Creates an SVG-Element
   * 
   * @param ltag SVG-Tag to add Line
   * @param svgp SVG-Plot
   * @param y Y-Value
   * @param plotInd Index of the ClusterOrderResult
   * @param scale The scale
   * @return SVG-Element
   */

  protected Element visualize() {
    double scale = StyleLibrary.SCALE;
    double space = scale * OPTICSPlotVis.SPACEFACTOR;
    double yValueLayerUp = opvis.getYValueOfPlot(plotInd);
    Double heightPlot = scale * imgratio;

    // rect greater than plot to mark ranges
    etag = svgp.svgRect(0 - space, yValueLayerUp, scale + space, heightPlot + space / 2);
    SVGUtil.addCSSClass(etag, OPTICSPlotVisualizer.CSS_EVENTRECT);
    addEventTag(opvis, svgp, etag);

    addMarker(svgp);
    // mtag first !
    layer.appendChild(mtag);
    layer.appendChild(etag);
    return layer;
  }

  private void addEventTag(OPTICSPlotVis<D> opvisualizer, SVGPlot svgp, Element etag) {
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
   * Adds the Markers to the given tag
   * 
   * @param svgp SVG-Plot
   * @param mtag Tag to which the markers should be added
   * @param cosIndex Index of the ClusterOrderResult
   * @param scale The scale
   */
  public void addMarker(SVGPlot svgp) {

    while(mtag.hasChildNodes()) {
      mtag.removeChild(mtag.getLastChild());
    }
    ArrayList<Integer> selection = opvis.opvisualizer.getSelection();

    for(int i = 0; i < selection.size(); i++) {
      Integer coeID = selection.get(i);
      Integer elementNr = -1;

      for(int j = 0; j < order.size(); j++) {
        Integer orderID = order.get(j).getID();
        if(coeID.equals(orderID)) {
          elementNr = j;
          break;
        }
      }
      Double width = StyleLibrary.SCALE / order.size();
      Double x1 = elementNr * width;
      Element marker = addMarkerRect(svgp, x1, width);
      SVGUtil.addCSSClass(marker, OPTICSPlotVisualizer.CSS_MARKER);
      mtag.appendChild(marker);
    }
  }

  /**
   * Creates an SVG-Element for the marker (Marker higher than plot!)
   * 
   * @param svgp SVG-Plot
   * @param cosIndex Index of the ClusterOrderResult
   * @param x1 X-Value
   * @param width Width
   * @return SVG-Element
   */
  public Element addMarkerRect(SVGPlot svgp, Double x1, Double width) {

    double yValueLayer = opvis.getYValueOfPlot(plotInd);

    double space = StyleLibrary.SCALE * OPTICSPlotVis.SPACEFACTOR;
    Double heightPlot = StyleLibrary.SCALE * imgratio;
    return svgp.svgRect(x1, yValueLayer, width, heightPlot + space / 2);
  }

  protected void handlePlotMouseDown(int mouseActIndex) {
    // logger.warning("mouseDown - Index: " + mouseActIndex);
    opvis.mouseDown = true;
    opvis.mouseDownIndex = mouseActIndex;
    if(mouseActIndex >= 0 && mouseActIndex < order.size()) {
      Double width = StyleLibrary.SCALE / order.size();
      Double x1 = mouseActIndex * width;
      Element marker = addMarkerRect(svgp, x1, width);
      SVGUtil.setCSSClass(marker, OPTICSPlotVisualizer.CSS_RANGEMARKER);
      mtag.appendChild(marker);
    }
  }

  protected void handlePlotMouseUp(int mouseActIndex) {
    // logger.warning("mouseUp - Index: " + mouseActIndex);

    if(!opvis.keyStrgPressed && !opvis.keyShiftPressed) {
      opvis.opvisualizer.clearSelection();
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
    fireRedrawEvent();
    opvis.opvisualizer.requestRedraw();
  }

  protected void handlePlotMouseMove(int mouseActIndex) {
    // logger.warning("mouseUp - Index: " + mouseActIndex);

    if(opvis.mouseDown) {
      if(mouseActIndex >= 0 || mouseActIndex <= order.size() || opvis.mouseDownIndex >= 0 || opvis.mouseDownIndex <= order.size()) {
        Double width = StyleLibrary.SCALE / order.size();
        Double x1;
        Double x2;
        if(mouseActIndex < opvis.mouseDownIndex) {
          x1 = mouseActIndex * width;
          x2 = (opvis.mouseDownIndex * width) + width;
        }
        else {
          x1 = opvis.mouseDownIndex * width;
          x2 = mouseActIndex * width + width;
        }
        mtag.removeChild(mtag.getLastChild());
        Element marker = addMarkerRect(svgp, x1, x2 - x1);
        SVGUtil.setCSSClass(marker, OPTICSPlotVisualizer.CSS_RANGEMARKER);
        mtag.appendChild(marker);
      }
    }
  }

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