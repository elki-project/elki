package experimentalcode.heidi.optics;

import java.util.List;

import org.apache.batik.dom.events.DOMMouseEvent;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.DBIDSelection;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Handle the marker in an OPTICS plot.
 * 
 * @author Heidi Kolb
 * 
 * @param <D> distance type
 */
public class OPTICSPlotPlotVis<D extends Distance<D>> extends AbstractVisualizer<DatabaseObject> implements EventListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICSPlotPlotVis";

  /**
   * Input modes
   */
  // TODO: Refactor all Mode copies into a shared class?
  private enum Mode {
    REPLACE, ADD, INVERT
  }

  /**
   * The SVGPlot
   */
  private SVGPlot svgp;

  /**
   * The concerned curve
   */
  private List<ClusterOrderEntry<D>> order;

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
   * MouseDown Index
   */
  protected Integer mouseDownIndex = null;

  /**
   * Constructor
   */
  public OPTICSPlotPlotVis() {
    super(NAME);
  }

  /**
   * Initializes the Visualizer
   * 
   * @param opticsplot The optics plot to show
   * @param svgp The SVGPlot
   * @param context The Context
   * @param order The curve
   */
  public void init(OPTICSPlot<D> opticsplot, SVGPlot svgp, VisualizerContext<? extends DatabaseObject> context, List<ClusterOrderEntry<D>> order) {
    super.init(context);
    this.order = order;
    this.svgp = svgp;

    etag = svgp.svgElement(SVGConstants.SVG_G_TAG);
    mtag = svgp.svgElement(SVGConstants.SVG_G_TAG);
    layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    this.opticsplot = opticsplot;
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
    double heightPlot = scale / opticsplot.getRatio();

    // Make event capturing rectangle greater than plot to easily mark ranges
    etag = svgp.svgRect(0 - space / 2, 0, scale + space, heightPlot);
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

    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEDOWN, this, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEUP, this, false);
    targ.addEventListener(SVGConstants.SVG_EVENT_MOUSEMOVE, this, false);
  }

  /**
   * Add marker for the selected IDs to mtag
   */
  public void addMarker() {
    // TODO: replace mtag!
    DBIDSelection selContext = context.getSelection();
    if(selContext != null) {
      DBIDs selection = DBIDUtil.ensureSet(selContext.getSelectedIds());

      final double width = StyleLibrary.SCALE / order.size();
      int begin = -1;
      for(int j = 0; j < order.size(); j++) {
        DBID id = order.get(j).getID();
        if(selection.contains(id)) {
          if(begin == -1) {
            begin = j;
          }
        }
        else {
          if(begin != -1) {
            Element marker = addMarkerRect(begin * width, (j - begin) * width);
            SVGUtil.addCSSClass(marker, OPTICSPlotVisualizerFactory.CSS_MARKER);
            mtag.appendChild(marker);
            begin = -1;
          }
        }
      }
      // tail
      if(begin != -1) {
        Element marker = addMarkerRect(begin * width, (order.size() - begin) * width);
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
    double heightPlot = StyleLibrary.SCALE / opticsplot.getRatio();
    return svgp.svgRect(x1, 0, width, heightPlot);
  }

  @Override
  public void handleEvent(Event evt) {
    if(evt.getType().equals(SVGConstants.SVG_EVENT_MOUSEDOWN)) {
      handlePlotMouseDown(evt);
    }
    else if(evt.getType().equals(SVGConstants.SVG_EVENT_MOUSEMOVE)) {
      handlePlotMouseMove(evt);
    }
    else if(evt.getType().equals(SVGConstants.SVG_EVENT_MOUSEUP)) {
      handlePlotMouseUp(evt);
    }
  }

  /**
   * Handle Mousedown. Save the actual clusterOrder index
   * 
   * @param mouseActIndex Index of the clusterOrderEntry where the event occured
   */
  private void handlePlotMouseDown(Event evt) {
    final SVGDocument doc = OPTICSPlotPlotVis.this.svgp.getDocument();
    int mouseActIndex = getSelectedIndex(this.order, evt, this.etag, doc);
    mouseDownIndex = mouseActIndex;
    if(mouseActIndex >= 0 && mouseActIndex < order.size()) {
      double width = StyleLibrary.SCALE / order.size();
      double x1 = mouseActIndex * width;
      Element marker = addMarkerRect(x1, width);
      SVGUtil.setCSSClass(marker, OPTICSPlotVisualizerFactory.CSS_RANGEMARKER);
      mtag.appendChild(marker);
    }
  }

  /**
   * Handle MouseUp. Update the selection
   * 
   * @param mouseActIndex Index of the clusterOrderEntry where the event occured
   */
  private void handlePlotMouseUp(Event evt) {
    if(mouseDownIndex == null) {
      return;
    }
    final SVGDocument doc = OPTICSPlotPlotVis.this.svgp.getDocument();
    int mouseActIndex = getSelectedIndex(this.order, evt, this.etag, doc);
    Mode mode = getInputMode(evt);
    final int begin = Math.max(Math.min(mouseDownIndex, mouseActIndex), 0);
    final int end = Math.min(Math.max(mouseDownIndex, mouseActIndex), order.size());
    updateSelection(mode, begin, end);

    mouseDownIndex = null;
  }

  /**
   * Handle Mousemove. Draw a rectangle between mouseDownIndex and actual Index
   * 
   * @param mouseActIndex Index of the clusterOrderEntry where the event occured
   */
  private void handlePlotMouseMove(Event evt) {
    if(mouseDownIndex == null) {
      return;
    }
    final SVGDocument doc = OPTICSPlotPlotVis.this.svgp.getDocument();
    int mouseActIndex = getSelectedIndex(this.order, evt, this.etag, doc);
    if(mouseActIndex >= 0 || mouseActIndex <= order.size() || mouseDownIndex >= 0 || mouseDownIndex <= order.size()) {
      double width = StyleLibrary.SCALE / order.size();
      double x1;
      double x2;
      if(mouseActIndex < mouseDownIndex) {
        x1 = mouseActIndex * width;
        x2 = (mouseDownIndex * width) + width;
      }
      else {
        x1 = mouseDownIndex * width;
        x2 = mouseActIndex * width + width;
      }
      mtag.removeChild(mtag.getLastChild());
      Element marker = addMarkerRect(x1, x2 - x1);
      SVGUtil.setCSSClass(marker, OPTICSPlotVisualizerFactory.CSS_RANGEMARKER);
      mtag.appendChild(marker);
    }
  }

  /**
   * Get the current input mode, on each mouse event.
   * 
   * @param evt Mouse event.
   * @return Input mode
   */
  private Mode getInputMode(Event evt) {
    if(evt instanceof DOMMouseEvent) {
      DOMMouseEvent domme = (DOMMouseEvent) evt;
      // TODO: visual indication of mode possible?
      if(domme.getShiftKey()) {
        return Mode.ADD;
      }
      else if(domme.getCtrlKey()) {
        return Mode.INVERT;
      }
      else {
        return Mode.REPLACE;
      }
    }
    // Default mode is replace.
    return Mode.REPLACE;
  }

  /**
   * Gets the Index of the ClusterOrderEntry where the event occured
   * 
   * @param order List of ClusterOrderEntries
   * @param evt Event
   * @param tag Related SVGElement
   * @param doc SVGDocument
   * @return Index of the object
   */
  private int getSelectedIndex(List<ClusterOrderEntry<D>> order, Event evt, Element tag, SVGDocument doc) {
    SVGPoint cPt = SVGUtil.elementCoordinatesFromEvent(doc, tag, evt);
    int mouseActIndex = (int) ((cPt.getX() / StyleLibrary.SCALE) * order.size());
    return mouseActIndex;
  }

  /**
   * Updates the selection for the given ClusterOrderEntry considering the
   * pressed keys. <br>
   * strg: add to selection, ctrl/shift: flip selection
   * 
   * @param mode Input mode
   * @param begin first index to select
   * @param end last index to select
   */
  protected void updateSelection(Mode mode, int begin, int end) {
    if(begin < 0 || begin > end || end >= order.size()) {
      logger.warning("Invalid range in updateSelection: " + begin + " .. " + end);
      return;
    }

    DBIDSelection selContext = context.getSelection();
    HashSetModifiableDBIDs selection;
    if(selContext == null || mode == Mode.REPLACE) {
      selection = DBIDUtil.newHashSet();
    }
    else {
      selection = DBIDUtil.newHashSet(selContext.getSelectedIds());
    }

    for(int i = begin; i <= end; i++) {
      DBID id = order.get(i).getID();
      if(mode == Mode.INVERT) {
        if(!selection.contains(id)) {
          selection.add(id);
        }
        else {
          selection.remove(id);
        }
      }
      else {
        // In REPLACE and ADD, add objects.
        // The difference was done before by not re-using the selection.
        // Since we are using a set, we can just add in any case.
        selection.add(id);
      }
    }
    context.setSelection(new DBIDSelection(selection));
  }
}