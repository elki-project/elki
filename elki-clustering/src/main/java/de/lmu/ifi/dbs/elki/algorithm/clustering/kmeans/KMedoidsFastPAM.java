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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMedoidsInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.LABInitialMeans;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * FastPAM: An improved version of PAM, that is usually O(k) times faster. This
 * class incorporates the benefits of {@link KMedoidsFastPAM1}, but in addition
 * it tries to perform multiple swaps in each iteration (FastPAM2), which can
 * reduce the total number of iterations needed substantially for large k, if
 * some areas of the data are largely independent.
 * <p>
 * There is a tolerance parameter, which controls how many additional swaps are
 * performed. When set to 0, it will only execute an additional swap if it
 * appears to be independent (i.e., the improvements resulting from the swap
 * have not decreased when the first swap was executed). We suggest to rather
 * leave it at the default of 1, which means to perform any additional swap
 * that gives an improvement. We could not observe a tendency to find worse
 * results when doing these additional swaps, but a reduced runtime.
 * <p>
 * Because of the speed benefits, we also suggest to use a linear-time
 * initialization, such as the k-means++ initialization or the proposed
 * LAB (linear approximative BUILD, the third component of FastPAM)
 * initialization, and try multiple times if the runtime permits.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Peter J. Rousseeuw<br>
 * Faster k-Medoids Clustering: Improving the PAM, CLARA, and CLARANS
 * Algorithms<br>
 * preprint, to appear
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <V> vector datatype
 */
@Priority(Priority.IMPORTANT + 2)
@Reference(authors = "Erich Schubert, Peter J. Rousseeuw", //
    title = "Faster k-Medoids Clustering: Improving the PAM, CLARA, and CLARANS Algorithms", //
    booktitle = "preprint, to appear", //
    url = "https://arxiv.org/abs/1810.05691", //
    bibkey = "DBLP:journals/corr/abs-1810-05691")
