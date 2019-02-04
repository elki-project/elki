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
package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot;

import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.ReferencePointsResult;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * The actual visualization instance, for a single projection
 *
 * @author Remigius Wojdanowski
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class ReferencePointsVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Reference Points";

  /**
   * Constructor.
   */
  public ReferencePointsVisualization() {
    super();
  }

  @Override
  public void processNewResult(VisualizerContext context, Object result) {
    VisualizationTree.findNewSiblings(context, result, ReferencePointsResult.class, ScatterPlotProjector.class, (rp, p) -> {
      final Relation<?> rel = p.getRelation();
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        return;
      }
      // FIXME: Make sure the reference points belong to this relation, and have
      // the same dimensionality. How can we encode this information?
      final VisualizationTask task = new VisualizationTask(this, NAME, rp, rel) //
          .level(VisualizationTask.LEVEL_DATA);
      context.addVis(rp, task);
      context.addVis(p, task);
    });
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  /**
   * Instance.
   *
   * @author Remigius Wojdanowski
   * @author Erich Schubert
   *
   * @navhas - visualizes - ReferencePointsResult
   */
  // TODO: add a result listener for the reference points.
  public class Instance extends AbstractScatterplotVisualization {
    /**
     * Generic tag to indicate the type of element. Used in IDs, CSS-Classes
     * etc.
     */
    public static final String REFPOINT = "refpoint";

    /**
     * Serves reference points.
     */
    protected ReferencePointsResult<? extends NumberVector> result;

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
      this.result = task.getResult();
      addListeners();
    }

    @Override
    public void fullRedraw() {
      setupCanvas();
      final StyleLibrary style = context.getStyleLibrary();
      setupCSS(svgp);
      Iterator<? extends NumberVector> iter = result.iterator();

      final double dotsize = style.getSize(StyleLibrary.REFERENCE_POINTS);
      while(iter.hasNext()) {
        NumberVector v = iter.next();
        double[] projected = proj.fastProjectDataToRenderSpace(v);
        Element dot = svgp.svgCircle(projected[0], projected[1], dotsize);
        SVGUtil.addCSSClass(dot, REFPOINT);
        layer.appendChild(dot);
      }
    }

    /**
     * Registers the Reference-Point-CSS-Class at a SVGPlot.
     *
     * @param svgp the SVGPlot to register the -CSS-Class.
     */
    private void setupCSS(SVGPlot svgp) {
      final StyleLibrary style = context.getStyleLibrary();
      CSSClass refpoint = new CSSClass(svgp, REFPOINT);
      refpoint.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getColor(StyleLibrary.REFERENCE_POINTS));
      svgp.addCSSClassOrLogError(refpoint);
    }
  }
}
