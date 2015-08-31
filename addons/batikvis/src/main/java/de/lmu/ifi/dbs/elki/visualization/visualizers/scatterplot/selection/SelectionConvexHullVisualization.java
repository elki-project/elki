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

import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.math.geometry.GrahamScanConvexHull2D;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * Visualizer for generating an SVG-Element containing the convex hull of the
 * selected points
 *
 * @author Robert Rödler
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class SelectionConvexHullVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Convex Hull of Selection";

  /**
   * Constructor
   */
  public SelectionConvexHullVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Hierarchy.Iter<ScatterPlotProjector<?>> it = VisualizationTree.filter(context, start, ScatterPlotProjector.class);
    for(; it.valid(); it.advance()) {
      ScatterPlotProjector<?> p = it.get();
      final VisualizationTask task = new VisualizationTask(NAME, context, context.getSelectionResult(), p.getRelation(), SelectionConvexHullVisualization.this);
      task.level = VisualizationTask.LEVEL_DATA - 2;
      task.addUpdateFlags(VisualizationTask.ON_DATA | VisualizationTask.ON_SELECTION);
      context.addVis(context.getSelectionResult(), task);
      context.addVis(p, task);
    }
  }

  /**
   * Instance
   *
   * @author Robert Rödler
   *
   * @apiviz.has SelectionResult oneway - - visualizes
   * @apiviz.has DBIDSelection oneway - - visualizes
   * @apiviz.uses GrahamScanConvexHull2D
   */
  public class Instance extends AbstractScatterplotVisualization implements DataStoreListener {
    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String SELECTEDHULL = "selectionConvexHull";

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
      DBIDSelection selContext = context.getSelection();
      if(selContext != null) {
        DBIDs selection = selContext.getSelectedIds();
        GrahamScanConvexHull2D hull = new GrahamScanConvexHull2D();
        for(DBIDIter iter = selection.iter(); iter.valid(); iter.advance()) {
          try {
            final double[] v = proj.fastProjectDataToRenderSpace(rel.get(iter));
            if(v[0] != v[0] || v[1] != v[1]) {
              continue; // NaN!
            }
            hull.add(new Vector(v));
          }
          catch(ObjectNotFoundException e) {
            // ignore
          }
        }
        Polygon chres = hull.getHull();
        if(chres != null && chres.size() >= 3) {
          SVGPath path = new SVGPath(chres);

          Element selHull = path.makeElement(svgp);
          SVGUtil.addCSSClass(selHull, SELECTEDHULL);
          // TODO: use relative selection size for opacity?
          layer.appendChild(selHull);
        }
      }
    }

    /**
     * Adds the required CSS-Classes
     *
     * @param svgp SVG-Plot
     */
    private void addCSSClasses(SVGPlot svgp) {
      // Class for the dot markers
      if(!svgp.getCSSClassManager().contains(SELECTEDHULL)) {
        final StyleLibrary style = context.getStyleLibrary();
        CSSClass cls = new CSSClass(this, SELECTEDHULL);
        // cls = new CSSClass(this, CONVEXHULL);
        cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, style.getColor(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getColor(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, ".25");
        cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        svgp.addCSSClassOrLogError(cls);
      }
    }
  }
}