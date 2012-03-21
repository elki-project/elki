package experimentalcode.students.roedler.parallelCoordinates.visualizer.selection;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.geom.Line2D;
import java.util.ArrayList;

import org.apache.batik.dom.events.DOMMouseEvent;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import experimentalcode.students.roedler.parallelCoordinates.projections.ProjectionParallel;
import experimentalcode.students.roedler.parallelCoordinates.projector.ParallelPlotProjector;
import experimentalcode.students.roedler.parallelCoordinates.visualizer.ParallelVisualization;

/**
 * Tool-Visualization for the tool to select objects
 * 
 * @author Robert Rödler
 * 
 * @apiviz.has SelectionResult oneway - - updates
 * @apiviz.has DBIDSelection oneway - - updates
 * 
 * @param <NV> vector type
 */
public class SelectionToolLineVisualization extends ParallelVisualization<NumberVector<?, ?>> implements DragableArea.DragListener, DataStoreListener {
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

  int dim;

  /**
   * Constructor.
   * 
   * @param task Task
   */
  public SelectionToolLineVisualization(VisualizationTask task) {
    super(task);
    context.addDataStoreListener(this);
    incrementalRedraw();
    dim = DatabaseUtil.dimensionality(rep);
  }

  @Override
  public void destroy() {
    super.destroy();
    context.removeDataStoreListener(this);
  }

  @Override
  public void contentChanged(DataStoreEvent e) {
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
    DragableArea drag = new DragableArea(svgp, 0.0, getMarginY() / 2., getSizeX(), getMarginY() * 1.5 + getAxisHeight(), this);
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
  public boolean startDrag(SVGPoint startPoint, Event evt) {
    return true;
  }

  @Override
  public boolean duringDrag(SVGPoint startPoint, SVGPoint dragPoint, Event evt, boolean inside) {
    deleteChildren(rtag);
    double x = Math.min(startPoint.getX(), dragPoint.getX());
    double y = Math.min(startPoint.getY(), dragPoint.getY());
    double width = Math.abs(startPoint.getX() - dragPoint.getX());
    double height = Math.abs(startPoint.getY() - dragPoint.getY());
    rtag.appendChild(svgp.svgRect(x, y, width, height));
    return true;
  }

  @Override
  public boolean endDrag(SVGPoint startPoint, SVGPoint dragPoint, Event evt, boolean inside) {
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
  private void updateSelection(Mode mode, ProjectionParallel proj, SVGPoint p1, SVGPoint p2) {
    DBIDSelection selContext = context.getSelection();
    // Note: we rely on SET semantics below!
    final HashSetModifiableDBIDs selection;
    if(selContext == null || mode == Mode.REPLACE) {
      selection = DBIDUtil.newHashSet();
    }
    else {
      selection = DBIDUtil.newHashSet(selContext.getSelectedIds());
    }
    int[] axisrange = getAxisRange(Math.min(p1.getX(), p2.getX()), Math.max(p1.getX(), p2.getX()));
    for(DBID objId : rep.iterDBIDs()) {
      Vector yPos = getYPositions(objId);
      if(checkSelected(axisrange, yPos, Math.max(p1.getX(), p2.getX()), Math.min(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.min(p1.getY(), p2.getY()))) {
        if(mode == Mode.INVERT) {
          if(!selection.contains(objId)) {
            selection.add(objId);
          }
          else {
            selection.remove(objId);
          }
        }
        else {
          // In REPLACE and ADD, add objects.
          // The difference was done before by not re-using the selection.
          // Since we are using a set, we can just add in any case.
          selection.add(objId);
        }
      }
    }
    context.setSelection(new DBIDSelection(selection));
  }

  private int[] getAxisRange(double x1, double x2) {
    double dim = DatabaseUtil.dimensionality(rep);
    int minaxis = 0;
    int maxaxis = 0;
    boolean minx = true;
    boolean maxx = false;
    int count = -1;
    for(int i = 0; i < dim; i++) {
      if(proj.isVisible(i)) {
        if(minx && proj.getXpos(i) > x1) {
          minaxis = count;
          minx = false;
          maxx = true;
        }
        if(maxx && (proj.getXpos(i) > x2 || i == dim - 1)) {
          maxaxis = count + 1;
          if(i == dim - 1 && proj.getXpos(i) <= x2) {
            maxaxis++;
          }
          break;
        }
      }
      count = i;
    }
    return new int[] { minaxis, maxaxis };
  }

  private boolean checkSelected(int[] ar, Vector yPos, double x1, double x2, double y1, double y2) {
    if(ar[1] >= dim) {
      ar[1] = dim - 1;
    }
    for(int i = ar[0] + 1; i <= ar[1] - 1; i++) {
      if(proj.isVisible(i)) {
        if(yPos.get(i) <= y1 && yPos.get(i) >= y2) {
          return true;
        }

      }
    }
    Line2D.Double idline1 = new Line2D.Double(proj.getXpos(ar[0]), yPos.get(ar[0]), proj.getXpos(ar[0] + 1), yPos.get(ar[0] + 1));
    Line2D.Double idline2 = new Line2D.Double(proj.getXpos(ar[1] - 1), yPos.get(ar[1] - 1), proj.getXpos(ar[1]), yPos.get(ar[1]));
    Line2D.Double rectline1 = new Line2D.Double(x2, y1, x1, y1);
    Line2D.Double rectline2 = new Line2D.Double(x2, y1, x2, y2);
    Line2D.Double rectline3 = new Line2D.Double(x2, y2, x1, y2);
    Line2D.Double rectline4 = new Line2D.Double(x1, y1, x1, y2);
    if(idline1.intersectsLine(rectline1) || idline1.intersectsLine(rectline2) || idline1.intersectsLine(rectline3)) {
      return true;
    }
    if(idline2.intersectsLine(rectline1) || idline2.intersectsLine(rectline4) || idline2.intersectsLine(rectline3)) {
      return true;
    }
    return false;
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
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new SelectionToolLineVisualization(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      final ArrayList<SelectionResult> selectionResults = ResultUtil.filterResults(result, SelectionResult.class);
      for(SelectionResult selres : selectionResults) {
        IterableIterator<ParallelPlotProjector<?>> ps = ResultUtil.filteredResults(baseResult, ParallelPlotProjector.class);
        for(ParallelPlotProjector<?> p : ps) {
          final VisualizationTask task = new VisualizationTask(NAME, selres, p.getRelation(), this);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_INTERACTIVE);
          task.put(VisualizationTask.META_TOOL, true);
          task.put(VisualizationTask.META_NOTHUMB, true);
          task.put(VisualizationTask.META_NOEXPORT, true);
          task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
          baseResult.getHierarchy().add(selres, task);
          baseResult.getHierarchy().add(p, task);
        }
      }
    }
  }
}
