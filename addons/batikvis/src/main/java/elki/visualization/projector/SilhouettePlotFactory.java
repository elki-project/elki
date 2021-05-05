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

import elki.data.Clustering;
import elki.database.datastore.DoubleDataStore;
import elki.visualization.VisualizationTree;
import elki.visualization.VisualizerContext;

/**
 * Produce Silhouette plot projections
 *
 * @author Robert Gehde
 *
 * @has - - - SilhouettePlotProjector
 */
public class SilhouettePlotFactory implements ProjectorFactory {
  /**
   * Constructor.
   */
  public SilhouettePlotFactory() {
    super();
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Clustering<?> c = VisualizationTree.findNewResults(context, start).filter(Clustering.class).get();
    // compute the plot only if you find the clustering
    if(c == null) {
      return;
    }
    VisualizationTree.findNewResults(context, start).filter(DoubleDataStore.class).forEach(dds -> {
      // FIXME: ensure that the DoubleDataStore *is* Silhouette scores!
      context.addVis(c, new SilhouettePlotProjector(c, dds));
    });
  }
}
