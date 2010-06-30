package experimentalcode.heidi;

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

/**
 * Handle the marked items in an OPTICS plots.
 * 
 * @author
 * 
 * @param <D> distance type
 */
public class OPTICSPlotPlotVis<D extends Distance<D>> extends AbstractVisualizer<DatabaseObject> {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Heidi OPTICSPlotPlotVis";

  /**
   * OpticsPlotVis
   */
  private OPTICSPlotVisualizer<D> opvis;

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

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public OPTICSPlotPlotVis() {
    super(NAME);
  }
  
  public void init(OPTICSPlotVisualizer<D> opvis, SVGPlot svgp, VisualizerContext<?> context, List<ClusterOrderEntry<D>> order, int plotInd) {
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
   * Creates an SVG-Element
   * 
   * @return SVG-Element
   */
  protected Element visualize() {
    double scale = StyleLibrary.SCALE;
    double space = scale * OPTICSPlotVisualizer.SPACEFACTOR;
    double yValueLayerUp = opvis.getYValueOfPlot(plotInd);
    Double heightPlot = scale * imgratio;

    // rect greater than plot to mark ranges
    etag = svgp.svgRect(0 - space, yValueLayerUp, scale + space, heightPlot + space / 2);
    SVGUtil.addCSSClass(etag, OPTICSPlotVisualizerFactory.CSS_EVENTRECT);
    addEventTag(opvis, svgp, etag);

    addMarker(svgp);
    // mtag first !
    layer.appendChild(mtag);
    layer.appendChild(etag);
    return layer;
  }

  private void addEventTag(OPTICSPlotVisualizer<D> opvisualizer, SVGPlot svgp, Element etag) {
    EventTarget targ = (EventTarget) etag;
    OPTICSPlotHandler<D> ophandler = new OPTICSPlotHandler<D>(this, svgp, order, etag);
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEDOWN, ophandler, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEUP, ophandler, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEMOVE, ophandler, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_KEYDOWN, ophandler, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_KEYPRESS, ophandler, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_KEYUP, ophandler, false);
  }

  private void deleteChildren(Element container) {
    // TODO: wie in SelectionUpdateVisualizer, oder nach "oben" schieben
    if(container.hasChildNodes()) {
      container = (Element) container.cloneNode(false);
    }
  }

  /**
   * Adds the Markers to the given tag
   * 
   * @param svgp SVG-Plot
   */
  public void addMarker(SVGPlot svgp) {
    deleteChildren(mtag);
    SelectionContext selContext = SelectionContext.getSelection(context);
    if(selContext != null) {
      ArrayModifiableDBIDs selection = selContext.getSelectedIds();

      for(int i = 0; i < selection.size(); i++) {
        DBID coeID = selection.get(i);
        Integer elementNr = -1;

        for(int j = 0; j < order.size(); j++) {
          DBID orderID = order.get(j).getID();
          if(coeID.equals(orderID)) {
            elementNr = j;
            break;
          }
        }
        Double width = StyleLibrary.SCALE / order.size();
        Double x1 = elementNr * width;
        Element marker = addMarkerRect(svgp, x1, width);
        SVGUtil.addCSSClass(marker, OPTICSPlotVisualizerFactory.CSS_MARKER);
        mtag.appendChild(marker);
      }
    }
  }

  /**
   * Creates an SVG-Element for the marker (Marker higher than plot!)
   * 
   * @param svgp SVG-Plot
   * @param x1 X-Value
   * @param width Width
   * @return SVG-Element
   */
  public Element addMarkerRect(SVGPlot svgp, Double x1, Double width) {
    double yValueLayer = opvis.getYValueOfPlot(plotInd);
    double space = StyleLibrary.SCALE * OPTICSPlotVisualizer.SPACEFACTOR;
    Double heightPlot = StyleLibrary.SCALE * imgratio;
    return svgp.svgRect(x1, yValueLayer, width, heightPlot + space / 2);
  }

  protected void handlePlotMouseDown(int mouseActIndex) {
    opvis.mouseDown = true;
    opvis.mouseDownIndex = mouseActIndex;
    if(mouseActIndex >= 0 && mouseActIndex < order.size()) {
      Double width = StyleLibrary.SCALE / order.size();
      Double x1 = mouseActIndex * width;
      Element marker = addMarkerRect(svgp, x1, width);
      SVGUtil.setCSSClass(marker, OPTICSPlotVisualizerFactory.CSS_RANGEMARKER);
      mtag.appendChild(marker);
    }
  }

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

  protected void handlePlotMouseMove(int mouseActIndex) {
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
        SVGUtil.setCSSClass(marker, OPTICSPlotVisualizerFactory.CSS_RANGEMARKER);
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