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
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;

/**
 * Variation of PAM that eagerly performs all swaps that yield an improvement
 * during an iteration. This has been used as early as in Whitaker's "fast
 * interchange" heuristic, and constitutes a variant of the "local
 * hill-climbing" approach considered by Estivill-Castro.
 * <p>
 * Reference:
 * <p>
 * R. A. Whitaker<br>
 * A Fast Algorithm For The Greedy Interchange For Large-Scale Clustering And
 * Median Location Problems<br>
 * INFOR: Information Systems and Operational Research 21(2)
 * <p>
 * V. Estivill-Castro and A. T. Murray<br>
 * Discovering Associations in Spatial Data - An Efficient Medoid Based
 * Approach<br>
 * Proc. 2nd Pacific-Asia Conf. on Research and Development in Knowledge
 * Discovery and Data Mining, PAKDD-98
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @navassoc - - - MedoidModel
 * @has - - - KMedoidsInitialization
 *
 * @param <O> object datatype
 */
@Priority(Priority.SUPPLEMENTARY)
@Reference(authors = "R. A. Whitaker", //
    title = "A Fast Algorithm For The Greedy Interchange For Large-Scale Clustering And Median Location Problems", //
    booktitle = "INFOR: Information Systems and Operational Research 21(2)", //
    url = "https://doi.org/10.1080/03155986.1983.11731889", //
    bibkey = "doi:10.1080/03155986.1983.11731889")
@Reference(authors = "V. Estivill-Castro and A. T. Murray", //
    title = "Discovering Associations in Spatial Data - An Efficient Medoid Based Approach", //
    booktitle = "Proc. 2nd Pacific-Asia Conf. on Research and Development in Knowledge Discovery and Data Mining, PAKDD-98", //
    url = "https://doi.org/10.1007/3-540-64383-4_10", //
    bibkey = "DBLP:conf/pakdd/Estivill-CastroM98")
public class EagerPAM<O> extends PAM<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(EagerPAM.class);

  /**
   * Key for loggers.
   */
  private static final String KEY = EagerPAM.class.getName();

  /**
   * Constructor.
   *
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public EagerPAM(Distance<? super O> distance, int k, int maxiter, KMedoidsInitialization<O> initializer) {
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
    return wrapResult(ids, assignment, medoids, "EagerPAM Clustering");
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
     * Run the EagerPAM optimization phase.
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
      double tc = assignToNearestCluster(medoids), nc = tc;
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + 0 + ".cost", tc));
      }

      final boolean metric = distQ.getDistance().isMetric();

      IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("PAM iteration", LOG) : null;
      // Swap phase
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
          if(DBIDUtil.equal(m.seek(assignment.intValue(h)), h)) {
            continue; // This is a medoid.
          }
          final double hdist = nearest.doubleValue(h); // Current cost of h.
          if(metric && hdist <= 0.) {
            continue; // Duplicate of a medoid.
          }
          // Find the best possible swap for h:
          for(int pi = 0; pi < k; pi++) {
            // hdist is the cost we get back by making the non-medoid h medoid.
            final double cpi = computeReassignmentCost(h, pi) - hdist;
            if(cpi < -1e-12 * nc) {
              ++swaps;
              medoids.set(pi, h);
              lastswap.set(h);
              // Reassign
              nc = assignToNearestCluster(medoids);
              if(LOG.isStatistics()) {
                LOG.statistics(new DoubleStatistic(KEY + ".swap-" + swaps + ".cost", nc));
              }
              break;
            }
          }
        }
        if(LOG.isStatistics()) {
          LOG.statistics(new LongStatistic(KEY + ".iteration-" + iteration + ".swaps", swaps - prevswaps));
        }
        if(prevswaps == swaps) {
          break;
        }
        if(nc > tc) {
          LOG.warning("EagerPAM failed to converge (numerical instability?)");
          break;
        }
        tc = nc;
        prevswaps = swaps;
      }
      LOG.setCompleted(prog);
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(KEY + ".swaps", swaps));
        LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
        LOG.statistics(new DoubleStatistic(KEY + ".final-cost", tc));
      }
      return tc;
    }
  }

  /**
   * Get the static class logger.
   */
  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> extends PAM.Par<O> {
    @Override
    public EagerPAM<O> make() {
      return new EagerPAM<>(distance, k, maxiter, initializer);
    }
  }
}
