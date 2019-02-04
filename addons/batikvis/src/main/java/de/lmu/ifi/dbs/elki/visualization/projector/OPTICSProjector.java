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

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.ClusterOrder;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.PlotItem;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.OPTICSProjection;

/**
 * Projection for OPTICS plots.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public class OPTICSProjector implements Projector {
  /**
   * Cluster order result
   */
  private ClusterOrder clusterOrder;

  /**
   * OPTICS plot image
   */
  private OPTICSPlot plot = null;

  /**
   * Constructor.
   *
   * @param co Cluster order
   */
  public OPTICSProjector(ClusterOrder co) {
    super();
    this.clusterOrder = co;
  }

  @Override
  public String getMenuName() {
    return "OPTICS Plot Projection";
  }

  @Override
  public Collection<PlotItem> arrange(VisualizerContext context) {
    List<PlotItem> col = new ArrayList<>(1);
    List<VisualizationTask> tasks = context.getVisTasks(this);
    if(!tasks.isEmpty()) {
      final PlotItem it = new PlotItem(4., 1., new OPTICSProjection(this));
      it.tasks = tasks;
      col.add(it);
    }
    return col;
  }

  /**
   * Get the cluster order
   *
   * @return the cluster order
   */
  public ClusterOrder getResult() {
    return clusterOrder;
  }

  /**
   * Get or produce the actual OPTICS plot.
   *
   * @param context Context to use
   * @return Plot
   */
  public OPTICSPlot getOPTICSPlot(VisualizerContext context) {
    if(plot == null) {
      plot = OPTICSPlot.plotForClusterOrder(clusterOrder, context);
    }
    return plot;
  }
}