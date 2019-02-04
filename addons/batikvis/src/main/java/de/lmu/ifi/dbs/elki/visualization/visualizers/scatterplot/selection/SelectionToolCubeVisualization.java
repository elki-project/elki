/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.selection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.RangeSelection;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.RenderFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Tool-Visualization for the tool to select ranges.
 *
 * TODO: support non-point spatial data
 *
 * @author Heidi Kolb
 * @since 0.4.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class SelectionToolCubeVisualization implements VisFactory {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SelectionToolCubeVisualization.class);

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Range Selection";

  /**
   * Constructor.
   */
  public SelectionToolCubeVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findVis(context, start).filter(ScatterPlotProjector.class).forEach(p -> {
      Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        return;
      }
      final VisualizationTask task = new VisualizationTask(this, NAME, context.getSelectionResult(), rel) //
          .level(VisualizationTask.LEVEL_INTERACTIVE) //
          .tool(true).visibility(false)//
          .with(RenderFlag.NO_THUMBNAIL).with(RenderFlag.NO_EXPORT).with(UpdateFlag.ON_SELECTION);
      context.addVis(context.getSelectionResult(), task);
      context.addVis(p, task);
    });
  }

  /**
   * Instance.
   *
   * @author Heidi Kolb
   *
   * @navhas - updates - RangeSelection
   */
  public class Instance extends AbstractScatterplotVisualization implements DragableArea.DragListener {
    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    private static final String CSS_RANGEMARKER = "selectionRangeMarker";

    /**
     * Dimension.
     */
    private int dim;

    /**
     * Element for selection rectangle.
     */
    private Element rtag;

    /**
     * Constructor.
     *
     * @param context Visualizer context
     * @param task Task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(context, task, plot, width, height, proj);
      this.dim = RelationUtil.dimensionality(rel);
      addListeners();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      addCSSClasses(svgp);

      // rtag: tag for the selected rect
      layer.appendChild(rtag = svgp.svgElement(SVGConstants.SVG_G_TAG, CSS_RANGEMARKER));
      // etag: sensitive area
      layer.appendChild(new DragableArea(svgp, -0.6 * StyleLibrary.SCALE, -0.7 * StyleLibrary.SCALE, 1.3 * StyleLibrary.SCALE, 1.4 * StyleLibrary.SCALE, this).getElement());
    }

    /**
     * Delete the children of the element.
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
     * context.
     *
     * @param x1 x-value of the first dimension
     * @param x2 x-value of the second dimension
     * @param y1 y-value of the first dimension
     * @param y2 y-value of the second dimension
     * @param ranges Ranges to update
     */
    private void updateSelectionRectKoordinates(double x1, double x2, double y1, double y2, ModifiableHyperBoundingBox ranges) {
      double[] nv1 = proj.fastProjectRenderToDataSpace(x1, y1);
      double[] nv2 = proj.fastProjectRenderToDataSpace(x2, y2);

      long[] actDim = proj.getVisibleDimensions2D();
      for(int d = BitsUtil.nextSetBit(actDim, 0); d >= 0; d = BitsUtil.nextSetBit(actDim, d + 1)) {
        ranges.setMin(d, Math.min(nv1[d], nv2[d]));
        ranges.setMax(d, Math.max(nv1[d], nv2[d]));
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
        updateSelection(startPoint, dragPoint);
      }
      return true;
    }

    /**
     * Update the selection in the context.
     *
     * @param p1 First Point of the selected rectangle
     * @param p2 Second Point of the selected rectangle
     */
    private void updateSelection(SVGPoint p1, SVGPoint p2) {
      if(p1 == null || p2 == null) {
        LOG.warning("no rect selected: p1: " + p1 + " p2: " + p2);
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
      ModifiableHyperBoundingBox ranges;

      double x1 = Math.min(p1.getX(), p2.getX());
      double x2 = Math.max(p1.getX(), p2.getX());
      double y1 = Math.max(p1.getY(), p2.getY());
      double y2 = Math.min(p1.getY(), p2.getY());

      if(selContext instanceof RangeSelection) {
        ranges = ((RangeSelection) selContext).getRanges();
      }
      else {
        ranges = new ModifiableHyperBoundingBox(dim, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
      }
      updateSelectionRectKoordinates(x1, x2, y1, y2, ranges);

      selection.clear();
      candidates: for(DBIDIter iditer = rel.iterDBIDs(); iditer.valid(); iditer.advance()) {
        NumberVector dbTupel = rel.get(iditer);
        for(int i = 0; i < dim; i++) {
          final double min = ranges.getMin(i), max = ranges.getMax(i);
          if(max < Double.POSITIVE_INFINITY || min > Double.NEGATIVE_INFINITY) {
            final double v = dbTupel.doubleValue(i);
            if(v < min || v > max) {
              continue candidates;
            }
          }
        }
        selection.add(iditer);
      }
      context.setSelection(new RangeSelection(selection, ranges));
    }

    /**
     * Adds the required CSS-Classes.
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
  }
}
