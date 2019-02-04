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
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.result.ScalesResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.PlotItem;
import de.lmu.ifi.dbs.elki.visualization.projections.ProjectionParallel;
import de.lmu.ifi.dbs.elki.visualization.projections.SimpleParallel;

/**
 * ParallelPlotProjector is responsible for producing a parallel axes
 * visualization.
 * <p>
 * Reference:
 * <p>
 * A. Inselberg<br>
 * Parallel Coordinates. Visual Multidimensional Geometry and Its Applications
 *
 * @author Robert Rödler
 * @since 0.5.0
 *
 * @param <V> Vector type
 */
// TODO: support categorical features, and multiple relations too
@Reference(authors = "A. Inselberg", //
    title = "Parallel Coordinates. Visual Multidimensional Geometry and Its Applications", booktitle = "", //
    url = "https://doi.org/10.1007/978-0-387-68628-8", //
    bibkey = "doi:10.1007/978-0-387-68628-8")
public class ParallelPlotProjector<V extends SpatialComparable> implements Projector {
  /**
   * Relation we project.
   */
  Relation<V> rel;

  /**
   * Constructor.
   *
   * @param rel Relation
   */
  public ParallelPlotProjector(Relation<V> rel) {
    super();
    this.rel = rel;
  }

  @Override
  public Collection<PlotItem> arrange(VisualizerContext context) {
    List<PlotItem> col = new ArrayList<>(1);
    List<VisualizationTask> tasks = context.getVisTasks(this);
    if(!tasks.isEmpty()) {
      ScalesResult scales = ScalesResult.getScalesResult(rel);
      ProjectionParallel proj = new SimpleParallel(this, scales.getScales());

      final double width = Math.max(.5, Math.ceil(MathUtil.log2(scales.getScales().length - 1)));
      final PlotItem it = new PlotItem(width, 1., proj);
      it.tasks = tasks;
      col.add(it);
    }
    return col;
  }

  @Override
  public String getMenuName() {
    return "Parallelplot";
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