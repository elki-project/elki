package de.lmu.ifi.dbs.elki.visualization.visualizers.parallel;

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

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.projector.ParallelPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Generates a SVG-Element containing axes, including labeling.
 * 
 * @author Robert Rödler
 * 
 * @apiviz.uses SVGSimpleLinearAxis
 */
public class ParallelAxisVisualization extends AbstractParallelVisualization<NumberVector<?, ?>> {
  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public ParallelAxisVisualization(VisualizationTask task) {
    super(task);
    incrementalRedraw();
    context.addResultListener(this);
  }

  @Override
  public void destroy() {
    context.removeResultListener(this);
    super.destroy();
  }

  @Override
  protected void redraw() {
    final int dim = proj.getVisibleDimensions();
    try {
      for(int i = 0; i < dim; i++) {
        boolean inv = proj.isAxisInverted(i);
        if(!inv) {
          SVGSimpleLinearAxis.drawAxis(svgp, layer, proj.getAxisScale(i), getVisibleAxisX(i), getSizeY(), getVisibleAxisX(i), 0, SVGSimpleLinearAxis.LabelStyle.ENDLABEL, context.getStyleLibrary());
        }
        else {
          SVGSimpleLinearAxis.drawAxis(svgp, layer, proj.getAxisScale(i), getVisibleAxisX(i), 0, getVisibleAxisX(i), getSizeY(), SVGSimpleLinearAxis.LabelStyle.ENDLABEL, context.getStyleLibrary());
        }
      }
    }
    catch(CSSNamingConflict e) {
      throw new RuntimeException("Conflict in CSS naming for axes.", e);
    }
  }

  /**
   * Factory for axis visualizations
   * 
   * @author Robert Rödler
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses ParallelAxisVisualization oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * A short name characterizing this Visualizer.
     */
    private static final String NAME = "Parallel Axes";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ParallelAxisVisualization(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Iterator<ParallelPlotProjector<?>> ps = ResultUtil.filteredResults(result, ParallelPlotProjector.class);
      while(ps.hasNext()) {
        ParallelPlotProjector<?> p = ps.next();
        final VisualizationTask task = new VisualizationTask(NAME, p, p.getRelation(), this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_BACKGROUND);
        baseResult.getHierarchy().add(p, task);
      }
    }

    @Override
    public boolean allowThumbnails(VisualizationTask task) {
      // Don't use thumbnails
      return true;
    }
  }
}