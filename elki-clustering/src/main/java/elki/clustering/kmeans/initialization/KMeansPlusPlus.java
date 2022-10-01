/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.kmeans.initialization;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import elki.clustering.kmedoids.initialization.KMedoidsInitialization;
import elki.data.NumberVector;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.logging.statistics.LongStatistic;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.random.RandomFactory;

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
@Title("K-means++")
@Reference(authors = "D. Arthur, S. Vassilvitskii", //
    title = "k-means++: the advantages of careful seeding", //
    booktitle = "Proc. 18th Annual ACM-SIAM Symposium on Discrete Algorithms (SODA 2007)", //
    url = "http://dl.acm.org/citation.cfm?id=1283383.1283494", //
    bibkey = "DBLP:conf/soda/ArthurV07")
public class KMeansPlusPlus<O> extends AbstractKMeansInitialization implements KMedoidsInitialization<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(KMeansPlusPlus.class);

  /**
   * Constructor.
   *
   * @param rnd Random generator.
   */
  public KMeansPlusPlus(RandomFactory rnd) {
    super(rnd);
  }

  @Override
  public double[][] chooseInitialMeans(Relation<? extends NumberVector> relation, int k, NumberVectorDistance<?> distance) {
    if(relation.size() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + relation.size() + " < k objects.");
    }
    return new NumberVectorInstance(relation, distance, rnd).run(k);
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DBIDs ids, DistanceQuery<? super O> distQ) {
    if(ids.size() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + ids.size() + " < k objects.");
    }
    return new MedoidsInstance(ids, distQ, rnd).run(k);
  }

  /**
   * Abstract instance implementing the weight handling.
   *
   * @author Erich Schubert
   *
   * @param <T> Object type handled
   */
  protected abstract static class Instance<T> {
    /**
     * Object IDs
     */
    protected DBIDs ids;

    /**
     * Weights
     */
    protected WritableDoubleDataStore weights;

    /**
     * Count the number of distance computations.
     */
    protected long diststat;

    /**
     * Random generator
     */
    protected Random random;

    /**
     * Constructor.
     *
     * @param ids IDs to process
     * @param rnd Random generator
     */
    public Instance(DBIDs ids, RandomFactory rnd) {
      this.ids = ids;
      this.random = rnd.getSingleThreadedRandom();
      this.weights = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0.);
    }

    /**
     * Compute the distance of two objects.
     *
     * @param a First object
     * @param b Second object
     * @return Distance
     */
    protected abstract double distance(T a, DBIDRef b);

    /**
     * Initialize the weight list.
     *
     * @param first Added ID
     * @return Weight sum
     */
    protected double initialWeights(T first) {
      double weightsum = 0.;
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        // Distance will usually already be squared
        double weight = distance(first, it);
        weights.putDouble(it, weight);
        weightsum += weight;
      }
      return weightsum;
    }

    /**
     * Update the weight list.
     *
     * @param latest Added ID
     * @return Weight sum
     */
    protected double updateWeights(T latest) {
      double weightsum = 0.;
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        double weight = weights.doubleValue(it);
        if(weight <= 0.) {
          continue; // Duplicate, or already chosen.
        }
        // Distances are assumed to be squared already
        double newweight = distance(latest, it);
        if(newweight < weight) {
          weights.putDouble(it, newweight);
          weight = newweight;
        }
        weightsum += weight;
      }
      return weightsum;
    }

    protected double nextDouble(double weightsum) {
      double r = random.nextDouble() * weightsum;
      while(r <= 0 && weightsum > Double.MIN_NORMAL) {
        r = random.nextDouble() * weightsum; // Try harder to not choose 0.
      }
      return r;
    }
  }

  /**
   * Instance for k-means, number vector based.
   *
   * @author Erich Schubert
   */
  protected static class NumberVectorInstance extends Instance<NumberVector> {
    /**
     * Distance function
     */
    protected NumberVectorDistance<?> distance;

    /**
     * Data relation.
     */
    protected Relation<? extends NumberVector> relation;

    /**
     * Constructor.
     *
     * @param relation Data relation to process
     * @param distance Distance function
     * @param rnd Random generator
     */
    public NumberVectorInstance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> distance, RandomFactory rnd) {
      super(relation.getDBIDs(), rnd);
      this.distance = distance;
      this.relation = relation;
    }

    /**
     * Run k-means++ initialization for number vectors.
     *
     * @param k K
     * @return Vectors
     */
    public double[][] run(int k) {
      List<NumberVector> means = new ArrayList<>(k);
      // Choose first mean
      NumberVector firstvec = relation.get(DBIDUtil.randomSample(ids, random));
      means.add(firstvec);
      chooseRemaining(k, means, initialWeights(firstvec));
      weights.destroy();
      LOG.statistics(new LongStatistic(KMeansPlusPlus.class.getName() + ".distance-computations", diststat));
      return unboxVectors(means);
    }

    @Override
    protected double distance(NumberVector a, DBIDRef b) {
      ++diststat;
      return distance.distance(a, relation.get(b));
    }

    /**
     * Choose remaining means, weighted by distance.
     *
     * @param k Number of means to choose
     * @param means Means storage
     * @param weightsum Sum of weights
     */
    protected void chooseRemaining(int k, List<NumberVector> means, double weightsum) {
      while(true) {
        if(weightsum > Double.MAX_VALUE) {
          throw new IllegalStateException("Could not choose a reasonable mean - too many data points, too large distance sum?");
        }
        if(weightsum < Double.MIN_NORMAL) {
          LOG.warning("Could not choose a reasonable mean - to few unique data points?");
        }
        double r = nextDouble(weightsum);
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
        final NumberVector newmean = relation.get(it);
        means.add(newmean);
        if(means.size() >= k) {
          break;
        }
        // Update weights:
        weights.putDouble(it, 0.);
        weightsum = updateWeights(newmean);
      }
    }
  }

  /**
   * Instance for k-medoids.
   *
   * @author Erich Schubert
   */
  protected static class MedoidsInstance extends Instance<DBIDRef> {
    /**
     * Distance query
     */
    DistanceQuery<?> distQ;

    public MedoidsInstance(DBIDs ids, DistanceQuery<?> distQ, RandomFactory rnd) {
      super(ids, rnd);
      this.distQ = distQ;
    }

    public DBIDs run(int k) {
      ArrayModifiableDBIDs means = DBIDUtil.newArray(k);
      // Choose the first object
      DBIDRef first = DBIDUtil.randomSample(ids, random);
      means.add(first);
      chooseRemaining(k, means, initialWeights(first));
      weights.destroy();
      LOG.statistics(new LongStatistic(KMeansPlusPlus.class.getName() + ".distance-computations", diststat));
      return means;
    }

    @Override
    protected double distance(DBIDRef a, DBIDRef b) {
      ++diststat;
      return distQ.distance(a, b);
    }

    /**
     * Choose remaining means, weighted by distance.
     *
     * @param k Number of means to choose
     * @param means Means storage
     * @param weightsum Sum of weights
     */
    protected void chooseRemaining(int k, ArrayModifiableDBIDs means, double weightsum) {
      while(true) {
        if(weightsum > Double.MAX_VALUE) {
          throw new IllegalStateException("Could not choose a reasonable mean - too many data points, too large distance sum?");
        }
        if(weightsum < Double.MIN_NORMAL) {
          LOG.warning("Could not choose a reasonable mean - to few unique data points?");
        }
        double r = nextDouble(weightsum);
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
        weightsum = updateWeights(it);
      }
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<V> extends AbstractKMeansInitialization.Par {
    @Override
    public KMeansPlusPlus<V> make() {
      return new KMeansPlusPlus<>(rnd);
    }
  }
}
