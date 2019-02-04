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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * K-Means++ initialization for k-means.
 * <p>
 * Reference:
 * <p>
 * D. Arthur, S. Vassilvitskii<br>
 * k-means++: the advantages of careful seeding<br>
 * Proc. 18th Annual ACM-SIAM Symposium on Discrete Algorithms (SODA 2007)
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <O> Vector type
 */
@Reference(authors = "D. Arthur, S. Vassilvitskii", //
    title = "k-means++: the advantages of careful seeding", //
    booktitle = "Proc. 18th Annual ACM-SIAM Symposium on Discrete Algorithms (SODA 2007)", //
    url = "http://dl.acm.org/citation.cfm?id=1283383.1283494", //
    bibkey = "DBLP:conf/soda/ArthurV07")
@Alias("de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansPlusPlusInitialMeans")
public class KMeansPlusPlusInitialMeans<O> extends AbstractKMeansInitialization implements KMedoidsInitialization<O> {
  /**
   * Constructor.
   *
   * @param rnd Random generator.
   */
  public KMeansPlusPlusInitialMeans(RandomFactory rnd) {
    super(rnd);
  }

  @Override
  public double[][] chooseInitialMeans(Database database, Relation<? extends NumberVector> relation, int k, NumberVectorDistanceFunction<?> distanceFunction) {
    if(relation.size() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + relation.size() + " < k objects.");
    }
    DBIDs ids = relation.getDBIDs();
    @SuppressWarnings("unchecked")
    DistanceQuery<NumberVector> distQ = database.getDistanceQuery((Relation<NumberVector>) relation, (NumberVectorDistanceFunction<NumberVector>) distanceFunction);

    // Chose first mean
    Random random = rnd.getSingleThreadedRandom();
    DBIDRef first = DBIDUtil.randomSample(ids, random);
    NumberVector firstvec = relation.get(first);
    List<NumberVector> means = new ArrayList<>(k);
    means.add(firstvec);

