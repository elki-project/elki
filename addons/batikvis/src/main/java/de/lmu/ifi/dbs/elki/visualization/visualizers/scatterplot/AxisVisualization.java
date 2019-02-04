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

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

import net.jafama.FastMath;

/**
 * Generates a SVG-Element containing axes, including labeling.
 *
 * @author Remigius Wojdanowski
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class AxisVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Axes";

  /**
   * Constructor.
   */
  public AxisVisualization() {
    super();
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    VisualizationTree.findVis(context, start).filter(ScatterPlotProjector.class).forEach(p -> {
      context.addVis(p, new VisualizationTask(this, NAME, p.getRelation(), p.getRelation()) //
          .level(VisualizationTask.LEVEL_BACKGROUND));
    });
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }

  /**
   * Instance.
   *
   * @author Erich Schubert
   * @author Remigius Wojdanowski
   *
   * @assoc - - - SVGSimpleLinearAxis
   *
   */
  public class Instance extends AbstractScatterplotVisualization {
    /**
     * Constructor.
     *
     * @param context Visualizer context
     * @param task VisualizationTask
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
      final StyleLibrary style = context.getStyleLibrary();
      final int dim = RelationUtil.dimensionality(rel);

      // origin
      double[] orig = proj.fastProjectScaledToRenderSpace(new double[dim]);
      // diagonal point opposite to origin
      double[] diag = new double[dim];
      for(int d2 = 0; d2 < dim; d2++) {
        diag[d2] = 1;
      }
      diag = proj.fastProjectScaledToRenderSpace(diag);
      // compute angle to diagonal line, used for axis labeling.
      double diaga = FastMath.atan2(diag[1] - orig[1], diag[0] - orig[0]);

      double alfontsize = 1.1 * style.getTextSize(StyleLibrary.AXIS_LABEL);
      CSSClass alcls = new CSSClass(AxisVisualization.class, "unmanaged");
      alcls.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, SVGUtil.fmt(alfontsize));
      alcls.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getTextColor(StyleLibrary.AXIS_LABEL));
      alcls.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, style.getFontFamily(StyleLibrary.AXIS_LABEL));

      // draw axes
      for(int d = 0; d < dim; d++) {
        double[] v = new double[dim];
        v[d] = 1;
        // projected endpoint of axis
        double[] ax = proj.fastProjectScaledToRenderSpace(v);
        boolean righthand = false;
        double axa = FastMath.atan2(ax[1] - orig[1], ax[0] - orig[0]);
        if(axa > diaga || (diaga > 0 && axa > diaga + Math.PI)) {
          righthand = true;
        }
        // System.err.println(ax.get(0) + " "+ ax.get(1)+
        // " "+(axa*180/Math.PI)+" "+(diaga*180/Math.PI));
        if(ax[0] != orig[0] || ax[1] != orig[1]) {
          try {
            SVGSimpleLinearAxis.drawAxis(svgp, layer, proj.getScale(d), orig[0], orig[1], ax[0], ax[1], righthand ? SVGSimpleLinearAxis.LabelStyle.RIGHTHAND : SVGSimpleLinearAxis.LabelStyle.LEFTHAND, style);
            // TODO: move axis labeling into drawAxis function.
            double offx = (righthand ? 1 : -1) * 0.02 * Projection.SCALE;
            double offy = (righthand ? 1 : -1) * 0.02 * Projection.SCALE;
            Element label = svgp.svgText(ax[0] + offx, ax[1] + offy, RelationUtil.getColumnLabel(rel, d));
            SVGUtil.setAtt(label, SVGConstants.SVG_STYLE_ATTRIBUTE, alcls.inlineCSS());
            SVGUtil.setAtt(label, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, righthand ? SVGConstants.SVG_START_VALUE : SVGConstants.SVG_END_VALUE);
            layer.appendChild(label);
          }
          catch(CSSNamingConflict e) {
            throw new RuntimeException("Conflict in CSS naming for axes.", e);
          }
        }
      }
    }
  }
}