public class KMedoidsFastPAM<V> extends KMedoidsFastPAM1<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMedoidsFastPAM.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMedoidsFastPAM.class.getName();

  /**
   * Tolerance for fast swapping behavior (may perform worse swaps).
   */
  protected double fasttol = 0.;

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public KMedoidsFastPAM(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer) {
    this(distanceFunction, k, maxiter, initializer, 1.);
  }

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   * @param fasttol Tolerance for fast swapping
   */
  public KMedoidsFastPAM(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer, double fasttol) {
    super(distanceFunction, k, maxiter, initializer);
    this.fasttol = fasttol;
  }

  @Override
  protected void run(DistanceQuery<V> distQ, DBIDs ids, ArrayModifiableDBIDs medoids, WritableIntegerDataStore assignment) {
    new Instance(distQ, ids, assignment, fasttol).run(medoids, maxiter);
  }

  /**
   * Instance for a single dataset.
   *
   * @author Erich Schubert
   */
  protected static class Instance extends KMedoidsFastPAM1.Instance {
    /**
     * Tolerance for fast swapping behavior (may perform worse swaps).
     */
    protected double fastswap = 0.;

    /**
     * Constructor.
     *
     * @param distQ Distance query
     * @param ids IDs to process
     * @param assignment Cluster assignment
     * @param fasttol Tolerance for fast swapping
     */
    public Instance(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment, double fasttol) {
      super(distQ, ids, assignment);
      this.fastswap = 1 - fasttol;
    }

    /**
     * Run the PAM optimization phase.
     *
     * @param medoids Medoids list
     * @param maxiter
     * @return final cost
     */
    protected double run(ArrayModifiableDBIDs medoids, int maxiter) {
      final int k = medoids.size();
      // Initial assignment to nearest medoids
      // TODO: reuse distance information, from the build phase, when possible?
      double tc = assignToNearestCluster(medoids);
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + 0 + ".cost", tc));
      }

      IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("PAM iteration", LOG) : null;
      int fastswaps = 0; // For statistics
      // Swap phase
      DBIDArrayIter m = medoids.iter();
      int iteration = 1;
      ArrayModifiableDBIDs bestids = DBIDUtil.newArray(k);
      DBIDVar bestid = DBIDUtil.newVar();
      double[] best = new double[k], cost = new double[k];
      for(; maxiter <= 0 || iteration <= maxiter; iteration++) {
        LOG.incrementProcessed(prog);
        findBestSwaps(m, bestids, best, cost);
        // Convergence check
        int min = argmin(best);
        if(!(best[min] < -1e-12 * tc)) {
          break; // Converged
        }
        // Update values for new medoid.
        while(min >= 0 && best[min] < -1e-12 * tc) {
          updateAssignment(medoids, m, bestids.assignVar(min, bestid), min);
          tc += best[min];
          best[min] = Double.POSITIVE_INFINITY; // Deactivate
          // Find next candidate:
          while((min = argmin(best)) >= 0 && best[min] < -1e-12 * tc) {
            bestids.assignVar(min, bestid);
            // Compare object to its own medoid.
            if(DBIDUtil.equal(m.seek(assignment.intValue(bestid) & 0x7FFF), bestid)) {
              best[min] = Double.POSITIVE_INFINITY; // Deactivate
              continue; // This is a medoid.
            }
            final double hdist = nearest.doubleValue(bestid); // Current cost
            // hdist is the cost we get back by making the non-medoid h medoid.
            double c = computeReassignmentCost(bestid, min) - hdist;
            if(c <= best[min] * fastswap) {
              best[min] = c;
              ++fastswaps;
              break;
            }
            best[min] = Double.POSITIVE_INFINITY; // Deactivate
          }
        }
        if(LOG.isStatistics()) {
          LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + iteration + ".cost", tc));
        }
      }
      // TODO: we may have accumulated some error on tc.
      LOG.setCompleted(prog);
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
        LOG.statistics(new LongStatistic(KEY + ".fast-swaps", fastswaps));
      }
      // Cleanup
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        assignment.putInt(it, assignment.intValue(it) & 0x7FFF);
      }
      return tc;
    }

    /**
     * Find the best swaps.
     *
     * @param m Medoids
     * @param bestids Storage for best non-medois
     * @param best Storage for best cost
     * @param cost Scratch space for cost
     */
    protected void findBestSwaps(DBIDArrayIter m, ArrayModifiableDBIDs bestids, double[] best, double[] cost) {
      Arrays.fill(best, Double.POSITIVE_INFINITY);
      // Iterate over all non-medoids:
      for(DBIDIter h = ids.iter(); h.valid(); h.advance()) {
        // Compare object to its own medoid.
        if(DBIDUtil.equal(m.seek(assignment.intValue(h) & 0x7FFF), h)) {
          continue; // This is a medoid.
        }
        // The cost we get back by making the non-medoid h medoid.
        Arrays.fill(cost, -nearest.doubleValue(h));
        computeReassignmentCost(h, cost);

        // Find the best possible swap for each medoid:
        for(int i = 0; i < cost.length; i++) {
          final double costi = cost[i];
          if(costi < best[i]) {
            best[i] = costi;
            bestids.set(i, h);
          }
        }
      }
    }

    /**
     * Find the smallest (most negative) value.
     * 
     * @param best Best changes
     * @return Index of smallest value
     */
    protected static int argmin(double[] best) {
      double min = Double.POSITIVE_INFINITY;
      int ret = -1;
      for(int i = 0; i < best.length; i++) {
        final double v = best[i];
        if(v < min) {
          min = v;
          ret = i;
        }
      }
      return ret;
    }

    /**
     * Compute the reassignment cost of one swap.
     *
     * @param h Current object to swap with the medoid
     * @param mnum Medoid number to be replaced
     * @return cost
     */
    protected double computeReassignmentCost(DBIDRef h, int mnum) {
      double cost = 0.;
      // Compute costs of reassigning other objects j:
      for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
        if(DBIDUtil.equal(h, j)) {
          continue;
        }
        // distance(j, i) to nearest medoid
        final double distcur = nearest.doubleValue(j);
        // distance(j, h) to new medoid
        final double dist_h = distQ.distance(h, j);
        // Check if current medoid of j is removed:
        if((assignment.intValue(j) & 0x7FFF) == mnum) {
          // distance(j, o) to second nearest / possible reassignment
          // Case 1b: j switches to new medoid, or to the second nearest:
          cost += Math.min(dist_h, second.doubleValue(j)) - distcur;
        }
        else if(dist_h < distcur) {
          // Case 1c: j is closer to h than its current medoid
          cost += dist_h - distcur;
        } // else Case 1a): j is closer to i than h and m, so no change.
      }
      return cost;
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V> extends KMedoidsFastPAM1.Parameterizer<V> {
    /**
     * Tolerance for performing additional swaps.
     */
    public static final OptionID FASTTOL_ID = new OptionID("pam.fasttol", "Tolerance for optimistically performing additional swaps, where 1 executes all fast swaps, 0 only those that are unaffected by the primary swaps.");

    /**
     * Tolerance for fast swapping behavior (may perform worse swaps).
     */
    protected double fasttol = 0.;

    @SuppressWarnings("rawtypes")
    @Override
    protected Class<? extends KMedoidsInitialization> defaultInitializer() {
      return LABInitialMeans.class;
    }

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter fasttolP = new DoubleParameter(FASTTOL_ID, 1.) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
      if(config.grab(fasttolP)) {
        fasttol = fasttolP.doubleValue();
      }
    }

    @Override
    protected KMedoidsFastPAM<V> makeInstance() {
      return new KMedoidsFastPAM<>(distanceFunction, k, maxiter, initializer, fasttol);
    }
  }
}
