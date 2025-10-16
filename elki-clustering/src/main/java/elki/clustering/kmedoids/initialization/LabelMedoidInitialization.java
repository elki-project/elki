/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2025
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
package elki.clustering.kmedoids.initialization;

import java.util.HashMap;
import java.util.Map;

import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.random.RandomFactory;

/**
 * Initialization method for Labeled k-Medoids that uses labeled data to initialize medoids.
 * For each labeled cluster, the medoid is selected as the point with minimum sum of distances to all other points in the same cluster.
 * If k is larger than the number of labels, additional medoids are randomly selected from the data set.
 *
 * @author Andreas Lang
 *
 * @param <O> Object type
 */
public class LabelMedoidInitialization<O> extends SemiSupervisedKMedoidsInitialization<O> {

  /**
   * Constructor.
   *
   * @param rnd Random generator
   */
  public LabelMedoidInitialization(RandomFactory rnd) {
    super(rnd);
  }

  /**
   * Choose initial medoids based on labeled data.
   * For each labeled cluster, the medoid is selected as the point with minimum sum of distances to all other points in the same cluster.
   * If k is larger than the number of labels, additional medoids are randomly selected from the data set.
   *
   * @param k Number of medoids to choose
   * @param noLabels Number of labels
   * @param ids DBIDs of all objects
   * @param labels Labels for each object
   * @param distance Distance function
   * @param medoids Output array for selected medoids
   * @return Cluster labels for the chosen medoids
   */
  @Override
  public int[] chooseInitialMedoids(int k, int noLabels, DBIDs ids, WritableIntegerDataStore labels,  DistanceQuery<? super O> distance, ArrayModifiableDBIDs medoids) {
    int[] clusterLabels = new int[k];
    // make a map label -> objects
    Map<Integer, ArrayModifiableDBIDs> labelToDataMap = new HashMap<>();
    clusterIdObjectsMap(ids, labelToDataMap, labels);
    // we do not know which "classes" to forget .. abort
    if(k < labelToDataMap.size() - 1) {
      throw new RuntimeException("Number of expected clusters in labelled data is larger than k. Consider increasing k.");
    }
    // if k = the number of labels, then for each partition find medoid
    int i = 0;
    for(Map.Entry<Integer, ArrayModifiableDBIDs> cluster : labelToDataMap.entrySet()) {
      if(cluster.getKey() == 0) {
        continue;
      }
      DBIDRef medoid = findMedoidFromData(cluster.getValue(), distance);
      medoids.add(medoid);
      clusterLabels[i++] = cluster.getKey();
    }
    // future check - when all is fixed if choosing uncolored means is
    // better
    while(medoids.size() < k) {
      DBIDVar medoid = DBIDUtil.randomSample(ids, rnd);
      if(!medoids.contains(medoid)) {
        medoids.add(medoid);
        clusterLabels[i++] = labels.intValue(medoid);
      }
    }
    return clusterLabels;
  }

  /**
  * Find the medoid for a given cluster of objects.
  * The medoid is the point with minimum sum of distances to all other points in the cluster.
  *
  * @param cluster Cluster assignments of objects
  * @param distQ Distance query function
  * @return Medoid object
  */
  protected DBIDRef findMedoidFromData(ArrayDBIDs cluster, DistanceQuery<? super O> distQ) {
    DBIDArrayIter otherIt = cluster.iter();
    double minSum = Double.MAX_VALUE;
    DBIDVar medoid = DBIDUtil.newVar();

    for(DBIDIter iter = cluster.iter(); iter.valid(); iter.advance()) {
      double sum = 0;
      for(otherIt.seek(0); otherIt.valid(); otherIt.advance()) {
        double d = distQ.distance(iter, otherIt);
        sum += d;
      }
      if(sum < minSum) {
        minSum = sum;
        medoid.set(iter);
      }
    }
    return medoid;
  }

  /**
  * Create a mapping from labels to objects.
  *
  * @param ids IDs of all objects
  * @param labelToDataMap Map from label to objects with that label (result)
  * @param labels Labels for each object
  */
  protected void clusterIdObjectsMap(DBIDs ids, Map<Integer, ArrayModifiableDBIDs> labelToDataMap, WritableIntegerDataStore labels) {
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      int myLabel = labels.intValue(iter);
      if(labelToDataMap.containsKey(myLabel)) {
        labelToDataMap.get(myLabel).add(iter);
      }
      else {
        ArrayModifiableDBIDs curLabelCl = DBIDUtil.newArray();
        curLabelCl.add(iter);
        labelToDataMap.put(myLabel, curLabelCl);
      }
    }
  }


    /**
   * Parameterization class.
   *
   * @author Andreas Lang
   */
  public static class Par<O> extends SemiSupervisedKMedoidsInitialization.Par {


    @Override
    public void configure(Parameterization config) {
      super.configure(config);
    }

    @Override
    public LabelMedoidInitialization<?> make() {
      return new LabelMedoidInitialization<>(rnd);
    }


  }
}
