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

import java.util.ArrayList;
import java.util.stream.IntStream;

import elki.clustering.ClusteringAlgorithmUtil;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.clustering.kmedoids.FasterPAM;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayMIter;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.random.RandomFactory;

/**
 * Initialization method for K-Medoids that uses a pre-clustering approach to initialize medoids.
 * It first initializes k medoids using another initialization method, then performs PAM clustering
 * on the initial medoids to create clusters. Then it assigns labels to each cluster based on
 * the most frequent label in that cluster.
 *
 * @author Andreas Lang
 *
 * @param <O> Object type
 */
public class PreclusteredInitialization<O> extends SemiSupervisedKMedoidsInitialization<O> {

  /**
   * Initialization method to use for the initial medoid selection.
   */
  KMedoidsInitialization<O> initializer;

  /**
   * Constructor.
   *
   * @param rnd Random generator
   * @param initializer Initialization method to use for initial medoid selection
   */
  public PreclusteredInitialization(RandomFactory rnd, KMedoidsInitialization<O> initializer) {
    super(rnd);
    this.initializer = initializer;
  }

  /**
   * Choose initial medoids using a pre-clustering approach.
   * First selects initial medoids using the provided initialization method,
   * then performs PAM clustering to create clusters, and finally assigns labels
   * based on the most frequent label in each cluster.
   *
   * @param k Number of medoids to choose
   * @param noLabels Number of labels
   * @param ids IDs of all objects
   * @param labels Labels for each object
   * @param distance Distance function
   * @param medoids Output array for selected medoids
   * @return Cluster labels for the chosen medoids
   */
  @Override
  public int[] chooseInitialMedoids(int k, int noLabels, DBIDs ids, WritableIntegerDataStore labels,  DistanceQuery<? super O> distance, ArrayModifiableDBIDs medoids) {
    
        ArrayModifiableDBIDs initialMedoids = DBIDUtil.newArray(initializer.chooseInitialMedoids(k, ids, distance));
        WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
        FasterPAM.Instance inst = new FasterPAM.Instance(distance, ids, assignment);
        inst.run(initialMedoids, 2);
        ArrayModifiableDBIDs[] clusters = ClusteringAlgorithmUtil.partitionsFromIntegerLabels(ids, assignment, initialMedoids.size());

        medoids.addDBIDs(initialMedoids);
        DBIDArrayMIter miter = medoids.iter();
        int[] clusterLabels = new int[k];
        int[] labelCount = new int[noLabels + 1];
        int[][] preColors = new int[k][];

        for(int i = 0; i < k; miter.advance(), i++) {
          preColors[i] = clusterColors(clusters[i], labels, noLabels);
          final int label = labels.intValue(miter);
          // // color based on medoid
          // if (label !=0){
          //   clusterLabels[i] = label;
          //   labelCount[label] += 1;
          //   continue;
          // }
          // color based on cluster most frequent color
          if (preColors[i].length > 1  && preColors[i][0] == 0) {
            clusterLabels[i] = preColors[i][1];
            labelCount[preColors[i][1]] += 1;
            if (label != 0 && label != preColors[i][1]){
              DBIDs candidates = filterForColor(clusters[i], labels, preColors[i][1]);
              DBIDRef closest = findClosestPoint(candidates, miter, distance);
              miter.seek(i).setDBID(closest);
            }
          } else {
            // most frequent color is no color, pick the second most frequent
            clusterLabels[i] = preColors[i][0];
            labelCount[preColors[i][0]] += 1;
            if (label != 0 && label != preColors[i][0]){
              DBIDs candidates = filterForColor(clusters[i], labels, preColors[i][0]);
              DBIDRef closest = findClosestPoint(candidates, miter, distance);
              miter.seek(i).setDBID(closest);
            }
          }
        }
        // not every label is neccessarily the most frequent
        // find clusters for the remaining labels
        boolean allLabels = true;
        for(int i = 1; i < noLabels + 1; i++) {
          if(labelCount[i] == 0) {
            allLabels = false;
            break;
          }
        }
        if (allLabels) {
          return clusterLabels;
        }

        // find labels with multiple Clusters
        ArrayList<Integer> multiColor = new ArrayList<>();
        for(int j = 1; j < noLabels + 1; j++) {
          if(labelCount[j] > 1) {
            multiColor.add(j);
          }
        }

        for(int i = 1; i < noLabels + 1; i++) {
          if (labelCount[i] > 0){
            continue;
          }
          // clusters that contain the label
          ArrayList<Integer> clusterWithColor = new ArrayList<>();
          for(int j = 0; j < k; j++) {
            for (int l = 0; l<preColors[j].length; l++){
              if(preColors[j][l] == i) {
                clusterWithColor.add(j);
                break;
              }
            }
          }
          int best_redundant = -1;
          int bestRank_redundant = noLabels+1;
          int best = -1;
          int bestRank = noLabels + 1;
          // search clusters with redundant labels and points in the missing color
          for (Integer j : clusterWithColor){
            for (int l = 0; l < preColors[j].length; l++){
              if(multiColor.contains(clusterLabels[j])) {
                if (preColors[j][l] == i && l < bestRank_redundant){
                  best_redundant = j;
                  bestRank_redundant = l;
                  break;
                  }
              }
              if (preColors[j][l] == i && l < bestRank){
                best = j;
                bestRank = l;
                break;
              }
            }
          }
          if (best_redundant==-1) {
            // replace random with point as best not redundant
            DBIDs coloredPoints = filterForColor(clusters[best], labels, i);
            ArrayModifiableDBIDs possibleMedoids = DBIDUtil.newArray();
            for (miter.seek(0); miter.valid(); miter.advance()){
              if (clusterLabels[miter.getOffset()] == 0 || multiColor.contains(clusterLabels[miter.getOffset()])){
                possibleMedoids.add(miter);
              }
            }
            DBIDRef closestMedoid = findClosestPoint(possibleMedoids, miter.seek(best), distance);
            DBIDRef clostestPoint = findClosestPointNonMedoid(coloredPoints, medoids, closestMedoid, distance);

            int closestOffset = -1;
            for (miter.seek(0); miter.valid(); miter.advance()){
              if (DBIDUtil.equal(miter, closestMedoid)){
                closestOffset = miter.getOffset();
                miter.setDBID(clostestPoint);
                break;
              }
            }

            labelCount[clusterLabels[closestOffset]] -= 1;
            labelCount[i] += 1;
            if (labelCount[clusterLabels[closestOffset]] <= 1 && multiColor.contains(clusterLabels[closestOffset]) && clusterLabels[closestOffset] != 0){
              multiColor.remove(Integer.valueOf(clusterLabels[closestOffset]));
            }
            clusterLabels[closestOffset] = i;
            miter.seek(closestOffset).setDBID(clostestPoint);
            continue;
          }
          labelCount[clusterLabels[best_redundant]] -= 1;
          labelCount[i] += 1;
          if (labelCount[clusterLabels[best_redundant]] <= 1 && multiColor.contains(clusterLabels[best_redundant]) && clusterLabels[best_redundant] != 0){
            multiColor.remove(Integer.valueOf(clusterLabels[best_redundant]));
          }
          clusterLabels[best_redundant] = i;
          DBIDs candidates = filterForColor(clusters[best_redundant], labels, i);
          DBIDRef closest = findClosestPointNonMedoid(candidates, medoids, miter.seek(best_redundant), distance);
          miter.setDBID(closest);
        }
        return clusterLabels;
  }

