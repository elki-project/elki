package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.quality;

/*
 This file is part of ELKI: Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;

/**
 * Class for computing the average overall distance.
 * 
 * The average of all average pairwise distances in a cluster.
 * 
 * @author Stephan Baier
 */
public class WithinClusterMeanDistanceQualityMeasure implements KMeansQualityMeasure<NumberVector<?>, NumberDistance<?, ?>> {
  @Override
  public <V extends NumberVector<?>> double calculateCost(Clustering<? extends MeanModel<V>> clustering, PrimitiveDistanceFunction<? super V, ? extends NumberDistance<?, ?>> distanceFunction, Relation<V> relation) {
    @SuppressWarnings("unchecked")
    final List<Cluster<MeanModel<V>>> clusterList = (List<Cluster<MeanModel<V>>>) (List<?>) clustering.getAllClusters();

    if (distanceFunction instanceof PrimitiveDoubleDistanceFunction) {
      @SuppressWarnings("unchecked")
      PrimitiveDoubleDistanceFunction<? super V> df = (PrimitiveDoubleDistanceFunction<? super V>) distanceFunction;
      double clusterDistanceSum = 0;
      for (Cluster<MeanModel<V>> cluster : clusterList) {
        DBIDs ids = cluster.getIDs();

        // Compute sum of pairwise distances:
        double clusterPairwiseDistanceSum = 0;
        for (DBIDIter iter1 = ids.iter(); iter1.valid(); iter1.advance()) {
          V obj1 = relation.get(iter1);
          for (DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
            clusterPairwiseDistanceSum += df.doubleDistance(obj1, relation.get(iter2));
          }
        }
        clusterDistanceSum += clusterPairwiseDistanceSum / (ids.size() * ids.size());
      }

      return clusterDistanceSum / clusterList.size();
    } else {
      double clusterDistanceSum = 0;
      for (Cluster<MeanModel<V>> cluster : clusterList) {
        DBIDs ids = cluster.getIDs();

        // Compute sum of pairwise distances:
        double clusterPairwiseDistanceSum = 0;
        for (DBIDIter iter1 = ids.iter(); iter1.valid(); iter1.advance()) {
          V obj1 = relation.get(iter1);
          for (DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
            clusterPairwiseDistanceSum += distanceFunction.distance(obj1, relation.get(iter2)).doubleValue();
          }
        }
        clusterDistanceSum += clusterPairwiseDistanceSum / (ids.size() * ids.size());
      }

      return clusterDistanceSum / clusterList.size();
    }
  }
}
