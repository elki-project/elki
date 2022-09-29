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
package elki.clustering.kmedoids;

import elki.clustering.kmedoids.initialization.KMedoidsInitialization;
import elki.data.Clustering;
import elki.data.model.MedoidModel;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.IndefiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.math.linearalgebra.VMath;
import elki.utilities.documentation.Reference;

/**
 * Variation of FastPAM that eagerly performs any swap that yields an
 * improvement during an iteration. Compared to {@link EagerPAM} it considers
 * all current medoids together, and chooses the best of these; hence apart from
 * computing candidate swaps O(k) times faster, it will also be able to
 * sometimes choose better swaps.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert and Peter J. Rousseeuw<br>
 * Fast and Eager k-Medoids Clustering: O(k) Runtime Improvement of the PAM,
 * CLARA, and CLARANS Algorithms<br>
 * Preprint
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @navassoc - - - MedoidModel
 * @has - - - KMedoidsInitialization
 *
 * @param <O> object datatype
 */
@Reference(authors = "Erich Schubert and Peter J. Rousseeuw", //
    title = "Fast and Eager k-Medoids Clustering: O(k) Runtime Improvement of the PAM, CLARA, and CLARANS Algorithms", //
    booktitle = "arXiv preprint", //
    url = "https://arxiv.org/abs/2008.05171", //
    bibkey = "DBLP:journals/corr/abs-2008-05171")
public class FasterPAM<O> extends FastPAM<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(FasterPAM.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = FasterPAM.class.getName();

  /**
   * Constructor.
   *
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public FasterPAM(Distance<? super O> distance, int k, int maxiter, KMedoidsInitialization<O> initializer) {
    super(distance, k, maxiter, initializer);
  }

  @Override
  public Clustering<MedoidModel> run(Relation<O> relation, int k, DistanceQuery<? super O> distQ) {
    DBIDs ids = relation.getDBIDs();
    ArrayModifiableDBIDs medoids = initialMedoids(distQ, ids, k);
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
    Duration optd = getLogger().newDuration(getClass().getName() + ".optimization-time").begin();
    new Instance(distQ, ids, assignment).run(medoids, maxiter);
    getLogger().statistics(optd.end());
    return wrapResult(ids, assignment, medoids, "FasterPAM Clustering");
  }

  /**
   * Instance for a single dataset.
   * <p>
   * Note: we experimented with not caching the distance to nearest and second
   * nearest, but only the assignments. The matrix lookup was more expensive, so
   * this is probably worth the 2*n doubles in storage.
   *
   * @author Erich Schubert
   */
  protected static class Instance extends FastPAM.Instance {
    /**
     * Constructor.
     *
     * @param distQ Distance query
     * @param ids IDs to process
     * @param assignment Cluster assignment
     */
    public Instance(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment) {
      super(distQ, ids, assignment, 1.);
    }

    /**
     * Run the PAM optimization phase.
     *
     * @param medoids Medoids list
     * @param maxiter
     * @return final cost
     */
    @Override
    protected double run(ArrayModifiableDBIDs medoids, int maxiter) {
      final int k = medoids.size();
      // Initial assignment to nearest medoids
      // TODO: reuse distance information, from the build phase, when possible?
      double tc = assignToNearestCluster(medoids);
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + 0 + ".cost", tc));
      }

      // Swap phase
      IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("FasterPAM iteration", LOG) : null;
      double[] cost = new double[k], pcost = new double[k];
      // Compute costs of reassigning to the second closest medoid.
      updatePriorCost(pcost);
      DBIDArrayIter m = medoids.iter();
      DBIDVar lastswap = DBIDUtil.newVar();
      int iteration = 0, prevswaps = 0, swaps = 0;
      while(iteration < maxiter || maxiter <= 0) {
        ++iteration;
        LOG.incrementProcessed(prog);
        // Iterate over all non-medoids:
        for(DBIDIter h = ids.iter(); h.valid(); h.advance()) {
          // Check if we completed an entire round without swapping:
          if(DBIDUtil.equal(h, lastswap)) {
            break;
          }
          // Compare object to its own medoid.
          if(DBIDUtil.equal(m.seek(assignment.intValue(h) & 0x7FFF), h)) {
            continue; // This is a medoid.
          }
          // Initialize with medoid removal cost:
          System.arraycopy(pcost, 0, cost, 0, pcost.length);
          // The cost we get back by making the non-medoid h medoid.
          double acc = computeReassignmentCost(h, cost);

          // Find the best possible swap for each medoid:
          int min = VMath.argmin(cost);
          double bestcost = cost[min] + acc;
          if(!(bestcost < -1e-12 * tc)) {
            continue;
          }
          ++swaps;
          lastswap.set(h);
          updateAssignment(medoids, m, h, min);
          updatePriorCost(pcost);
          tc += bestcost;
          assert tc >= 0;
          if(LOG.isStatistics()) {
            LOG.statistics(new DoubleStatistic(KEY + ".swap-" + swaps + ".cost", tc));
          }
        }
        if(LOG.isStatistics()) {
          LOG.statistics(new LongStatistic(KEY + ".iteration-" + iteration + ".swaps", swaps - prevswaps));
        }
        if(prevswaps == swaps) {
          break; // Converged
        }
        prevswaps = swaps;
        if(LOG.isStatistics()) {
          LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + iteration + ".cost", tc));
        }
      }
      // TODO: we may have accumulated some error on tc.
      LOG.setCompleted(prog);
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
        LOG.statistics(new LongStatistic(KEY + ".swaps", swaps));
        LOG.statistics(new DoubleStatistic(KEY + ".final-cost", tc));
      }
      // Cleanup
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        assignment.putInt(it, assignment.intValue(it) & 0x7FFF);
      }
      return tc;
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
  public static class Par<O> extends FastPAM.Par<O> {
    @Override
    public FasterPAM<O> make() {
      return new FasterPAM<>(distance, k, maxiter, initializer);
    }
  }
}