    // Initialize weights
    WritableDoubleDataStore weights = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0.);
    double weightsum = initialWeights(weights, ids, firstvec, distQ);
    chooseRemaining(relation, ids, distQ, k, means, weights, weightsum, random);
    weights.destroy();
    return unboxVectors(means);
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DBIDs ids, DistanceQuery<? super O> distQ) {
    Random random = rnd.getSingleThreadedRandom();
    DBIDRef first = DBIDUtil.randomSample(ids, random);
    ArrayModifiableDBIDs means = DBIDUtil.newArray(k);
    means.add(first);

    // Initialize weights
    WritableDoubleDataStore weights = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0.);
    double weightsum = initialWeights(weights, ids, first, distQ);
    chooseRemaining(ids, distQ, k, means, weights, weightsum, random);
    weights.destroy();
    return means;
  }

  /**
   * Initialize the weight list.
   *
   * @param weights Weight list
   * @param ids IDs
   * @param first Added ID
   * @param distQ Distance query
   * @return Weight sum
   */
  static double initialWeights(WritableDoubleDataStore weights, DBIDs ids, NumberVector first, DistanceQuery<? super NumberVector> distQ) {
    double weightsum = 0.;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      // Distance will usually already be squared
      double weight = distQ.distance(first, it);
      weights.putDouble(it, weight);
      weightsum += weight;
    }
    return weightsum;
  }

  /**
   * Initialize the weight list.
   *
   * @param weights Weight list
   * @param ids IDs
   * @param latest Added ID
   * @param distQ Distance query
   * @return Weight sum
   */
  static double initialWeights(WritableDoubleDataStore weights, DBIDs ids, DBIDRef latest, DistanceQuery<?> distQ) {
    double weightsum = 0.;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      // Distance will usually already be squared
      double weight = distQ.distance(latest, it);
      weights.putDouble(it, weight);
      weightsum += weight;
    }
    return weightsum;
  }

  /**
   * Choose remaining means, weighted by distance.
   *
   * @param relation Data relation
   * @param ids IDs
   * @param distQ Distance function
   * @param k Number of means to choose
   * @param means Means storage
   * @param weights Weights (initialized!)
   * @param weightsum Sum of weights
   * @param random Random generator
   */
  static void chooseRemaining(Relation<? extends NumberVector> relation, DBIDs ids, DistanceQuery<NumberVector> distQ, int k, List<NumberVector> means, WritableDoubleDataStore weights, double weightsum, Random random) {
    while(true) {
      if(weightsum > Double.MAX_VALUE) {
        throw new IllegalStateException("Could not choose a reasonable mean - too many data points, too large distance sum?");
      }
      if(weightsum < Double.MIN_NORMAL) {
        LoggingUtil.warning("Could not choose a reasonable mean - to few data points?");
      }
      double r = random.nextDouble() * weightsum;
      while(r <= 0 && weightsum > Double.MIN_NORMAL) {
        r = random.nextDouble() * weightsum; // Try harder to not choose 0.
      }
      DBIDIter it = ids.iter();
      while(it.valid()) {
        if((r -= weights.doubleValue(it)) < 0) {
          break;
        }
        it.advance();
      }
      if(!it.valid()) { // Rare case, but happens due to floating math
        weightsum -= r; // Decrease
        continue; // Retry
      }
      // Add new mean:
      final NumberVector newmean = relation.get(it);
      means.add(newmean);
      if(means.size() >= k) {
        break;
      }
      // Update weights:
      weights.putDouble(it, 0.);
      weightsum = updateWeights(weights, ids, newmean, distQ);
    }
  }

  /**
   * Choose remaining means, weighted by distance.
   *
   * @param ids IDs
   * @param distQ Distance function
   * @param k Number of means to choose
   * @param means Means storage
   * @param weights Weights (initialized!)
   * @param weightsum Sum of weights
   * @param random Random generator
   */
  static void chooseRemaining(DBIDs ids, DistanceQuery<?> distQ, int k, ArrayModifiableDBIDs means, WritableDoubleDataStore weights, double weightsum, Random random) {
    while(true) {
      if(weightsum > Double.MAX_VALUE) {
        throw new IllegalStateException("Could not choose a reasonable mean - too many data points, too large distance sum?");
      }
      if(weightsum < Double.MIN_NORMAL) {
        LoggingUtil.warning("Could not choose a reasonable mean - to few data points?");
      }
      double r = random.nextDouble() * weightsum;
      while(r <= 0 && weightsum > Double.MIN_NORMAL) {
        r = random.nextDouble() * weightsum; // Try harder to not choose 0.
      }
      DBIDIter it = ids.iter();
      while(it.valid()) {
        if((r -= weights.doubleValue(it)) <= 0) {
          break;
        }
        it.advance();
      }
      if(!it.valid()) { // Rare case, but happens due to floating math
        weightsum -= r; // Decrease
        continue; // Retry
      }
      // Add new mean:
      means.add(it);
      if(means.size() >= k) {
        break;
      }
      // Update weights:
      weights.putDouble(it, 0.);
      weightsum = updateWeights(weights, ids, it, distQ);
    }
  }

  /**
   * Update the weight list.
   *
   * @param weights Weight list
   * @param ids IDs
   * @param latest Added ID
   * @param distQ Distance query
   * @return Weight sum
   */
  private static double updateWeights(WritableDoubleDataStore weights, DBIDs ids, DBIDRef latest, DistanceQuery<?> distQ) {
    double weightsum = 0.;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      double weight = weights.doubleValue(it);
      if(weight <= 0.) {
        continue; // Duplicate, or already chosen.
      }
      double newweight = distQ.distance(latest, it);
      if(newweight < weight) {
        weights.putDouble(it, newweight);
        weight = newweight;
      }
      weightsum += weight;
    }
    return weightsum;
  }

  /**
   * Update the weight list.
   *
   * @param weights Weight list
   * @param ids IDs
   * @param latest Added ID
   * @param distQ Distance query
   * @return Weight sum
   */
  private static double updateWeights(WritableDoubleDataStore weights, DBIDs ids, NumberVector latest, DistanceQuery<? super NumberVector> distQ) {
    double weightsum = 0.;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      double weight = weights.doubleValue(it);
      if(weight <= 0.) {
        continue; // Duplicate, or already chosen.
      }
      double newweight = distQ.distance(latest, it);
      if(newweight < weight) {
        weights.putDouble(it, newweight);
        weight = newweight;
      }
      weightsum += weight;
    }
    return weightsum;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V> extends AbstractKMeansInitialization.Parameterizer {
    @Override
    protected KMeansPlusPlusInitialMeans<V> makeInstance() {
      return new KMeansPlusPlusInitialMeans<>(rnd);
    }
  }
}
