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
package elki.clustering.kmeans.quality;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;

/**
 * Class for computing the average overall distance.
 * <p>
 * The average of all average pairwise distances in a cluster.
 *
 * @author Stephan Baier
 * @since 0.6.0
 */
public class WithinClusterMeanDistance implements KMeansQualityMeasure<NumberVector> {
  @Override
  public <V extends NumberVector> double quality(Clustering<? extends MeanModel> clustering, NumberVectorDistance<? super V> distanceFunction, Relation<V> relation) {
    double clusterDistanceSum = 0;
    for(Cluster<? extends MeanModel> cluster : clustering.getAllClusters()) {
      DBIDs ids = cluster.getIDs();

      // Compute sum of pairwise distances:
      double clusterPairwiseDistanceSum = 0;
      for(DBIDIter iter1 = ids.iter(); iter1.valid(); iter1.advance()) {
        NumberVector obj1 = relation.get(iter1);
        for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
          clusterPairwiseDistanceSum += distanceFunction.distance(obj1, relation.get(iter2));
        }
      }
      clusterDistanceSum += clusterPairwiseDistanceSum / (ids.size() * ids.size());
    }

    return clusterDistanceSum / clustering.getAllClusters().size();
  }

  @Override
  public boolean isBetter(double currentCost, double bestCost) {
    // Careful: bestCost may be NaN!
    return !(currentCost >= bestCost);
  }
}
