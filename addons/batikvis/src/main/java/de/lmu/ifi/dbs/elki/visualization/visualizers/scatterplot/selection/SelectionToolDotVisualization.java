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

import org.apache.batik.dom.events.DOMMouseEvent;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.RenderFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Tool-Visualization for the tool to select objects
 *
 * @author Heidi Kolb
 * @since 0.4.0
 *
 * @stereotype factory
 * @composed - - - Mode
 * @assoc - create - Instance
 */
public class SelectionToolDotVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Object Selection";

  /**
   * Input modes
   */
  private enum Mode {
    REPLACE, ADD, INVERT
  }

  /**
   * Constructor.
   */
  public SelectionToolDotVisualization() {
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
      final VisualizationTask task = new VisualizationTask(this, NAME, context.getSelectionResult(), rel).level(VisualizationTask.LEVEL_INTERACTIVE) //
          .tool(true).visibility(false) //
          .with(RenderFlag.NO_THUMBNAIL).with(RenderFlag.NO_EXPORT) //
          .with(UpdateFlag.ON_DATA).with(UpdateFlag.ON_SELECTION);
      context.addVis(context.getSelectionResult(), task);
      context.addVis(p, task);
    });
  }

  /**
   * Instance
   *
   * @author Heidi Kolb
   *
   * @navhas - updates - DBIDSelection
   */
  public class Instance extends AbstractScatterplotVisualization implements DragableArea.DragListener {
    /**
     * CSS class of the selection rectangle while selecting.
     */
    private static final String CSS_RANGEMARKER = "selectionRangeMarker";

    /**
     * Element for selection rectangle
     */
    Element rtag;

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
      addListeners();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      addCSSClasses(svgp);

      layer.appendChild(rtag = svgp.svgElement(SVGConstants.SVG_G_TAG, CSS_RANGEMARKER));
      // etag: sensitive area
      layer.appendChild(new DragableArea(svgp, -0.6 * StyleLibrary.SCALE, -0.7 * StyleLibrary.SCALE, 1.3 * StyleLibrary.SCALE, 1.4 * StyleLibrary.SCALE, this).getElement());
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
        return domme.getShiftKey() ? Mode.ADD : domme.getCtrlKey() ? Mode.INVERT : Mode.REPLACE;
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
      DBIDSelection selContext = context.getSelection();
      // Note: we rely on SET semantics below!
      HashSetModifiableDBIDs selection = (selContext == null || mode == Mode.REPLACE) ? DBIDUtil.newHashSet() //
          : DBIDUtil.newHashSet(selContext.getSelectedIds());
      for(DBIDIter iditer = rel.iterDBIDs(); iditer.valid(); iditer.advance()) {
        double[] vec = proj.fastProjectDataToRenderSpace(rel.get(iditer));
        if(vec[0] >= Math.min(p1.getX(), p2.getX()) && vec[0] <= Math.max(p1.getX(), p2.getX()) && vec[1] >= Math.min(p1.getY(), p2.getY()) && vec[1] <= Math.max(p1.getY(), p2.getY())) {
          if(mode == Mode.INVERT) {
            if(!selection.add(iditer)) {
              selection.remove(iditer);
            }
          }
          else {
            // In REPLACE and ADD, add objects.
            // The difference was done before by not re-using the selection.
            // Since we are using a set, we can just add in any case.
            selection.add(iditer);
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
  }
}
