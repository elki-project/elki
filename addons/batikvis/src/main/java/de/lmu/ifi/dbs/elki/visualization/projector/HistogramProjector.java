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
package de.lmu.ifi.dbs.elki.visualization.projector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.result.ScalesResult;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.RenderFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.PlotItem;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection1D;
import de.lmu.ifi.dbs.elki.visualization.projections.Simple1D;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.LabelVisualization;

/**
 * ScatterPlotProjector is responsible for producing a set of scatterplot
 * visualizations.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - ScalesResult
 * @assoc - - - Projection1D
 *
 * @param <V> Vector type
 */
public class HistogramProjector<V extends NumberVector> implements Projector {
  /**
   * Relation we project.
   */
  Relation<V> rel;

  /**
   * Database dimensionality.
   */
  int dmax;

  /**
   * Constructor.
   *
   * @param rel Relation
   * @param maxdim Maximum dimension to use
   */
  public HistogramProjector(Relation<V> rel, int maxdim) {
    super();
    this.rel = rel;
    this.dmax = maxdim;
    assert(maxdim <= RelationUtil.dimensionality(rel)) : "Requested dimensionality larger than data dimensionality?!?";
  }

  @Override
  public Collection<PlotItem> arrange(VisualizerContext context) {
    List<PlotItem> layout = new ArrayList<>(1 + dmax);
    List<VisualizationTask> tasks = context.getVisTasks(this);
    if(!tasks.isEmpty()) {
      final double xoff = (dmax > 1) ? .1 : 0.;
      final double hheight = .5;
      final double lheight = .1;
      PlotItem master = new PlotItem(dmax + xoff, hheight + lheight, null);
      ScalesResult scales = ScalesResult.getScalesResult(rel);
      for(int d1 = 0; d1 < dmax; d1++) {
        Projection1D proj = new Simple1D(this, scales.getScales(), d1);
        final PlotItem it = new PlotItem(d1 + xoff, lheight, 1., hheight, proj);
        it.tasks = tasks;
        master.subitems.add(it);
      }
      layout.add(master);
      // Add labels
      for(int d1 = 0; d1 < dmax; d1++) {
        PlotItem it = new PlotItem(d1 + xoff, 0, 1., lheight, null);
        LabelVisualization lbl = new LabelVisualization(RelationUtil.getColumnLabel(rel, d1));
        it.tasks.add(new VisualizationTask(lbl, "", null, null) //
            .requestSize(1, lheight).with(RenderFlag.NO_DETAIL));
        master.subitems.add(it);
      }
    }
    return layout;
  }

  @Override
  public String getMenuName() {
    return "Axis plot";
  }

  /**
   * Get the relation we project.
   *
   * @return Relation
   */
  public Relation<V> getRelation() {
    return rel;
  }
}