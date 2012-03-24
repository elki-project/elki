package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Generates a SVG-Element containing axes, including labeling.
 * 
 * @author Remigius Wojdanowski
 * 
 * @apiviz.uses SVGSimpleLinearAxis
 */
public class AxisVisualization extends AbstractScatterplotVisualization {
  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public AxisVisualization(VisualizationTask task) {
    super(task);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    int dim = DatabaseUtil.dimensionality(rel);

    // origin
    double[] orig = proj.fastProjectScaledToRenderSpace(new double[dim]);
    // diagonal point opposite to origin
    double[] diag = new double[dim];
    for(int d2 = 0; d2 < dim; d2++) {
      diag[d2] = 1;
    }
    diag = proj.fastProjectScaledToRenderSpace(diag);
    // compute angle to diagonal line, used for axis labeling.
    double diaga = Math.atan2(diag[1] - orig[1], diag[0] - orig[0]);

    double alfontsize = 1.2 * context.getStyleLibrary().getTextSize(StyleLibrary.AXIS_LABEL);
    CSSClass alcls = new CSSClass(svgp, "unmanaged");
    alcls.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, SVGUtil.fmt(alfontsize));
    alcls.setStatement(SVGConstants.CSS_FILL_PROPERTY, context.getStyleLibrary().getTextColor(StyleLibrary.AXIS_LABEL));
    alcls.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, context.getStyleLibrary().getFontFamily(StyleLibrary.AXIS_LABEL));

    // draw axes
    for(int d = 0; d < dim; d++) {
      double[] v = new double[dim];
      v[d] = 1;
      // projected endpoint of axis
      double[] ax = proj.fastProjectScaledToRenderSpace(v);
      boolean righthand = false;
      double axa = Math.atan2(ax[1] - orig[1], ax[0] - orig[0]);
      if(axa > diaga || (diaga > 0 && axa > diaga + Math.PI)) {
        righthand = true;
      }
      // System.err.println(ax.get(0) + " "+ ax.get(1)+
      // " "+(axa*180/Math.PI)+" "+(diaga*180/Math.PI));
      if(ax[0] != orig[0] || ax[1] != orig[1]) {
        try {
          SVGSimpleLinearAxis.drawAxis(svgp, layer, proj.getScale(d), orig[0], orig[1], ax[0], ax[1], righthand ? SVGSimpleLinearAxis.LabelStyle.RIGHTHAND : SVGSimpleLinearAxis.LabelStyle.LEFTHAND, context.getStyleLibrary());
          // TODO: move axis labeling into drawAxis function.
          double offx = (righthand ? 1 : -1) * 0.02 * Projection.SCALE;
          double offy = (righthand ? 1 : -1) * 0.02 * Projection.SCALE;
          Element label = svgp.svgText(ax[0] + offx, ax[1] + offy, DatabaseUtil.getColumnLabel(rel, d + 1));
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

  /**
   * Factory for axis visualizations
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses AxisVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * A short name characterizing this Visualizer.
     */
    private static final String NAME = "Axes";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new AxisVisualization(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      IterableIterator<ScatterPlotProjector<?>> ps = ResultUtil.filteredResults(result, ScatterPlotProjector.class);
      for(ScatterPlotProjector<?> p : ps) {
        final VisualizationTask task = new VisualizationTask(NAME, p, p.getRelation(), this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_BACKGROUND);
        baseResult.getHierarchy().add(p, task);
      }
    }

    @Override
    public boolean allowThumbnails(VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }
  }
}