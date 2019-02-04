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

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.AffineTransformation;
import de.lmu.ifi.dbs.elki.result.ScalesResult;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.RenderFlag;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.PlotItem;
import de.lmu.ifi.dbs.elki.visualization.projections.AffineProjection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.projections.Simple2D;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.LabelVisualization;

/**
 * ScatterPlotProjector is responsible for producing a set of scatterplot
 * visualizations.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - ScalesResult
 * @assoc - - - Projection2D
 *
 * @param <V> Vector type
 */
public class ScatterPlotProjector<V extends SpatialComparable> implements Projector {
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
  public ScatterPlotProjector(Relation<V> rel, int maxdim) {
    super();
    this.rel = rel;
    this.dmax = maxdim;
    assert (maxdim <= RelationUtil.dimensionality(rel)) : "Requested dimensionality larger than data dimensionality?!?";
  }

  @Override
  public Collection<PlotItem> arrange(VisualizerContext context) {
    List<PlotItem> layout = new ArrayList<>(1);
    List<VisualizationTask> tasks = context.getVisTasks(this);
    if(!tasks.isEmpty()) {
      ScalesResult scales = ScalesResult.getScalesResult(rel);
      final PlotItem master;
      if(dmax == 2) {
        // In 2d, make the plot twice as big.
        master = new PlotItem(2 + .1, 2 + .1, null);
        {
          Projection2D proj = new Simple2D(this, scales.getScales(), 0, 1);
          PlotItem it = new PlotItem(.1, 0, 2., 2., proj);
          it.tasks = tasks;
          master.subitems.add(it);
        }
        // Label at bottom
        {
          PlotItem it = new PlotItem(.1, 2., 2., .1, null);
          it.tasks.add(new VisualizationTask(new LabelVisualization(RelationUtil.getColumnLabel(rel, 0)), "", null, null) //
              .requestSize(2., .1).with(RenderFlag.NO_DETAIL));
          master.subitems.add(it);
        }
        // Label on left
        {
          PlotItem it = new PlotItem(0, 0, .1, 2, null);
          it.tasks.add(new VisualizationTask(new LabelVisualization(RelationUtil.getColumnLabel(rel, 1), true), "", null, null) //
              .requestSize(.1, 2.).with(RenderFlag.NO_DETAIL));
          master.subitems.add(it);
        }
      }
      else {
        final double sizeh = Math.ceil((dmax - 1) / 2.0);
        master = new PlotItem(sizeh * 2. + .1, dmax - 1 + .1, null);

        for(int d1 = 0; d1 < dmax - 1; d1++) {
          for(int d2 = d1 + 1; d2 < dmax; d2++) {
            Projection2D proj = new Simple2D(this, scales.getScales(), d1, d2);
            PlotItem it = new PlotItem(d1 + .1, d2 - 1, 1., 1., proj);
            it.tasks = tasks;
            master.subitems.add(it);
          }
        }
        if(dmax >= 3) {
          AffineTransformation p = AffineProjection.axisProjection(RelationUtil.dimensionality(rel), 1, 2);
          p.addRotation(0, 2, MathUtil.deg2rad(-10.));
          p.addRotation(1, 2, MathUtil.deg2rad(15.));
          // Wanna try 4d? go ahead:
          // p.addRotation(0, 3, Math.PI / 180 * -20.);
          // p.addRotation(1, 3, Math.PI / 180 * 30.);
          Projection2D proj = new AffineProjection(this, scales.getScales(), p);
          PlotItem it = new PlotItem(sizeh + .1, 0, sizeh, sizeh, proj);
          it.tasks = tasks;
          master.subitems.add(it);
        }
        // Labels at bottom
        for(int d1 = 0; d1 < dmax - 1; d1++) {
          PlotItem it = new PlotItem(d1 + .1, dmax - 1, 1., .1, null);
          it.tasks.add(new VisualizationTask(new LabelVisualization(RelationUtil.getColumnLabel(rel, d1)), "", null, null) //
              .requestSize(1, .1).with(RenderFlag.NO_DETAIL));
          master.subitems.add(it);
        }
        // Labels on left
        for(int d2 = 1; d2 < dmax; d2++) {
          PlotItem it = new PlotItem(0, d2 - 1, .1, 1, null);
          it.tasks.add(new VisualizationTask(new LabelVisualization(RelationUtil.getColumnLabel(rel, d2), true), "", null, null) //
              .requestSize(.1, 1.).with(RenderFlag.NO_DETAIL));
          master.subitems.add(it);
        }
      }

      layout.add(master);
    }
    return layout;
  }

  @Override
  public String getMenuName() {
    return "Scatterplot";
  }

  /**
   * The relation we project.
   *
   * @return Relation
   */
  public Relation<V> getRelation() {
    return rel;
  }
}
