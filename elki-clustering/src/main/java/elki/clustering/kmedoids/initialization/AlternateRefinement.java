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
package elki.clustering.kmedoids.initialization;

import java.util.Arrays;

import elki.clustering.kmeans.initialization.KMeansPlusPlus;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.IntegerDataStore;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.math.linearalgebra.VMath;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Meta-Initialization for k-medoids by performing one (or many) k-means-style
 * iteration. Such iterations have been used by by Maranzana, Reynolds,
 * and are the core of Park and Jun's approach for k-medoids.
 * <p>
 * Captivo is an early author to propose integrating "alternate" into "greedy"
 * for initialization, whereas the others only considered this as an algorithm
 * of its own. But since the quality of this heuristic is much worse than the
 * interchange heuristics, its better suited as initialization.
 * Captivo's method is available as {@link GreedyG}. This implementation simply
 * uses the "alternate" heuristic to refine initial medoids obtained by some
 * other heuristic.
 * <p>
 * Reference:
 * <p>
 * M. Eugénia Captivo<br>
 * Fast primal and dual heuristics for the p-median location problem<br>
 * European Journal of Operational Research 52(1)
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @param <O> Object type for KMedoids initialization
 */
@Reference(authors = "M. Eugénia Captivo", //
    title = "Fast primal and dual heuristics for the p-median location problem", //
    booktitle = "European Journal of Operational Research 52(1)", //
    url = "https://doi.org/10.1016/0377-2217(91)90336-T", //
    bibkey = "doi:10.1016/0377-2217(91)90336-T")
@Reference(authors = "F. E. Maranzana", //
    title = "On the location of supply points to minimize transport costs", //
    booktitle = "Journal of the Operational Research Society 15.3", //
    url = "https://doi.org/10.1057/jors.1964.47", //
    bibkey = "doi:10.1057/jors.1964.47")
public class AlternateRefinement<O> implements KMedoidsInitialization<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(AlternateRefinement.class);

  /**
   * Inner initialization.
   */
  private KMedoidsInitialization<O> inner;

  /**
   * Maximum number of refinement iterations.
   */
  int maxiter;

  /**
   * Constructor.
   */
  public AlternateRefinement(KMedoidsInitialization<O> inner, int maxiter) {
    super();
    this.inner = inner;
    this.maxiter = maxiter;
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DBIDs ids, DistanceQuery<? super O> distQ) {
    // Get initial medoids, assign points.
    ArrayModifiableDBIDs medoids = DBIDUtil.newArray(inner.chooseInitialMedoids(k, ids, distQ));
    DBIDArrayMIter miter = medoids.iter();
    k = medoids.size();
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, 0);

    double[] cost = new double[k];
    double tc = assignToNearestCluster(miter, ids, distQ, assignment, cost);
    if(LOG.isStatistics()) {
      LOG.statistics(new DoubleStatistic(getClass().getName() + ".initial-cost", tc));
    }
    // Refine medoids
    int iteration = 0;
    while(iteration < maxiter || maxiter <= 0) {
      ++iteration;
      boolean changed = false;
      // Try to swap the medoid with a better cluster member:
      for(int i = 0; i < k; i++) {
        changed |= findMedoid(ids, distQ, assignment, i, miter.seek(i), cost);
      }
      if(!changed || iteration == maxiter) {
        break;
      }
      double nc = VMath.sum(cost);
      tc = assignToNearestCluster(miter, ids, distQ, assignment, cost);
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(getClass().getName() + ".iteration." + iteration + ".estimated-cost", nc));
        LOG.statistics(new DoubleStatistic(getClass().getName() + ".iteration." + iteration + ".reassigned-cost", tc));
      }
      if(tc >= nc) {
        break; // No further improvement
      }
    }
    LOG.statistics(new DoubleStatistic(getClass().getName() + ".refined-cost", tc));
    return medoids;
  }

  /**
   * Find the best medoid of a given fixed set.
   * 
   * @param ids Object ids
   * @param distQ Distance query
   * @param assignment Cluster assignment
   * @param j Cluster number
   * @param miter Medoid iterator, pointing to the current medoid (modified)
   * @param cost Prior cost, of the current assignment / cost[j] is output
   * @return {@code true} if the medoid changed.
   */
  public static boolean findMedoid(DBIDs ids, DistanceQuery<?> distQ, IntegerDataStore assignment, int j, DBIDArrayMIter miter, double[] cost) {
    boolean changed = false;
    double bestm = cost[j];
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      if(DBIDUtil.equal(miter, iter) || assignment.intValue(iter) != j) {
        continue;
      }
      double sum = 0;
      for(DBIDIter iter2 = ids.iter(); iter2.valid() && sum < bestm; iter2.advance()) {
        if(DBIDUtil.equal(iter, iter2) || assignment.intValue(iter2) != j) {
          continue;
        }
        sum += distQ.distance(iter, iter2);
      }
      if(sum < bestm) {
        miter.setDBID(iter);
        bestm = sum;
        changed = true;
      }
    }
    cost[j] = bestm;
    return changed;
  }

  /**
   * Compute the initial cluster assignment.
   *
   * @param miter Medoids iterator
   * @param ids All objects
   * @param distQ Distance query
   * @param assignment Output: clusters
   * @param cost Output: cost per cluster
   * @return Cost (and "clusters" is changed)
   */
  public static double assignToNearestCluster(DBIDArrayIter miter, DBIDs ids, DistanceQuery<?> distQ, WritableIntegerDataStore assignment, double[] cost) {
    Arrays.fill(cost, 0);
    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      int curindx = assignment.intValue(iditer), minindx = curindx;
      double mindist = distQ.distance(iditer, miter.seek(curindx));
      for(miter.seek(0); miter.valid(); miter.advance()) {
        if(miter.getOffset() == curindx) {
          continue;
        }
        final double dist = distQ.distance(iditer, miter);
        if(dist < mindist) {
          minindx = miter.getOffset();
          mindist = dist;
        }
      }
      assignment.put(iditer, minindx);
      cost[minindx] += mindist;
    }
    return VMath.sum(cost);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Nested inner initialization.
     */
    public static final OptionID INIT_P = new OptionID("kmedoids.inner.init", "Nested inner initialization.");

    /**
     * Maximum number of refinement steps.
     */
    public static final OptionID MAXITER_P = new OptionID("kmedoids.init.maxiter", "Refinement steps on initialization.");

    /**
     * Inner initialization.
     */
    private KMedoidsInitialization<O> inner;

    /**
     * Maximum number of refinement iterations.
     */
    int maxiter;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<KMedoidsInitialization<O>>(INIT_P, KMedoidsInitialization.class, KMeansPlusPlus.class) //
          .grab(config, x -> inner = x);
      new IntParameter(MAXITER_P) //
          .setDefaultValue(0) //
          .grab(config, x -> maxiter = x);
    }

    @Override
    public AlternateRefinement<O> make() {
      return new AlternateRefinement<>(inner, maxiter);
    }
  }
}
