package de.lmu.ifi.dbs.elki.visualization.projector;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.PlotItem;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;

/**
 * Projection for OPTICS plots.
 * 
 * @author Erich Schubert
 */
public class OPTICSProjector<D extends Distance<D>> extends AbstractHierarchicalResult implements Projector {
  /**
   * Cluster order result
   */
  private ClusterOrderResult<D> clusterOrder;

  /**
   * OPTICS plot image
   */
  private OPTICSPlot<D> plot = null;

  /**
   * Constructor.
   * 
   * @param co Cluster order
   */
  public OPTICSProjector(ClusterOrderResult<D> co) {
    super();
    this.clusterOrder = co;
  }

  @Override
  public String getLongName() {
    return "OPTICS projection";
  }

  @Override
  public String getShortName() {
    return "optics";
  }

  @Override
  public Collection<PlotItem> arrange() {
    List<PlotItem> col = new ArrayList<PlotItem>(1);
    List<VisualizationTask> tasks = ResultUtil.filterResults(this, VisualizationTask.class);
    if (tasks.size() > 0) {
      final PlotItem it = new PlotItem(4., 1., null);
      it.visualizations = tasks;
      col.add(it);
    }
    return col;
  }

  /**
   * Get the cluster order
   * 
   * @return the cluster order
   */
  public ClusterOrderResult<D> getResult() {
    return clusterOrder;
  }

  /**
   * Get or produce the actual OPTICS plot.
   * 
   * @param context Context to use
   * @return Plot
   */
  public OPTICSPlot<D> getOPTICSPlot(VisualizerContext context) {
    if(plot == null) {
      plot = OPTICSPlot.plotForClusterOrder(clusterOrder, context);
    }
    return plot;
  }
}