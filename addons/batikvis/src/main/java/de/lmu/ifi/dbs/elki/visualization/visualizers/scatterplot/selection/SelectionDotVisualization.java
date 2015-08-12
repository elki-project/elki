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

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
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
 * Visualizer for generating an SVG-Element containing dots as markers
 * representing the selected Database's objects.
 *
 * @author Heidi Kolb
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class SelectionDotVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Selection";

  /**
   * Constructor
   */
  public SelectionDotVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Hierarchy.Iter<ScatterPlotProjector<?>> it = VisualizationTree.filter(context, start, ScatterPlotProjector.class);
    for(; it.valid(); it.advance()) {
      ScatterPlotProjector<?> p = it.get();
      final VisualizationTask task = new VisualizationTask(NAME, context, context.getSelectionResult(), p.getRelation(), SelectionDotVisualization.this);
      task.level = VisualizationTask.LEVEL_DATA - 1;
      task.addUpdateFlags(VisualizationTask.ON_DATA | VisualizationTask.ON_SELECTION);
      context.addVis(context.getSelectionResult(), task);
      context.addVis(p, task);
    }
  }

  /**
   * Instance
   *
   * @author Heidi Kolb
   *
   * @apiviz.has SelectionResult oneway - - visualizes
   * @apiviz.has DBIDSelection oneway - - visualizes
   */
  public class Instance extends AbstractScatterplotVisualization implements DataStoreListener {
    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String MARKER = "selectionDotMarker";

    /**
     * Constructor.
     *
     * @param task Task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      addListeners();
    }

    @Override
    protected void redraw() {
      super.redraw();
      final StyleLibrary style = context.getStyleLibrary();
      addCSSClasses(svgp);
      final double size = style.getSize(StyleLibrary.SELECTION);
      DBIDSelection selContext = context.getSelection();
      if(selContext != null) {
        DBIDs selection = selContext.getSelectedIds();
        for(DBIDIter iter = selection.iter(); iter.valid(); iter.advance()) {
          try {
            double[] v = proj.fastProjectDataToRenderSpace(rel.get(iter));
            if(v[0] != v[0] || v[1] != v[1]) {
              continue; // NaN!
            }
            Element dot = svgp.svgCircle(v[0], v[1], size);
            SVGUtil.addCSSClass(dot, MARKER);
            layer.appendChild(dot);
          }
          catch(ObjectNotFoundException e) {
            // ignore
          }
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
      if(!svgp.getCSSClassManager().contains(MARKER)) {
        CSSClass cls = new CSSClass(this, MARKER);
        final StyleLibrary style = context.getStyleLibrary();
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getColor(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
        svgp.addCSSClassOrLogError(cls);
      }
    }
  }
}