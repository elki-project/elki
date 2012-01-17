package experimentalcode.erich.visualization;

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
import java.util.ArrayList;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.P2DVisualization;

public class DensityEstimation2DVisualization extends P2DVisualization {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Density Estimation";

  /**
   * Constructor.
   * 
   * @param task Task
   */
  public DensityEstimation2DVisualization(VisualizationTask task) {
    super(task);
  }

  @Override
  protected void redraw() {
    // TODO Auto-generated method stub

  }

  /**
   * The visualization factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses DensityEstimation2DVisualization oneway - - «create»
   * 
   * @param <NV> Type of the NumberVector being visualized.
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new DensityEstimation2DVisualization(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      ArrayList<Clustering<?>> cs = ResultUtil.filterResults(result, Clustering.class);
      boolean hasClustering = (cs.size() > 0);

      Iterator<ScatterPlotProjector<?>> ps = ResultUtil.filteredResults(result, ScatterPlotProjector.class);
      for(ScatterPlotProjector<?> p : IterableUtil.fromIterator(ps)) {
        final VisualizationTask task = new VisualizationTask(NAME, p.getRelation(), p.getRelation(), this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 1);
        if(hasClustering) {
          task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
        }
        // baseResult.getHierarchy().add(p.getRelation(), task);
        baseResult.getHierarchy().add(p, task);
      }
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
      }

      @Override
      protected Factory makeInstance() {
        return new Factory();
      }
    }
  }
}
