package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.ArrayList;

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
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;

/**
 * Tool-Visualization for the tool to select objects
 * 
 * @author Heidi Kolb
 * 
 * @apiviz.has SelectionResult oneway - - updates
 * @apiviz.has DBIDSelection oneway - - updates
 * 
 * @param <NV> vector type
 */
public class SelectionToolDotVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> implements DragableArea.DragListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Object Selection";

  /**
   * CSS class of the selection rectangle while selecting.
   */
  private static final String CSS_RANGEMARKER = "selectionRangeMarker";

  /**
   * Input modes
   * 
   * @apiviz.exclude
   */
  private enum Mode {
    REPLACE, ADD, INVERT
  }

  /**
   * Element for selection rectangle
   */
  Element rtag;

  /**
   * Element for the rectangle to add listeners
   */
  Element etag;

  /**
   * Our result
   */
  private SelectionResult result;

  public SelectionToolDotVisualization(VisualizationTask task) {
    super(task, VisualizationTask.LEVEL_INTERACTIVE);
    this.result = task.getResult();
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
      rcls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getColor(StyleLibrary.SELECTION_ACTIVE));
      rcls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION_ACTIVE));
      svgp.addCSSClassOrLogError(rcls);
    }
  }

  /**
   * Factory for tool visualizations for selecting objects
   * 
   * @author Heidi Kolb
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses SelectionToolDotVisualization - - «create»
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends P2DVisFactory<NV> {
    /**
     * Constructor
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new SelectionToolDotVisualization<NV>(task);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, Result result) {
      final ArrayList<SelectionResult> selectionResults = ResultUtil.filterResults(result, SelectionResult.class);
      for(SelectionResult selres : selectionResults) {
        final VisualizationTask task = new VisualizationTask(NAME, context, selres, this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
        task.put(VisualizationTask.META_TOOL, true);
        task.put(VisualizationTask.META_NOTHUMB, true);
        task.put(VisualizationTask.META_NOEXPORT, true);
        context.addVisualizer(selres, task);
      }
    }
  }
}
