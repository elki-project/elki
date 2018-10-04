/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
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
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * A slightly faster version of PAM (but in the same overall complexity). The
 * change here is fairly trivial - we just aggregate the costs of all possible
 * medoids in an array, rather than doing one at a time. This reduces the number
 * of distance computations and lookups in the nearest / second nearest lists by
 * a factor of k, at the cost of k doubles. Furthermore, this version tries to
 * perform the best update for each cluster, and thus may need fewer iterations.
 *
 * @author Erich Schubert
 *
 * @apiviz.has MedoidModel
 * @apiviz.composedOf KMedoidsInitialization
 *
 * @param <V> vector datatype
 */
@Priority(Priority.IMPORTANT + 2)
public class KMedoidsPAMFaster<V> extends KMedoidsPAMFast<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMedoidsPAMFaster.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMedoidsPAMFaster.class.getName();

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
  public KMedoidsPAMFaster(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer) {
    this(distanceFunction, k, maxiter, initializer, 1.);
  }

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   * @param fastswap Tolerance for fast swapping
   */
  public KMedoidsPAMFaster(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer, double fasttol) {
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
  protected static class Instance extends KMedoidsPAMFast.Instance {
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

      final boolean metric = distQ.getDistanceFunction().isMetric();

      IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("PAM iteration", LOG) : null;
      // Swap phase
      DBIDArrayIter m = medoids.iter();
      int iteration = 1;
      int fastswaps = 0; // Statistics
      ArrayModifiableDBIDs bestids = DBIDUtil.newArray(k);
      double[] best = new double[k], cost = new double[k];
      for(; maxiter <= 0 || iteration <= maxiter; iteration++) {
        LOG.incrementProcessed(prog);
        Arrays.fill(best, Double.POSITIVE_INFINITY);
        // Try to swap a non-medoid with a medoid member:
        // Iterate over all non-medoids:
        for(DBIDIter h = ids.iter(); h.valid(); h.advance()) {
          // Compare object to its own medoid.
          if(DBIDUtil.equal(m.seek(assignment.intValue(h) & 0x7FFF), h)) {
            continue; // This is a medoid.
          }
          final double hdist = nearest.doubleValue(h); // Current cost of h.
          if(metric && hdist <= 0.) {
            continue; // Duplicate of a medoid.
          }
          // hdist is the cost we get back by making the non-medoid h medoid.
          Arrays.fill(cost, -hdist);
          computeReassignmentCost(h, cost);

          // Find the best possible swap for h:
          for(int i = 0; i < k; i++) {
            final double costi = cost[i];
            if(costi < best[i]) {
              best[i] = costi;
              bestids.set(i, h);
            }
          }
        }
        // Convergence check
        int min = argminNegative(best);
        if(min < 0) {
          break; // Converged
        }
        // Update values for new medoid.
        DBIDVar bestid = DBIDUtil.newVar();
        while(min >= 0) {
          assert best[min] < 0;
          {
            medoids.set(min, bestids.assignVar(min, bestid));
            final double hdist = nearest.putDouble(bestid, 0);
            final int olda = assignment.intValue(bestid);
            // In the high short, we store the second nearest center!
            if((olda & 0x7FFF) != min) {
              assignment.putInt(bestid, min | ((olda & 0x7FFF) << 16));
              second.putDouble(bestid, hdist);
            }
            else {
              assignment.putInt(bestid, min | (olda & 0x7FFF0000));
            }
            // Reassign
            updateAssignment(m, bestid, min);
            tc += best[min];
          }
          best[min] = Double.POSITIVE_INFINITY; // Deactivate
          // Find next candidate:
          while((min = argminNegative(best)) >= 0) {
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
            else {
              best[min] = Double.POSITIVE_INFINITY;
            }
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
     * Check if there is any negative cost (= improvement).
     * 
     * @param best Best changes
     * @return Index of smallest value, -1 if no negative values.
     */
    protected int argminNegative(double[] best) {
      double min = 0.;
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
          final double distsec = second.doubleValue(j);
          // Case 1b: j switches to new medoid, or to the second nearest:
          cost += Math.min(dist_h, distsec) - distcur;
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
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<V> extends KMedoidsPAMFast.Parameterizer<V> {
    /**
     * Tolerance for performing additional swaps.
     */
    public static final OptionID FASTTOL_ID = new OptionID("pam.fasttol", "Tolerance for optimistically performing additional swaps, where 1 executes all fast swaps, 0 only those that are unaffected by the primary swaps.");

    /**
     * Tolerance for fast swapping behavior (may perform worse swaps).
     */
    protected double fasttol = 0.;

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
    protected KMedoidsPAMFaster<V> makeInstance() {
      return new KMedoidsPAMFaster<>(distanceFunction, k, maxiter, initializer, fasttol);
    }
  }
}
