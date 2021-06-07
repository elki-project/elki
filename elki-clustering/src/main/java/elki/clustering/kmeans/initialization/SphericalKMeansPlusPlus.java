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
package elki.clustering.kmeans.initialization;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import elki.data.NumberVector;
import elki.data.VectorUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.relation.Relation;
import elki.distance.*;
import elki.logging.Logging;
import elki.logging.statistics.LongStatistic;
import elki.utilities.random.RandomFactory;

/**
 * Spherical K-Means++ initialization for k-means.
 * <p>
 * FIXME: currently assumes the vectors to be L2 normalized beforehand, but does
 * not ensure that this is true.
 *
 * @author Erich Schubert
 *
 * @param <O> Vector type
 */
public class SphericalKMeansPlusPlus<O> extends AbstractKMeansInitialization {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(SphericalKMeansPlusPlus.class);

  /**
   * Constructor.
   *
   * @param rnd Random generator.
   */
  public SphericalKMeansPlusPlus(RandomFactory rnd) {
    super(rnd);
  }

  @Override
  public double[][] chooseInitialMeans(Relation<? extends NumberVector> relation, int k, NumberVectorDistance<?> distance) {
    if(relation.size() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + relation.size() + " < k objects.");
    }
    if(distance instanceof CosineDistance || distance instanceof CosineUnitlengthDistance //
        || distance instanceof ArcCosineDistance || distance instanceof ArcCosineUnitlengthDistance) {
      return new Instance(relation, rnd).run(k);
    }
    LOG.warning("Spherical k-means++ was used with an instance of " + distance.getClass() + ". Falling back to regular k-means++.");
    return new KMeansPlusPlus.NumberVectorInstance(relation, distance, rnd).run(k);
  }

  /**
   * Abstract instance implementing the weight handling.
   *
   * @author Erich Schubert
   */
  protected static class Instance {
    /**
     * Data relation.
     */
    protected Relation<? extends NumberVector> relation;

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
     * @param relation Data relation
     * @param rnd Random generator
     */
    public Instance(Relation<? extends NumberVector> relation, RandomFactory rnd) {
      this.relation = relation;
      this.random = rnd.getSingleThreadedRandom();
      this.weights = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0.);
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
      NumberVector firstvec = relation.get(DBIDUtil.randomSample(relation.getDBIDs(), random));
      means.add(firstvec);
      chooseRemaining(k, means, initialWeights(firstvec));
      weights.destroy();
      LOG.statistics(new LongStatistic(SphericalKMeansPlusPlus.class.getName() + ".distance-computations", diststat));
      return unboxVectors(means);
    }

    /**
     * Compute the distance of two objects.
     *
     * @param a First object
     * @param b Second object
     * @return Distance
     */
    protected double similarity(NumberVector a, DBIDRef b) {
      ++diststat;
      return VectorUtil.dot(a, relation.get(b));
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
        DBIDIter it = relation.iterDBIDs();
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

    /**
     * Initialize the weight list.
     *
     * @param first Added ID
     * @return Weight sum
     */
    protected double initialWeights(NumberVector first) {
      double weightsum = 0.;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        // Distance will usually already be squared
        double weight = 1 - similarity(first, it);
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
    protected double updateWeights(NumberVector latest) {
      double weightsum = 0.;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        double weight = weights.doubleValue(it);
        if(weight <= 0.) {
          continue; // Duplicate, or already chosen.
        }
        // Distances are assumed to be squared already
        double newweight = 1 - similarity(latest, it);
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
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<V> extends AbstractKMeansInitialization.Par {
    @Override
    public SphericalKMeansPlusPlus<V> make() {
      return new SphericalKMeansPlusPlus<>(rnd);
    }
  }
}
