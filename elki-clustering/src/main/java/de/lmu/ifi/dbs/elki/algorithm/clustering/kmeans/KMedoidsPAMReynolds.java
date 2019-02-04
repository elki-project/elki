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

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMedoidsInitialization;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * The Partitioning Around Medoids (PAM) algorithm with some additional
 * optimizations proposed by Reynolds et al.
 * <p>
 * In our implementation, we could not observe a substantial improvement over
 * the original PAM algorithm. This may be because of modern CPU architectures,
 * where saving an addition may be neglibile compared to caching and pipelining.
 * <p>
 * Reference:
 * <p>
 * A. P. Reynolds, G. Richards, B. de la Iglesia, V. J. Rayward-Smith<br>
 * Clustering Rules: A Comparison of Partitioning and Hierarchical Clustering
 * Algorithms<br>
 * J. Math. Model. Algorithms 5(4)
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <V> vector datatype
 */
@Reference(authors = "A. P. Reynolds, G. Richards, B. de la Iglesia, V. J. Rayward-Smith", //
    title = "Clustering Rules: A Comparison of Partitioning and Hierarchical Clustering Algorithms", //
    booktitle = "J. Math. Model. Algorithms 5(4)", //
    url = "https://doi.org/10.1007/s10852-005-9022-1", //
    bibkey = "DBLP:journals/jmma/ReynoldsRIR06")
public class KMedoidsPAMReynolds<V> extends KMedoidsPAM<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMedoidsPAMReynolds.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMedoidsPAMReynolds.class.getName();

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public KMedoidsPAMReynolds(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer) {
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

      WritableDoubleDataStore tnearest = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);

      IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("PAM iteration", LOG) : null;
      // Swap phase
      DBIDVar bestid = DBIDUtil.newVar();
      int iteration = 1;
      for(; maxiter <= 0 || iteration <= maxiter; iteration++) {
        LOG.incrementProcessed(prog);
        // Try to swap a non-medoid with a medoid member:
        double best = Double.POSITIVE_INFINITY;
        int bestcluster = -1;
        // Iterate over each medoid:
        for(int pi = 0; pi < k; pi++) {
          // Compute medoid removal costs only once, c.f., Reynolds et al.
          double basecost = computeRemovalCost(pi, tnearest);
          // Iterate over all non-medoids:
          for(DBIDIter h = ids.iter(); h.valid(); h.advance()) {
            // h is a non-medoid currently in cluster of medoid m.
            // hdist is the cost we get back by making the non-medoid h medoid.
            double cpi = basecost + computeReassignmentCost(h, tnearest);
            if(cpi < best) {
              best = cpi;
              bestid.set(h);
              bestcluster = pi;
            }
          }
        }
        if(!(best < -1e-12 * tc)) {
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
      return tc;
    }

    /**
     * Compute the cost of removing a medoid just once. This can then be reused
     * for every point, thus decreasing the runtime cost at low memory overhead.
     *
     * The output array contains for each medoid the cost of removing all its
     * points, and reassigning them to the second nearest medoid instead.
     *
     * @return Cost
     */
    protected double computeRemovalCost(int i, WritableDoubleDataStore tnearest) {
      double cost = 0;
      // Compute costs of reassigning to the second closest medoid.
      for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
        final double n = nearest.doubleValue(j);
        if(assignment.intValue(j) == i) {
          final double s = second.doubleValue(j);
          cost += s - n;
          tnearest.put(j, s);
        }
        else {
          tnearest.put(j, n);
        }
      }
      return cost;
    }

    /**
     * Compute the reassignment cost, for all medoids in one pass.
     *
     * @param h Current object to swap with any medoid.
     * @param tnearest Distance to the nearest except the removed medoid
     * @return cost change
     */
    protected double computeReassignmentCost(DBIDRef h, WritableDoubleDataStore tnearest) {
      double cost = 0.;
      // Compute costs of reassigning other objects j:
      for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
        // New medoid is closest. Reassignment to second nearest was
        // precomputed already, in {@link #computeRemovalCost}
        // Case 1c: j is closer to h than its current medoid
        double dist = distQ.distance(h, j), cur = tnearest.doubleValue(j);
        if(dist < cur) {
          cost += dist - cur;
        }
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
  public static class Parameterizer<V> extends KMedoidsPAM.Parameterizer<V> {
    @Override
    protected KMedoidsPAMReynolds<V> makeInstance() {
      return new KMedoidsPAMReynolds<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