      /**
       * Get the most frequent colors in a cluster.
       *
       * @param ids IDs of objects in the cluster
       * @param labels Labels for each object
       * @param noLabels Number of labels
       * @return Array of colors sorted by frequency
       */
      private int[] clusterColors(DBIDs ids, WritableIntegerDataStore labels, int noLabels){
        int[] colors = new int[noLabels + 1];
        for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()){
          colors[labels.intValue(iter)] += 1;
        }
        int[] sortedIndices = IntStream.range(0, colors.length)
                  .boxed().sorted((i, j) -> Integer.compare(colors[j], colors[i])) // sort desc
                  .mapToInt(ele -> ele).filter( id -> colors[id] > 0).toArray();
        return sortedIndices;
    }

    /**
     * Filter objects by color (label).
     *
     * @param ids IDs of objects to filter
     * @param labels Labels for each object
     * @param color Color to filter by
     * @return Filtered DBIDs
     */
    protected DBIDs filterForColor(DBIDs ids, WritableIntegerDataStore labels, int color) {
      ArrayModifiableDBIDs filtered = DBIDUtil.newArray();
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        if(labels.intValue(iter) == color || labels.intValue(iter) == 0) {
          filtered.add(iter);
        }
      }
      return filtered;
    }

    /**
     * Find the closest point to a reference point.
     *
     * @param ids IDs of candidate points
     * @param ref Reference point
     * @param distQ Distance query function
     * @return Closest point to reference
     */
    protected DBIDRef findClosestPoint(DBIDs ids, DBIDRef ref, DistanceQuery<? super O> distQ){
      DBIDVar closest = DBIDUtil.newVar();
      double minDist = Double.MAX_VALUE;
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()){
        double dist = distQ.distance(ref, iter);
        if(dist < minDist && !DBIDUtil.equal(ref, iter)){
          minDist = dist;
          closest.set(iter);
        }
      }
      return closest;
    }

    /**
     * Find the closest point to a reference point that is not a medoid.
     *
     * @param ids IDs of candidate points
     * @param meds IDs of current medoids
     * @param ref Reference point
     * @param distQ Distance query function
     * @return Closest non-medoid point to reference
     */
    protected DBIDRef findClosestPointNonMedoid(DBIDs ids, DBIDs meds, DBIDRef ref, DistanceQuery<? super O> distQ) {
      DBIDVar closest = DBIDUtil.newVar();
      double minDist = Double.MAX_VALUE;
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        double dist = distQ.distance(ref, iter);
        if(dist < minDist && !DBIDUtil.equal(ref, iter) && !meds.contains(iter)) {
          minDist = dist;
          closest.set(iter);
        }
      }
      return closest;
    }


    /**
   * Parameterization class.
   *
   * @author Andreas Lang
   */
  public static class Par<O> extends SemiSupervisedKMedoidsInitialization.Par {

    /**
     * Method to choose means without looking at labels.
     */
    protected KMedoidsInitialization<O> initializer;


    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<KMedoidsInitialization<O>>(KMeans.INIT_ID, KMedoidsInitialization.class, RandomlyChosen.class) //
          .grab(config, x -> initializer = x);
    }

    @Override
    public PreclusteredInitialization<?> make() {
      return new PreclusteredInitialization<>(rnd, initializer);
    }

  }
}
