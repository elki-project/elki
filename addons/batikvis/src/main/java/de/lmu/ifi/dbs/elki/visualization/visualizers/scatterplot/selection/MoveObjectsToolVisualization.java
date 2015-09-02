package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.selection;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.UpdatableDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea;
import de.lmu.ifi.dbs.elki.visualization.batikutil.DragableArea.DragListener;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Tool to move the currently selected objects.
 *
 * @author Heidi Kolb
 * @author Erich Schubert
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class MoveObjectsToolVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Move Objects";

  /**
   * Constructor
   */
  public MoveObjectsToolVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Database db = ResultUtil.findDatabase(context.getHierarchy());
    if(!(db instanceof UpdatableDatabase)) {
      return;
    }
    Hierarchy.Iter<ScatterPlotProjector<?>> it = VisualizationTree.filter(context, start, ScatterPlotProjector.class);
    for(; it.valid(); it.advance()) {
      ScatterPlotProjector<?> p = it.get();
      final Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        continue;
      }
      final VisualizationTask task = new VisualizationTask(NAME, context, p.getRelation(), rel, MoveObjectsToolVisualization.this);
      task.level = VisualizationTask.LEVEL_INTERACTIVE;
      task.tool = true;
      task.addFlags(VisualizationTask.FLAG_NO_THUMBNAIL | VisualizationTask.FLAG_NO_EXPORT);
      task.addUpdateFlags(VisualizationTask.ON_DATA | VisualizationTask.ON_SAMPLE | VisualizationTask.ON_SELECTION);
      task.initDefaultVisibility(false);
      // baseResult.getHierarchy().add(p.getRelation(), task);
      context.addVis(p, task);
    }
  }

  /**
   * Instance.
   *
   * @author Heidi Kolb
   * @author Erich Schubert
   *
   * @apiviz.has de.lmu.ifi.dbs.elki.data.NumberVector oneway - - edits
   */
  public class Instance extends AbstractScatterplotVisualization implements DragListener {
    /**
     * CSS tag for our event rectangle
     */
    protected static final String CSS_ARROW = "moveArrow";

    /**
     * Element for the rectangle to add listeners
     */
    private Element etag;

    /**
     * Element to contain the drag arrow
     */
    private Element rtag;

    /**
     * Constructor.
     *
     * @param task Task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      addListeners();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      addCSSClasses(svgp);

      rtag = svgp.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.addCSSClass(rtag, CSS_ARROW);
      layer.appendChild(rtag);

      DragableArea drag = new DragableArea(svgp, -0.6 * StyleLibrary.SCALE, -0.7 * StyleLibrary.SCALE, 1.3 * StyleLibrary.SCALE, 1.4 * StyleLibrary.SCALE, this);
      etag = drag.getElement();
      layer.appendChild(etag);
    }

    /**
     * Updates the objects with the given DBIDs It will be moved depending on
     * the given Vector
     *
     * @param dbids - DBIDs of the objects to move
     * @param movingVector - Vector for moving object
     */
    // TODO: move to DatabaseUtil?
    private void updateDB(DBIDs dbids, Vector movingVector) {
      throw new AbortException("FIXME: INCOMPLETE TRANSITION");
      /*
       * NumberVector nv = null; database.accumulateDataStoreEvents();
       * Representation<DatabaseObjectMetadata> mrep =
       * database.getMetadataQuery(); for(DBID dbid : dbids) { NV obj =
       * database.get(dbid); // Copy metadata to keep DatabaseObjectMetadata
       * meta = mrep.get(dbid);
       *
       * Vector v = proj.projectDataToRenderSpace(obj); v.set(0, v.get(0) +
       * movingVector.get(0)); v.set(1, v.get(1) + movingVector.get(1)); NV nv =
       * proj.projectRenderToDataSpace(v, obj); nv.setID(obj.getID());
       *
       * try { database.delete(dbid); database.insert(new Pair<NV,
       * DatabaseObjectMetadata>(nv, meta)); } catch(UnableToComplyException e)
       * { de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e); } }
       * database.flushDataStoreEvents();
       */
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
     * Adds the required CSS-Classes
     *
     * @param svgp SVGPlot
     */
    private void addCSSClasses(SVGPlot svgp) {
      // Class for the rectangle to add eventListeners
      if(!svgp.getCSSClassManager().contains(CSS_ARROW)) {
        final CSSClass acls = new CSSClass(this, CSS_ARROW);
        final StyleLibrary style = context.getStyleLibrary();
        acls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, style.getColor(StyleLibrary.SELECTION_ACTIVE));
        acls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.SELECTION_ACTIVE));
        acls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        svgp.addCSSClassOrLogError(acls);
      }
    }

    @Override
    public boolean startDrag(SVGPoint startPoint, Event evt) {
      return true;
    }

    @Override
    public boolean duringDrag(SVGPoint startPoint, SVGPoint dragPoint, Event evt, boolean inside) {
      deleteChildren(rtag);
      rtag.appendChild(svgp.svgLine(startPoint.getX(), startPoint.getY(), dragPoint.getX(), dragPoint.getY()));
      return true;
    }

    @Override
    public boolean endDrag(SVGPoint startPoint, SVGPoint dragPoint, Event evt, boolean inside) {
      Vector movingVector = new Vector(2);
      movingVector.set(0, dragPoint.getX() - startPoint.getX());
      movingVector.set(1, dragPoint.getY() - startPoint.getY());
      if(context.getSelection() != null) {
        updateDB(context.getSelection().getSelectedIds(), movingVector);
      }
      deleteChildren(rtag);
      return true;
    }
  }
}