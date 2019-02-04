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
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * FastPAM1: A version of PAM that is O(k) times faster, i.e., now in O((n-k)²).
 * The change here feels pretty small - we handle all k medoids in parallel
 * using an array. But this means the innermost loop only gets executed in
 * O(1/k) of all iterations, and thus we benefit on average.
 * <p>
 * This acceleration gives <em>exactly</em> (assuming perfect numerical
 * accuracy) the same results as the original PAM. For further improvements that
 * can affect the result, see also {@link KMedoidsFastPAM}, which is recommended
 * for usage in practice.
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
@Reference(authors = "Erich Schubert, Peter J. Rousseeuw", //
    title = "Faster k-Medoids Clustering: Improving the PAM, CLARA, and CLARANS Algorithms", //
    booktitle = "preprint, to appear", //
    url = "https://arxiv.org/abs/1810.05691", //
    bibkey = "DBLP:journals/corr/abs-1810-05691")
@Priority(Priority.SUPPLEMENTARY) // Use FastPAM instead
public class KMedoidsFastPAM1<V> extends KMedoidsPAM<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMedoidsFastPAM1.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMedoidsFastPAM1.class.getName();

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public KMedoidsFastPAM1(DistanceFunction<? super V> distanceFunction, int k, int maxiter, KMedoidsInitialization<V> initializer) {
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
     * @return cost
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
      // Swap phase
      DBIDVar bestid = DBIDUtil.newVar();
      DBIDArrayIter m = medoids.iter();
      int iteration = 1;
      double[] cost = new double[k];
      for(; maxiter <= 0 || iteration <= maxiter; iteration++) {
        LOG.incrementProcessed(prog);
        double best = Double.POSITIVE_INFINITY;
        int bestcluster = -1;
        // Iterate over all non-medoids:
        for(DBIDIter h = ids.iter(); h.valid(); h.advance()) {
          // Compare object to its own medoid.
          if(DBIDUtil.equal(m.seek(assignment.intValue(h) & 0x7FFF), h)) {
            continue; // This is a medoid.
          }
          // The cost we get back by making the non-medoid h medoid.
          Arrays.fill(cost, -nearest.doubleValue(h));
          computeReassignmentCost(h, cost);

          // Find the best possible swap for h:
          for(int i = 0; i < k; i++) {
            final double costi = cost[i];
            if(costi < best) {
              best = costi;
              bestid.set(h);
              bestcluster = i;
            }
          }
        }
        if(!(best < -1e-12 * tc)) {
          break; // Converged
        }
        // Update values for new medoid.
        updateAssignment(medoids, m, bestid, bestcluster);
        tc += best;
        if(LOG.isStatistics()) {
          LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + iteration + ".cost", tc));
        }
      }
      // TODO: we may have accumulated some error on tc.
      LOG.setCompleted(prog);
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
      }
      // Cleanup
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        assignment.putInt(it, assignment.intValue(it) & 0x7FFF);
      }
      return tc;
    }

    /**
     * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids
     * of those objects, that are nearest to the k<sup>th</sup> mean.
     *
     * @param means Object centroids
     * @return Assignment cost
     */
    protected double assignToNearestCluster(ArrayDBIDs means) {
      DBIDArrayIter miter = means.iter();
      double cost = 0.;
      for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
        double mindist = Double.POSITIVE_INFINITY,
            mindist2 = Double.POSITIVE_INFINITY;
        int minindx = -1, minindx2 = -1;
        for(miter.seek(0); miter.valid(); miter.advance()) {
          final double dist = distQ.distance(iditer, miter);
          if(dist < mindist) {
            minindx2 = minindx;
            mindist2 = mindist;
            minindx = miter.getOffset();
            mindist = dist;
          }
          else if(dist < mindist2) {
            minindx2 = miter.getOffset();
            mindist2 = dist;
          }
        }
        if(minindx < 0) {
          throw new AbortException("Too many infinite distances. Cannot assign objects.");
        }
        assignment.put(iditer, minindx | (minindx2 << 16));
        nearest.put(iditer, mindist);
        second.put(iditer, mindist2);
        cost += mindist;
      }
      return cost;
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
        final int pj = assignment.intValue(j) & 0x7FFF;
        cost[pj] += Math.min(dist_h, distsec) - distcur;
        if(dist_h < distcur) {
          final double delta = dist_h - distcur;
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

    /**
     * Update an existing cluster assignment.
     *
     * @param medoids Medoids set
     * @param miter Medoid iterator
     * @param h New medoid
     * @param m Position of replaced medoid
     */
    protected void updateAssignment(ArrayModifiableDBIDs medoids, DBIDArrayIter miter, DBIDRef h, int m) {
      // The new medoid itself.
      medoids.set(m, h);
      final double hdist = nearest.putDouble(h, 0);
      final int olda = assignment.intValue(h);
      // In the high short, we store the second nearest center!
      if((olda & 0x7FFF) != m) {
        assignment.putInt(h, m | ((olda & 0x7FFF) << 16));
        second.putDouble(h, hdist);
      }
      else {
        assignment.putInt(h, m | (olda & 0x7FFF0000));
      }
      // assert (distQ.distance(h, m.seek(assignment.intValue(h) & 0x7FFF)) ==
      // nearest.doubleValue(h));
      // assert (distQ.distance(h, m.seek(assignment.intValue(h) >>> 16)) ==
      // second.doubleValue(h));
      assert (DBIDUtil.equal(h, miter.seek(m)));
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
        int pj = assignment.intValue(j), po = pj >>> 16;
        pj &= 0x7FFF; // Low byte is the old nearest cluster.
        if(pj == m) { // Nearest medoid is gone.
          if(dist_h < distsec) { // Replace nearest.
            nearest.putDouble(j, dist_h);
            assignment.putInt(j, m | (po << 16));
          }
          else { // Second is new nearest.
            nearest.putDouble(j, distsec);
            // Find new second nearest.
            assignment.putInt(j, po | (updateSecondNearest(j, miter, m, dist_h, po) << 16));
          }
        }
        else { // Nearest medoid not replaced
          if(dist_h < distcur) {
            nearest.putDouble(j, dist_h);
            second.putDouble(j, distcur);
            assignment.putInt(j, m | (pj << 16));
          }
          else if(po == m) { // Second was replaced.
            assignment.putInt(j, pj | (updateSecondNearest(j, miter, m, dist_h, pj) << 16));
          }
          else if(dist_h < distsec) {
            second.putDouble(j, dist_h);
            assignment.putInt(j, pj | (m << 16));
          }
        }
      }
    }

    /**
     * Find the second nearest medoid.
     *
     * @param j Query point
     * @param medoids Medoids
     * @param h Known medoid
     * @param dist_h Distance to h
     * @param n Known nearest
     * @return Index of second nearest medoid, {@link #second} is updated.
     */
    private int updateSecondNearest(DBIDRef j, DBIDArrayIter medoids, int h, double dist_h, int n) {
      double sdist = dist_h;
      int sbest = h;
      for(medoids.seek(0); medoids.valid(); medoids.advance()) {
        if(medoids.getOffset() != h && medoids.getOffset() != n) {
          double d = distQ.distance(j, medoids);
          if(d < sdist) {
            sdist = d;
            sbest = medoids.getOffset();
          }
        }
      }
      second.putDouble(j, sdist);
      return sbest;
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
    protected KMedoidsFastPAM1<V> makeInstance() {
      return new KMedoidsFastPAM1<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
