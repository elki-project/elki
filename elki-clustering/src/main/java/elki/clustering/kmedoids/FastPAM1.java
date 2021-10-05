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
package elki.clustering.kmedoids;

import java.util.Arrays;

import elki.clustering.kmedoids.initialization.KMedoidsInitialization;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.IndefiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.exceptions.AbortException;

/**
 * FastPAM1: A version of PAM that is O(k) times faster, i.e., now in O((n-k)²).
 * The change here feels pretty small - we handle all k medoids in parallel
 * using an array. But this means the innermost loop only gets executed in
 * O(1/k) of all iterations, and thus we benefit on average.
 * <p>
 * This acceleration gives <em>exactly</em> (assuming perfect numerical
 * accuracy) the same results as the original PAM. For further improvements that
 * can affect the result, see also {@link FastPAM}, which is recommended
 * for usage in practice.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Peter J. Rousseeuw<br>
 * Faster k-Medoids Clustering: Improving the PAM, CLARA, and CLARANS
 * Algorithms<br>
 * Proc. 12th Int. Conf. Similarity Search and Applications (SISAP'2019)
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <V> vector datatype
 */
@Reference(authors = "Erich Schubert, Peter J. Rousseeuw", //
    title = "Faster k-Medoids Clustering: Improving the PAM, CLARA, and CLARANS Algorithms", //
    booktitle = "Proc. 12th Int. Conf. Similarity Search and Applications (SISAP'2019)", //
    url = "https://doi.org/10.1007/978-3-030-32047-8_16", //
    bibkey = "DBLP:conf/sisap/SchubertR19")
@Priority(Priority.SUPPLEMENTARY) // Use FastPAM instead
public class FastPAM1<V> extends PAM<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(FastPAM1.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = FastPAM1.class.getName();

  /**
   * Constructor.
   *
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public FastPAM1(Distance<? super V> distance, int k, int maxiter, KMedoidsInitialization<V> initializer) {
    super(distance, k, maxiter, initializer);
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
  protected static class Instance extends PAM.Instance {
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
      double[] cost = new double[k], pcost = new double[k];
      int iteration = 0;
      while(iteration < maxiter || maxiter <= 0) {
        ++iteration;
        LOG.incrementProcessed(prog);
        // Compute costs of reassigning to the second closest medoid.
        updatePriorCost(pcost);
        double best = Double.POSITIVE_INFINITY;
        int bestcluster = -1;
        // Iterate over all non-medoids:
        for(DBIDIter h = ids.iter(); h.valid(); h.advance()) {
          // Compare object to its own medoid.
          if(DBIDUtil.equal(m.seek(assignment.intValue(h) & 0x7FFF), h)) {
            continue; // This is a medoid.
          }
          // Initialize with medoid removal cost:
          System.arraycopy(pcost, 0, cost, 0, pcost.length);
          // The cost we get back by making the non-medoid h medoid.
          final double acc = computeReassignmentCost(h, cost);

          // Find the best possible swap for h:
          for(int i = 0; i < k; i++) {
            final double costi = cost[i] + acc;
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
        LOG.statistics(new DoubleStatistic(KEY + ".final-cost", tc));
      }
      // Cleanup
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        assignment.putInt(it, assignment.intValue(it) & 0x7FFF);
      }
      return tc;
    }

    /**
     * Prior assignment costs.
     *
     * @param pcost Prior cost.
     */
    protected void updatePriorCost(double[] pcost) {
      WritableIntegerDataStore a = assignment;
      WritableDoubleDataStore s = second, n = nearest;
      Arrays.fill(pcost, 0);
      for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
        pcost[a.intValue(j) & 0x7FFF] += s.doubleValue(j) - n.doubleValue(j);
      }
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
        double mindist = Double.POSITIVE_INFINITY;
        double mindist2 = Double.POSITIVE_INFINITY;
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
     * @param xj Current object to swap with any medoid.
     * @param loss Loss change aggregation array, must have size k
     * @return Loss change accumulator that applies to all
     */
    protected double computeReassignmentCost(DBIDRef xj, double[] loss) {
      final WritableDoubleDataStore nearest = this.nearest;
      final WritableDoubleDataStore second = this.second;
      final WritableIntegerDataStore assignment = this.assignment;
      double acc = 0.;
      // Compute costs of reassigning other objects o:
      for(DBIDIter xo = ids.iter(); xo.valid(); xo.advance()) {
        final double dn = nearest.doubleValue(xo), ds = second.doubleValue(xo);
        final double dxo = distQ.distance(xj, xo);
        // Case (i): new medoid is closest:
        if(dxo < dn) {
          acc += dxo - dn;
          // loss already includes ds - dn, remove
          loss[assignment.intValue(xo) & 0x7FFF] += dn - ds;
        }
        else if(dxo < ds) {
          // loss already includes ds - dn, adjust to d(xo) - dn
          loss[assignment.intValue(xo) & 0x7FFF] += dxo - ds;
        }
      }
      return acc;
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
            // assignment.putInt(j, m | (po << 16));
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
    protected int updateSecondNearest(DBIDRef j, DBIDArrayIter medoids, int h, double dist_h, int n) {
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
  public static class Par<V> extends PAM.Par<V> {
    @Override
    public FastPAM1<V> make() {
      return new FastPAM1<>(distance, k, maxiter, initializer);
    }
  }
}
