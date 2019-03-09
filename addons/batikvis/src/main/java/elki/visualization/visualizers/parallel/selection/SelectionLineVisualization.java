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
package elki.visualization.visualizers.parallel.selection;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import elki.data.NumberVector;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreListener;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.result.DBIDSelection;
import elki.visualization.VisualizationTask;
import elki.visualization.VisualizationTask.UpdateFlag;
import elki.visualization.VisualizationTree;
import elki.visualization.VisualizerContext;
import elki.visualization.css.CSSClass;
import elki.visualization.gui.VisualizationPlot;
import elki.visualization.projections.Projection;
import elki.visualization.projector.ParallelPlotProjector;
import elki.visualization.style.StyleLibrary;
import elki.visualization.svg.SVGPath;
import elki.visualization.svg.SVGPlot;
import elki.visualization.svg.SVGUtil;
import elki.visualization.visualizers.VisFactory;
import elki.visualization.visualizers.Visualization;
import elki.visualization.visualizers.parallel.AbstractParallelVisualization;

/**
 * Visualizer for generating SVG-Elements representing the selected objects
 *
 * @author Robert Rödler
 * @since 0.5.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class SelectionLineVisualization implements VisFactory {
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
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findVis(context, start).filter(ParallelPlotProjector.class).forEach(p -> {
      Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        return;
      }
      final VisualizationTask task = new VisualizationTask(this, NAME, context.getSelectionResult(), rel) //
          .level(VisualizationTask.LEVEL_DATA - 1) //
          .with(UpdateFlag.ON_DATA).with(UpdateFlag.ON_SELECTION);
      context.addVis(context.getSelectionResult(), task);
      context.addVis(p, task);
    });
  }

  /**
   * Instance
   *
   * @author Robert Rödler
   *
   * @navhas - visualizes - DBIDSelection
   */
  public class Instance extends AbstractParallelVisualization<NumberVector> implements DataStoreListener {
    /**
     * CSS Class for the range marker
     */
    public static final String MARKER = "SelectionLine";

    /**
     * Constructor.
     *
     * @param context Visualizer context
     * @param task Visualization task
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
      super.fullRedraw();
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
