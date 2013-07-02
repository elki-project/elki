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
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;

/**
 * Class for computing the variance in a clustering result (sum-of-squares).
 * 
 * @author Stephan Baier
 */
public class WithinClusterVarianceQualityMeasure implements KMeansQualityMeasure<NumberVector<?>, NumberDistance<?, ?>> {
  @Override
  public <V extends NumberVector<?>> double calculateCost(Clustering<? extends MeanModel<V>> clustering, PrimitiveDistanceFunction<? super V, ? extends NumberDistance<?, ?>> distanceFunction, Relation<V> relation) {
    @SuppressWarnings("unchecked")
    final List<Cluster<MeanModel<V>>> clusterList = (List<Cluster<MeanModel<V>>>) (List<?>) clustering.getAllClusters();

    boolean squared = (distanceFunction instanceof SquaredEuclideanDistanceFunction);
    if (distanceFunction instanceof PrimitiveDoubleDistanceFunction) {
      @SuppressWarnings("unchecked")
      PrimitiveDoubleDistanceFunction<? super V> df = (PrimitiveDoubleDistanceFunction<? super V>) distanceFunction;
      double variance = 0.0;
      for (Cluster<MeanModel<V>> cluster : clusterList) {
        DBIDs ids = cluster.getIDs();
        V mean = cluster.getModel().getMean();

        for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
          double dist = df.doubleDistance(relation.get(iter), mean);
          if (squared) {
            variance += dist;
          } else {
            variance += dist * dist;
          }
        }
      }
      return variance;
    } else {
      double variance = 0.0;
      for (Cluster<MeanModel<V>> cluster : clusterList) {
        DBIDs ids = cluster.getIDs();
        V mean = cluster.getModel().getMean();

        for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
          double dist = distanceFunction.distance(relation.get(iter), mean).doubleValue();
          variance += dist * dist;
        }
      }
      return variance;
    }
  }
}
