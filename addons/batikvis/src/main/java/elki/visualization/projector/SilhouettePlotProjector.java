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
package elki.visualization.projector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import elki.data.Clustering;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.visualization.VisualizationTask;
import elki.visualization.VisualizerContext;
import elki.visualization.gui.overview.PlotItem;
import elki.visualization.projections.SilhouetteProjection;
import elki.visualization.silhouette.SilhouettePlot;

/**
 * Projection for Silhouette plots.
 *
 * @author Robert Gehde
 */
public class SilhouettePlotProjector implements Projector {
  /**
   * underlying clustering
   */
  private Clustering<?> clustering;

  /**
   * Silhouette plot image
   */
  private SilhouettePlot plot = null;

  /**
   * silhouette values
   */
  private ModifiableDoubleDBIDList[] values = null;

  /**
   * Constructor.
   *
   * @param clustering underlying clustering on which the silhouette values are
   *        calculated
   * @param silhouettes silhouette values sorted by clusters
   */
  public SilhouettePlotProjector(Clustering<?> clustering, ModifiableDoubleDBIDList[] silhouettes) {
    super();
    this.clustering = clustering;
    this.values = silhouettes;
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
  public ModifiableDoubleDBIDList[] getValues() {
    return values;
  }
}
