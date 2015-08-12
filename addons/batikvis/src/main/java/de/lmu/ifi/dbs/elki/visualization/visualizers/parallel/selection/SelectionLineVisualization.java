package de.lmu.ifi.dbs.elki.visualization.visualizers.parallel.selection;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ParallelPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.parallel.AbstractParallelVisualization;

/**
 * Visualizer for generating SVG-Elements representing the selected objects
 *
 * @author Robert Rödler
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class SelectionLineVisualization extends AbstractVisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Selection Line";

  /**
   * Constructor.
   */
  public SelectionLineVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Hierarchy.Iter<ParallelPlotProjector<?>> it = VisualizationTree.filter(context, start, ParallelPlotProjector.class);
    for(; it.valid(); it.advance()) {
      ParallelPlotProjector<?> p = it.get();
      final VisualizationTask task = new VisualizationTask(NAME, context, context.getSelectionResult(), p.getRelation(), SelectionLineVisualization.this);
      task.level = VisualizationTask.LEVEL_DATA - 1;
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
   */
  public class Instance extends AbstractParallelVisualization<NumberVector>implements DataStoreListener {
    /**
     * CSS Class for the range marker
     */
    public static final String MARKER = "SelectionLine";

    /**
     * Constructor.
     *
     * @param task Visualization task
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
      addCSSClasses(svgp);
      DBIDSelection selContext = context.getSelection();
      if(selContext != null) {
        DBIDs selection = selContext.getSelectedIds();

        for(DBIDIter iter = selection.iter(); iter.valid(); iter.advance()) {
          Element marker = drawLine(iter);
          if(marker == null) {
            continue;
          }
          SVGUtil.addCSSClass(marker, MARKER);
          layer.appendChild(marker);
        }
      }
    }

    /**
     * Draw a single line.
     *
     * @param iter Object reference
     * @return SVG Element
     */
    private Element drawLine(DBIDRef iter) {
      SVGPath path = new SVGPath();
      double[] yPos = proj.fastProjectDataToRenderSpace(relation.get(iter));
      boolean draw = false, drawprev = false, drawn = false;
      for(int i = 0; i < yPos.length; i++) {
        // NaN handling:
        if(yPos[i] != yPos[i]) {
          draw = false;
          drawprev = false;
          continue;
        }
        if(draw) {
          if(drawprev) {
            path.moveTo(getVisibleAxisX(i - 1), yPos[i - 1]);
            drawprev = false;
          }
          path.lineTo(getVisibleAxisX(i), yPos[i]);
          drawn = true;
        }
        else {
          drawprev = true;
        }
        draw = true;
      }
      if(!drawn) {
        return null; // Not enough data.
      }
      return path.makeElement(svgp);
    }

    /**
     * Adds the required CSS-Classes
     *
     * @param svgp SVG-Plot
     */
    private void addCSSClasses(SVGPlot svgp) {
      final StyleLibrary style = context.getStyleLibrary();
      // Class for the cube
      if(!svgp.getCSSClassManager().contains(MARKER)) {
        CSSClass cls = new CSSClass(this, MARKER);
        cls.setStatement(SVGConstants.CSS_STROKE_VALUE, style.getColor(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_STROKE_OPACITY_PROPERTY, style.getOpacity(StyleLibrary.SELECTION));
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT) * 2.);
        cls.setStatement(SVGConstants.CSS_STROKE_LINECAP_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        cls.setStatement(SVGConstants.CSS_STROKE_LINEJOIN_PROPERTY, SVGConstants.CSS_ROUND_VALUE);
        cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        svgp.addCSSClassOrLogError(cls);
      }
    }
  }
}