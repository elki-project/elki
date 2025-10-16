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

import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.random.RandomFactory;

/**
 * Unsupervised initialization method for labeled k-Medoids.
   * This class provides an unsupervised approach to initialize medoids by using
 * another initialization method and then assigning labels based on the original data labels.
 *
 * @author Andreas Lang
 *
 * @param <O> Object type
 */
public class UnsupervisedInitialization<O> extends SemiSupervisedKMedoidsInitialization<O> {

  /**
   * Initialization method to use for the medoid selection.
   */
  KMedoidsInitialization<O> initializer;

  /**
   * Constructor.
   *
   * @param rnd Random generator
   * @param initializer Initialization method to use for initial medoid selection
   */
  public UnsupervisedInitialization(RandomFactory rnd, KMedoidsInitialization<O> initializer) {
    super(rnd);
    this.initializer = initializer;
  }

  /**
   * Choose initial medoids using an unsupervised approach.
   * First selects initial medoids using the provided initialization method,
   * then assigns labels to each medoid based on the original data labels.
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
    medoids.addDBIDs(initializer.chooseInitialMedoids(k, ids, distance));
    int[] clusterLabels = new int[k];
    DBIDArrayIter miter = medoids.iter();
    int[] labelCount = new int[noLabels + 1];

    for (int i = 0; miter.valid(); miter.advance(),i++) {
      final int label = labels.intValue(miter);
      clusterLabels[i] = label;
      labelCount[label] += 1;
    }

    int l = 0;
    int p = 0;
    for (int i = 1; i < noLabels + 1; i++) {
      if (labelCount[i] == 0) {
        // if(labelCount[0]>0){
        //   labelCount[0]-=1;
        //   labelCount[i]+=1;
        //   for(;p<k;p++){
        //     if (clusterLabels[p] == 0) {
        //       clusterLabels[p] = i;
        //       continue labelloop;
        //     }
        //   }
        // }
        while(labelCount[l]==0 || (labelCount[l] < 2 && l > 0) ){
          l++;
          p=0;
        }
        assert l < noLabels + 1;
        labelCount[l] -=1;
        labelCount[i] +=1;
        for(;p<k;p++){
          if (clusterLabels[p] == l) {
            clusterLabels[p] = i;
            medoids.set(p, findPointOfColor(ids, i, medoids, labels));
            break;
          }
        }
      }
    }
    return clusterLabels;
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
    public UnsupervisedInitialization<?> make() {
      return new UnsupervisedInitialization<>(rnd, initializer);
    }


  }
}
