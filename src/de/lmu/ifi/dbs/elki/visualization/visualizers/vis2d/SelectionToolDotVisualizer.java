package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.apache.batik.dom.events.DOMMouseEvent;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;

/**
 * Factory for tool visualizations for selecting objects
 * 
 * @author Heidi Kolb
 * 
 * @apiviz.has de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.SelectionToolDotVisualizer.SelectionToolDotVisualization oneway - - produces
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class SelectionToolDotVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Object Selection";

  /**
   * Input modes
   * 
   * @apiviz.exclude
   */
  private enum Mode {
    REPLACE, ADD, INVERT
  }

  /**
   * Constructor
   */
  public SelectionToolDotVisualizer() {
    super(NAME);
    super.metadata.put(Visualizer.META_TOOL, true);
    super.metadata.put(Visualizer.META_GROUP, Visualizer.GROUP_TOOLS);
    super.metadata.put(Visualizer.META_NOTHUMB, true);
    super.metadata.put(Visualizer.META_NOEXPORT, true);
  }

  @Override
  public Visualization visualize(SVGPlot svgp, Projection2D proj, double width, double height) {
    return new SelectionToolDotVisualization(context, svgp, proj, width, height);
  }

  /**
   * Tool-Visualization for the tool to select objects
   * 
   * @author Heidi Kolb
   * 
   * @apiviz.uses de.lmu.ifi.dbs.elki.visualization.visualizers.DBIDSelection oneway - - updates
   */
  public class SelectionToolDotVisualization extends Projection2DVisualization<NV> implements DragableArea.DragListener {
    /**
     * CSS class of the selection rectangle while selecting.
     */
    private static final String CSS_RANGEMARKER = "selectionRangeMarker";

    /**
     * Element for selection rectangle
     */
    Element rtag;

    /**
     * Element for the rectangle to add listeners
     */
    Element etag;

    /**
     * The actual visualization instance, for a single projection
     * 
     * @param context The context
     * @param svgp The SVGPlot
     * @param proj The Projection
     * @param width The width
     * @param height The height
     */
    public SelectionToolDotVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, Projection2D proj, double width, double height) {
      super(context, svgp, proj, width, height, Visualizer.LEVEL_INTERACTIVE);
      context.addContextChangeListener(this);
      incrementalRedraw();
    }

    @Override
    public void destroy() {
      super.destroy();
      context.removeContextChangeListener(this);
    }

    @Override
    public void contextChanged(@SuppressWarnings("unused") ContextChangedEvent e) {
      synchronizedRedraw();
    }

    @Override
    protected void redraw() {
      addCSSClasses(svgp);

      // 
      rtag = svgp.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.addCSSClass(rtag, CSS_RANGEMARKER);
      layer.appendChild(rtag);

      // etag: sensitive area
      DragableArea drag = new DragableArea(svgp, -0.6 * StyleLibrary.SCALE, -0.7 * StyleLibrary.SCALE, 1.3 * StyleLibrary.SCALE, 1.4 * StyleLibrary.SCALE, this);
      etag = drag.getElement();
      layer.appendChild(etag);
    }

    /**
     * Delete the children of the element
     * 
     * @param container SVG-Element
     */
    private void deleteChildren(Element container) {
      while(container.hasChildNodes()) {
        container.removeChild(container.getLastChild());
      }
    }

    @Override
    public boolean startDrag(@SuppressWarnings("unused") SVGPoint startPoint, @SuppressWarnings("unused") Event evt) {
      return true;
    }

    @Override
    public boolean duringDrag(SVGPoint startPoint, SVGPoint dragPoint, @SuppressWarnings("unused") Event evt, @SuppressWarnings("unused") boolean inside) {
      deleteChildren(rtag);
      double x = Math.min(startPoint.getX(), dragPoint.getX());
      double y = Math.min(startPoint.getY(), dragPoint.getY());
      double width = Math.abs(startPoint.getX() - dragPoint.getX());
      double height = Math.abs(startPoint.getY() - dragPoint.getY());
      rtag.appendChild(svgp.svgRect(x, y, width, height));
      return true;
    }

    @Override
    public boolean endDrag(SVGPoint startPoint, SVGPoint dragPoint, Event evt, @SuppressWarnings("unused") boolean inside) {
      Mode mode = getInputMode(evt);
      deleteChildren(rtag);
      if(startPoint.getX() != dragPoint.getX() || startPoint.getY() != dragPoint.getY()) {
        updateSelection(mode, proj, startPoint, dragPoint);
      }
      return true;
    }

    /**
     * Get the current input mode, on each mouse event.
     * 
     * @param evt Mouse event.
     * @return current input mode
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
     * Updates the selection in the context.<br>
     * 
     * @param mode Input mode
     * @param proj
     * @param p1 first point of the selected rectangle
     * @param p2 second point of the selected rectangle
     */
    private void updateSelection(Mode mode, Projection2D proj, SVGPoint p1, SVGPoint p2) {
      Database<? extends NV> database = context.getDatabase();
      DBIDSelection selContext = context.getSelection();
      // Note: we rely on SET semantics below!
      HashSetModifiableDBIDs selection;
      if(selContext == null || mode == Mode.REPLACE) {
        selection = DBIDUtil.newHashSet();
      }
      else {
        selection = DBIDUtil.newHashSet(selContext.getSelectedIds());
      }
      for(DBID id : database) {
        double[] vec = proj.fastProjectDataToRenderSpace(database.get(id));
        if(vec[0] >= Math.min(p1.getX(), p2.getX()) && vec[0] <= Math.max(p1.getX(), p2.getX()) && vec[1] >= Math.min(p1.getY(), p2.getY()) && vec[1] <= Math.max(p1.getY(), p2.getY())) {
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
      }
      context.setSelection(new DBIDSelection(selection));
    }

    /**
     * Adds the required CSS-Classes
     * 
     * @param svgp SVG-Plot
     */
    protected void addCSSClasses(SVGPlot svgp) {
      // Class for the range marking
      if(!svgp.getCSSClassManager().contains(CSS_RANGEMARKER)) {
        final CSSClass rcls = new CSSClass(this, CSS_RANGEMARKER);
        final StyleLibrary style = context.getStyleLibrary();
        rcls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style .getColor(StyleLibrary.SELECTION_ACTIVE));
        rcls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION_ACTIVE));
        svgp.addCSSClassOrLogError(rcls);
      }
    }
  }
}
