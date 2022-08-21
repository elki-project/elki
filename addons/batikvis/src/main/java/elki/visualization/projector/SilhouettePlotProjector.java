/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.visualization.projector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DoubleDBIDList;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.database.relation.DoubleRelation;
import elki.visualization.VisualizationTask;
import elki.visualization.VisualizerContext;
import elki.visualization.gui.overview.PlotItem;
import elki.visualization.projections.SilhouetteProjection;
import elki.visualization.silhouette.SilhouettePlot;

/**
 * Projection for Silhouette plots.
 *
 * @author Robert Gehde
 * @since 0.8.0
 */
public class SilhouettePlotProjector implements Projector {
  /**
   * Underlying clustering
   */
  private Clustering<?> clustering;

  /**
   * Silhouette plot image
   */
  private SilhouettePlot plot = null;

  /**
   * Silhouette values
   */
  private DoubleDBIDList[] values = null;

  /**
   * Constructor.
   *
   * @param clustering clustering on which the silhouette values are calculated
   * @param dds silhouette values sorted by clusters
   */
  public SilhouettePlotProjector(Clustering<?> clustering, DoubleRelation dds) {
    super();
    this.clustering = clustering;
    this.values = sortSilhouette(clustering, dds);
  }

  /**
   * Sort the silhouettes for visualization.
   * 
   * @param c Clustering
   * @param dds Silhouette values
   * @return Sorted silhouette per cluster
   */
  private static DoubleDBIDList[] sortSilhouette(Clustering<?> c, DoubleRelation dds) {
    DoubleDBIDList[] silhouettes = new DoubleDBIDList[c.getAllClusters().size()];
    int i = 0;
    for(Cluster<?> cluster : c.getAllClusters()) {
      ModifiableDoubleDBIDList dbidlist = DBIDUtil.newDistanceDBIDList(cluster.size());
      for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
        dbidlist.add(dds.doubleValue(iter), iter);
      }
      dbidlist.sortDescending();
      silhouettes[i++] = dbidlist;
    }
    return silhouettes;
  }

  @Override
  public String getMenuName() {
    return "Silhouette Plot Projection";
  }

  @Override
  public Collection<PlotItem> arrange(VisualizerContext context) {
    List<PlotItem> col = new ArrayList<>(1);
    List<VisualizationTask> tasks = context.getVisTasks(this);
    if(!tasks.isEmpty()) {
      final PlotItem it = new PlotItem(4., 1., new SilhouetteProjection(this));
      it.tasks = tasks;
      col.add(it);
    }
    return col;
  }

  /**
   * Get or produce the actual Silhouette plot.
   *
   * @param context Context to use
   * @return Plot
   */
  public SilhouettePlot getSilhouettePlot(VisualizerContext context) {
    if(plot == null) {
      plot = SilhouettePlot.plotForSilhouetteValues(values, context);
    }
    return plot;
  }

  /**
   * Get the underlying Clustering
   *
   * @return the clustering
   */
  public Clustering<?> getResult() {
    return clustering;
  }

  /**
   * Get the silhouette values
   *
   * @return the silhouette values
   */
  public DoubleDBIDList[] getValues() {
    return values;
  }
}
