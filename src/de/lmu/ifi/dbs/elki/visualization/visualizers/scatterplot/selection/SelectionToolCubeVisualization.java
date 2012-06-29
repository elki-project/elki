package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.selection;

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

import java.util.BitSet;
import java.util.Collection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.RangeSelection;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Tool-Visualization for the tool to select ranges
 * 
 * @author Heidi Kolb
 * 
 * @apiviz.has SelectionResult oneway - - updates
 * @apiviz.has RangeSelection oneway - - updates
 */
public class SelectionToolCubeVisualization extends AbstractScatterplotVisualization implements DragableArea.DragListener {
  /**
   * The logger for this class.
   */
  protected static final Logging logger = Logging.getLogger(SelectionToolCubeVisualization.class);

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Range Selection";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  private static final String CSS_RANGEMARKER = "selectionRangeMarker";

  /**
   * Dimension
   */
  private int dim;

  /**
   * Element for selection rectangle
   */
  private Element rtag;

  /**
   * Element for the rectangle to add listeners
   */
  private Element etag;

  /**
   * Constructor.
   * 
   * @param task Task
   */
  public SelectionToolCubeVisualization(VisualizationTask task) {
    super(task);
    this.dim = DatabaseUtil.dimensionality(rel);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    addCSSClasses(svgp);

    // rtag: tag for the selected rect
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

  /**
   * Set the selected ranges and the mask for the actual dimensions in the
   * context
   * 
   * @param x1 x-value of the first dimension
   * @param x2 x-value of the second dimension
   * @param y1 y-value of the first dimension
   * @param y2 y-value of the second dimension
   */
  private void updateSelectionRectKoordinates(double x1, double x2, double y1, double y2, DoubleDoublePair[] ranges) {
    BitSet actDim = proj.getVisibleDimensions2D();
    double[] v1 = new double[dim];
    double[] v2 = new double[dim];
    v1[0] = x1;
    v1[1] = y1;
    v2[0] = x2;
    v2[1] = y2;

    double[] nv1 = proj.fastProjectRenderToDataSpace(v1);
    double[] nv2 = proj.fastProjectRenderToDataSpace(v2);

    for(int d = actDim.nextSetBit(0); d >= 0; d = actDim.nextSetBit(d + 1)) {
      ranges[d] = new DoubleDoublePair(Math.min(nv1[d], nv2[d]), Math.max(nv1[d], nv2[d]));
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
    deleteChildren(rtag);
    if(startPoint.getX() != dragPoint.getX() || startPoint.getY() != dragPoint.getY()) {
      updateSelection(proj, startPoint, dragPoint);
    }
    return true;
  }

  /**
   * Update the selection in the context.
   * 
   * @param proj The projection
   * @param p1 First Point of the selected rectangle
   * @param p2 Second Point of the selected rectangle
   */
  private void updateSelection(Projection proj, SVGPoint p1, SVGPoint p2) {
    if(p1 == null || p2 == null) {
      logger.warning("no rect selected: p1: " + p1 + " p2: " + p2);
      return;
    }

    DBIDSelection selContext = context.getSelection();
    ModifiableDBIDs selection;
    if(selContext != null) {
      selection = DBIDUtil.newHashSet(selContext.getSelectedIds());
    }
    else {
      selection = DBIDUtil.newHashSet();
    }
    DoubleDoublePair[] ranges;

    double x1 = Math.min(p1.getX(), p2.getX());
    double x2 = Math.max(p1.getX(), p2.getX());
    double y1 = Math.max(p1.getY(), p2.getY());
    double y2 = Math.min(p1.getY(), p2.getY());

    if(selContext instanceof RangeSelection) {
      ranges = ((RangeSelection) selContext).getRanges();
    }
    else {
      ranges = new DoubleDoublePair[dim];
    }
    updateSelectionRectKoordinates(x1, x2, y1, y2, ranges);

    selection.clear();
    candidates: for(DBIDIter iditer = rel.iterDBIDs(); iditer.valid(); iditer.advance()) {
      NumberVector<?, ?> dbTupel = rel.get(iditer);
      for(int i = 0; i < dim; i++) {
        if(ranges != null && ranges[i] != null) {
          if(dbTupel.doubleValue(i + 1) < ranges[i].first || dbTupel.doubleValue(i + 1) > ranges[i].second) {
            continue candidates;
          }
        }
      }
      selection.add(iditer);
    }
    context.setSelection(new RangeSelection(selection, ranges));
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
   * Factory for tool visualizations for selecting ranges and the inclosed
   * objects
   * 
   * @author Heidi Kolb
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses SelectionToolCubeVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new SelectionToolCubeVisualization(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Collection<SelectionResult> selectionResults = ResultUtil.filterResults(result, SelectionResult.class);
      for(SelectionResult selres : selectionResults) {
        Collection<ScatterPlotProjector<?>> ps = ResultUtil.filterResults(baseResult, ScatterPlotProjector.class);
        for(ScatterPlotProjector<?> p : ps) {
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