/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2024
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
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Abstract base class for initializing Semi Supervised K-Medoids.
 * This initialization method is designed to work with labeled data, where some objects
   * have known cluster assignments. The initialization will only return members of the
  * original data set as medoids.
  *
 * @author Andreas Lang
 *
 * @param <O> Object type
 */
public abstract class SemiSupervisedKMedoidsInitialization<O> {
  /**
   * Random number generator
   */
  protected RandomFactory rnd;

  /**
   * Constructor.
   *
   * @param rnd Random number generator
   */
  public SemiSupervisedKMedoidsInitialization(RandomFactory rnd) {
    this.rnd = rnd;
  }

  /**
   * Choose initial medoids for semi-supervised k-medoids clustering.
   * This method selects medoids based on the provided labeled data and distance function.
   *
   * @param k Number of medoids to choose
   * @param l Number of labels
   * @param ids IDs of all objects
   * @param labels Labels for each object
   * @param distance Distance function
   * @param medoids Output array for selected medoids
   * @return Cluster labels for the chosen medoids
   */
  abstract public int[] chooseInitialMedoids(int k, int l, DBIDs ids, WritableIntegerDataStore labels,  DistanceQuery<? super O> distance, ArrayModifiableDBIDs medoids);

  /**
   * Find a point with the specified label that is not already in the medoids set.
   *
   * @param ids IDs of all objects
   * @param label Target label to search for
   * @param medoids Current medoids set
   * @param labels Labels for each object
   * @return A point with the specified label that is not in medoids
   */
  protected DBIDRef findPointOfColor(DBIDs ids, int label, ArrayModifiableDBIDs medoids, WritableIntegerDataStore labels){
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()){
      if((labels.intValue(iter) == label) && !medoids.contains(iter)){
        return iter;
      }
    }
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      if((labels.intValue(iter) == label || labels.intValue(iter) == 0) && !medoids.contains(iter)) {
        return iter;
      }
    }
    throw new RuntimeException("No point with label " + label + " found");
  }

  /**
   * Parameterization class for semi-supervised k-medoids initialization.
   *
   * @author Andreas Lang
   */
  public abstract static class Par implements Parameterizer {
    /**
     * Random generator
     */
    protected RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      new RandomParameter(KMeans.SEED_ID).grab(config, x -> rnd = x);
    }
  }
}
