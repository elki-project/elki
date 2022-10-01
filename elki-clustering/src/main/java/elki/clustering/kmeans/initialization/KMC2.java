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

import elki.data.NumberVector;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.logging.statistics.LongStatistic;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.random.RandomFactory;

/**
 * K-MC² initialization
 * <p>
 * Reference:
 * <p>
 * O. Bachem, M. Lucic, S. H. Hassani, A. Krause<br>
 * Approximate K-Means++ in Sublinear Time<br>
 * Proc. 30th AAAI Conference on Artificial Intelligence
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
@Title("K-MC²")
@Reference(authors = "O. Bachem, M. Lucic, S. H. Hassani, A. Krause", //
    title = "Approximate K-Means++ in Sublinear Time", //
    booktitle = "Proc. 30th AAAI Conference on Artificial Intelligence", //
    url = "http://www.aaai.org/ocs/index.php/AAAI/AAAI16/paper/view/12147", //
    bibkey = "DBLP:conf/aaai/BachemLHK16")
public class KMC2 extends AbstractKMeansInitialization {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(KMC2.class);

  /**
   * Number of sampling attempts.
   */
  protected int m;

  /**
   * Constructor.
   *
   * @param m M parameter
   * @param rnd Random generator.
   */
  public KMC2(int m, RandomFactory rnd) {
    super(rnd);
    this.m = m;
  }

  @Override
  public double[][] chooseInitialMeans(Relation<? extends NumberVector> relation, int k, NumberVectorDistance<?> distance) {
    if(relation.size() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + relation.size() + " < k objects.");
    }
    return new Instance(relation, distance, m, rnd).run(k);
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
     * Distance function
     */
    protected NumberVectorDistance<?> distance;

    /**
     * Weights
     */
    protected WritableDoubleDataStore weights;

    /**
     * Count the number of distance computations.
     */
    protected long diststat;

    /**
     * Number of sampling attempts.
     */
    protected int m;

    /**
     * Random generator
     */
    protected Random random;

    /**
     * Constructor.
     *
     * @param relation Data relation to process
     * @param distance Distance function
     * @param m M parameter
     * @param rnd Random generator
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> distance, int m, RandomFactory rnd) {
      this.relation = relation;
      this.distance = distance;
      this.m = m;
      this.random = rnd.getSingleThreadedRandom();
      this.weights = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0.);
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
        double weight = distance(first, it);
        weights.putDouble(it, weight);
        weightsum += weight;
      }
      return weightsum;
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
      getLogger().statistics(new LongStatistic(this.getClass().getName() + ".distance-computations", diststat));
      return unboxVectors(means);
    }

    /**
     * Compute the distance of two objects.
     *
     * @param a First object
     * @param b Second object
     * @return Distance
     */
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
      while(means.size() < k) {
        DBIDRef best = sample(weightsum);
        // These are not normalized, but we use ratios anyway.
        double curp = distance(best, means) / weights.doubleValue(best);
        for(int i = 1; i < m; i++) {
          DBIDRef cand = sample(weightsum);
          double candp = distance(cand, means) / weights.doubleValue(cand);
          if(!(curp > 0) || candp / curp > random.nextDouble()) {
            best = cand;
            curp = candp;
          }
        }
        means.add(relation.get(best));
      }
    }

    /**
     * Weighted sampling.
     *
     * @param weightsum Total weight sum.
     * @return Sampled object
     */
    protected DBIDRef sample(double weightsum) {
      while(true) {
        if(weightsum > Double.MAX_VALUE) {
          throw new IllegalStateException("Could not choose a reasonable mean - too many data points, too large distance sum?");
        }
        if(weightsum < Double.MIN_NORMAL) {
          LOG.warning("Could not choose a reasonable mean - to few unique data points?");
        }
        // We have not normalized the weights:
        double r = random.nextDouble() * weightsum;
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
        return it;
      }
    }

    /**
     * Minimum distance to the current means.
     *
     * @param cand Candidate
     * @param means Current means
     * @return Minimum distance
     */
    protected double distance(DBIDRef cand, List<NumberVector> means) {
      double d = weights.doubleValue(cand); // distance to first
      for(int i = 1; i < means.size(); i++) {
        double d2 = distance(means.get(i), cand);
        d = d2 < d ? d2 : d;
      }
      return d;
    }

    /**
     * Class logger.
     * 
     * @return Logger
     */
    protected Logging getLogger() {
      return LOG;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par extends AbstractKMeansInitialization.Par {
    /**
     * Parameter m of the AFK-MC² method.
     */
    public static final OptionID M_ID = new OptionID("afkmc2.m", "Number of MCMC steps to do");

    /**
     * Number of sampling attempts.
     */
    protected int m;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(M_ID, 100) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> m = x);
    }

    @Override
    public KMC2 make() {
      return new KMC2(m, rnd);
    }
  }
}
