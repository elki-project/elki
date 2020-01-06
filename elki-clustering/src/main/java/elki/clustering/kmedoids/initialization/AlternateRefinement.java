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
package elki.clustering.kmedoids.initialization;

import java.util.ArrayList;
import java.util.Arrays;

import elki.clustering.kmeans.initialization.KMeansPlusPlus;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.math.linearalgebra.VMath;
import elki.utilities.documentation.Reference;
import elki.utilities.exceptions.AbortException;
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
    // For storing the initial clusters:
    ArrayList<ModifiableDBIDs> clusters = new ArrayList<>(k);
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newArray((ids.size() / k) + 10));
    }
    // Get initial medoids, assign points.
    ArrayModifiableDBIDs medoids = DBIDUtil.newArray(inner.chooseInitialMedoids(k, ids, distQ));
    k = medoids.size();
    double[] cost = new double[k];
    double tc = assignToNearestCluster(medoids, ids, distQ, clusters, cost);
    if(LOG.isStatistics()) {
      LOG.statistics(new DoubleStatistic(getClass().getName() + ".initial-cost", tc));
    }
    // Refine medoids
    DBIDArrayMIter miter = medoids.iter();
    // Swap phase
    int iteration = 0;
    while(iteration < maxiter || maxiter <= 0) {
      ++iteration;
      boolean changed = false;
      // Try to swap the medoid with a better cluster member:
      for(int i = 0; i < k; i++) {
        double bestm = findMedoid(clusters.get(i), distQ, miter.seek(i), cost[i]);
        if(bestm < cost[i]) {
          changed = true;
          cost[i] = bestm;
        }
      }
      if(!changed || iteration == maxiter) {
        break;
      }
      double nc = VMath.sum(cost);
      tc = assignToNearestCluster(medoids, ids, distQ, clusters, cost);
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
   * @param ci Cluster
   * @param distQ Distance query
   * @param miter Medoid iterator, pointing to the current medoid (modified)
   * @param bestm Prior cost, of the current assignment
   * @return New cost
   */
  protected static double findMedoid(ModifiableDBIDs ci, DistanceQuery<?> distQ, DBIDArrayMIter miter, double bestm) {
    for(DBIDIter iter = ci.iter(); iter.valid(); iter.advance()) {
      if(DBIDUtil.equal(miter, iter)) {
        continue;
      }
      double sum = 0;
      for(DBIDIter iter2 = ci.iter(); iter2.valid() && sum < bestm; iter2.advance()) {
        if(DBIDUtil.equal(iter, iter2)) {
          continue;
        }
        sum += distQ.distance(iter, iter2);
      }
      if(sum < bestm) {
        miter.setDBID(iter);
        bestm = sum;
      }
    }
    return bestm;
  }

  /**
   * Compute the initial cluster assignment.
   *
   * @param medoids Initial medoids
   * @param ids All objects
   * @param distQ Distance query
   * @param clusters Output: clusters
   * @param cost Output: cost per cluster
   * @return Cost (and "clusters" is changed)
   */
  protected static double assignToNearestCluster(ArrayDBIDs medoids, DBIDs ids, DistanceQuery<?> distQ, ArrayList<ModifiableDBIDs> clusters, double[] cost) {
    Arrays.fill(cost, 0);
    for(ModifiableDBIDs c : clusters) {
      c.clear();
    }
    DBIDArrayIter miter = medoids.iter();
    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      double mindist = Double.POSITIVE_INFINITY;
      int minindx = -1;
      for(miter.seek(0); miter.valid(); miter.advance()) {
        final double dist = distQ.distance(iditer, miter);
        if(dist < mindist) {
          minindx = miter.getOffset();
          mindist = dist;
        }
      }
      if(minindx < 0) {
        throw new AbortException("Too many infinite distances. Cannot assign objects.");
      }
      clusters.get(minindx).add(iditer);
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
