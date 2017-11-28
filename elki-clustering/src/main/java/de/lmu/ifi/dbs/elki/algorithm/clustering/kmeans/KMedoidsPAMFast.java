/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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

/**
 * A slightly faster version of PAM (but in the same overall complexity). The
 * change here is fairly trivial - we just aggregate the costs of all possible
 * medoids in an array, rather than doing one at a time. This reduces the number
 * of distance computations and lookups in the nearest / second nearest lists by
 * a factor of k, at the cost of k doubles.
 *
 * @author Erich Schubert
 *
 * @apiviz.has MedoidModel
 * @apiviz.composedOf KMedoidsInitialization
 *
 * @param <V> vector datatype
 */
@Priority(Priority.IMPORTANT + 1)
public class KMedoidsPAMFast<V> extends KMedoidsPAM<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMedoidsPAMFast.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMedoidsPAMFast.class.getName();

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public KMedoidsPAMFast(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer) {
    super(distanceFunction, k, maxiter, initializer);
  }

  @Override
  protected void run(DistanceQuery<V> distQ, DBIDs ids, ArrayModifiableDBIDs medoids, WritableIntegerDataStore assignment) {
    new Instance(distQ, ids, assignment).run(medoids, maxiter);
  }

  /**
   * Instance for a single dataset.
   *
   * @author Erich Schubert
   */
  protected static class Instance extends KMedoidsPAM.Instance {
    /**
     * Constructor.
     *
     * @param distQ Distance query
     * @param ids IDs to process
     * @param assignment Cluster assignment
     */
    public Instance(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment) {
      super(distQ, ids, assignment);
    }

    /**
     * Run the PAM optimization phase.
     *
     * @param medoids Medoids list
     * @param maxiter
     * @return this
     */
    protected Instance run(ArrayModifiableDBIDs medoids, int maxiter) {
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
      DBIDVar bestid = DBIDUtil.newVar();
      DBIDArrayIter m = medoids.iter();
      int iteration = 1;
      double[] cost = new double[k];
      for(; maxiter <= 0 || iteration <= maxiter; iteration++) {
        LOG.incrementProcessed(prog);
        // Try to swap a non-medoid with a medoid member:
        double best = Double.POSITIVE_INFINITY;
        int bestcluster = -1;
        // Iterate over all non-medoids:
        for(DBIDIter h = ids.iter(); h.valid(); h.advance()) {
          // Compare object to its own medoid.
          if(DBIDUtil.equal(m.seek(assignment.intValue(h)), h)) {
            continue; // This is a medoid.
          }
          final double hdist = nearest.doubleValue(h); // Current cost of h.
          if(metric && hdist <= 0.) {
            continue; // Duplicate of a medoid.
          }
          computeReassignmentCost(h, cost);

          // Find the best possible swap for h:
          for(int pi = 0; pi < k; pi++) {
            final double cpi = cost[pi];
            if(cpi < best) {
              best = cpi;
              bestid.set(h);
              bestcluster = pi;
            }
          }
          // hdist is the cost we get back by making the non-medoid h medoid.
          best -= hdist;
        }
        if(best >= 0.) {
          break;
        }
        medoids.set(bestcluster, bestid);
        // Reassign
        double nc = assignToNearestCluster(medoids);
        if(LOG.isStatistics()) {
          LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + iteration + ".cost", nc));
        }
        if(nc > tc) {
          if(nc - tc < 1e-7 * tc) {
            LOG.warning("PAM failed to converge (numerical instability?)");
            break;
          }
          LOG.warning("PAM failed to converge: costs increased by: " + (nc - tc) + " exepected a decrease by " + best);
          break;
        }
        tc = nc;
      }
      LOG.setCompleted(prog);
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
      }
      return this;
    }

    /**
     * Compute the reassignment cost, for all medoids in one pass.
     *
     * @param h Current object to swap with any medoid.
     * @param cost Cost aggregation array, must have size k
     */
    protected void computeReassignmentCost(DBIDRef h, double[] cost) {
      // Compute costs of reassigning other objects j:
      for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
        if(DBIDUtil.equal(h, j)) {
          continue;
        }
        // distance(j, i) for pi == pj
        final double distcur = nearest.doubleValue(j);
        // distance(j, o) to second nearest / possible reassignment
        final double distsec = second.doubleValue(j);
        // distance(j, h) to new medoid
        final double dist_h = distQ.distance(h, j);
        // Case 1b: j switches to new medoid, or to the second nearest:
        final int pj = assignment.intValue(j);
        cost[pj] += Math.min(dist_h, distsec) - distcur;
        final double delta = dist_h - distcur;
        if(delta < 0) {
          // Case 1c: j is closer to h than its current medoid
          for(int pi = 0; pi < pj; pi++) {
            cost[pi] += delta;
          }
          for(int pi = pj + 1; pi < cost.length; pi++) {
            cost[pi] += delta;
          }
        } // else Case 1a): j is closer to i than h and m, so no change.
      }
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
  public static class Parameterizer<V> extends KMedoidsPAM.Parameterizer<V> {
    @Override
    protected KMedoidsPAMFast<V> makeInstance() {
      return new KMedoidsPAMFast<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
