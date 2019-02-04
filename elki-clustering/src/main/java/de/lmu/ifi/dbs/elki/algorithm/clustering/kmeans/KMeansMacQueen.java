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

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * The original k-means algorithm, using MacQueen style incremental updates;
 * making this effectively an "online" (streaming) algorithm.
 * <p>
 * This implementation will by default iterate over the data set until
 * convergence, although MacQueen likely only meant to do a single pass over the
 * data, but the result quality improves with multiple passes.
 * <p>
 * Reference:
 * <p>
 * J. MacQueen<br>
 * Some Methods for Classification and Analysis of Multivariate Observations<br>
 * 5th Berkeley Symp. Math. Statist. Prob.
 *
 * @author Erich Schubert
 * @since 0.1
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector type to use
 */
@Title("k-Means (MacQueen Algorithm)")
@Reference(authors = "J. MacQueen", //
    title = "Some Methods for Classification and Analysis of Multivariate Observations", //
    booktitle = "5th Berkeley Symp. Math. Statist. Prob.", //
    url = "http://projecteuclid.org/euclid.bsmsp/1200512992", //
    bibkey = "conf/bsmsp/MacQueen67")
public class KMeansMacQueen<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansMacQueen.class);

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   */
  public KMeansMacQueen(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer) {
    super(distanceFunction, k, maxiter, initializer);
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    Instance instance = new Instance(relation, getDistanceFunction(), initialMeans(database, relation));
    instance.run(maxiter);
    return instance.buildResult();
  }

  /**
   * Inner instance, storing state for a single data set.
   *
   * @author Erich Schubert
   */
  protected static class Instance extends AbstractKMeans.Instance {
    /**
     * Constructor.
     *
     * @param relation Relation
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistanceFunction<?> df, double[][] means) {
      super(relation, df, means);
    }

    @Override
    protected int iterate(int iteration) {
      int changed = 0;
      Arrays.fill(varsum, 0.);

      // Incremental update
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        double mindist = Double.POSITIVE_INFINITY;
        NumberVector fv = relation.get(iditer);
        int minIndex = 0;
        for(int i = 0; i < k; i++) {
          double dist = distance(fv, DoubleVector.wrap(means[i]));
          if(dist < mindist) {
            minIndex = i;
            mindist = dist;
          }
        }
        varsum[minIndex] += mindist;
        if(updateMeanAndAssignment(minIndex, fv, iditer)) {
          ++changed;
        }
      }
      return changed;
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }

    /**
     * Try to update the cluster assignment.
     *
     * @param minIndex Cluster to assign to
     * @param fv Vector
     * @param iditer Object ID
     * @return {@code true} when assignment changed
     */
    private boolean updateMeanAndAssignment(int minIndex, NumberVector fv, DBIDIter iditer) {
      int cur = assignment.intValue(iditer);
      if(cur == minIndex) {
        return false;
      }
      final ModifiableDBIDs curclus = clusters.get(minIndex);
      curclus.add(iditer);
      incrementalUpdateMean(means[minIndex], fv, curclus.size(), +1);

      if(cur >= 0) {
        ModifiableDBIDs ci = clusters.get(cur);
        ci.remove(iditer);
        incrementalUpdateMean(means[cur], fv, ci.size() + 1, -1);
      }

      assignment.putInt(iditer, minIndex);
      return true;
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
  public static class Parameterizer<V extends NumberVector> extends AbstractKMeans.Parameterizer<V> {
    @Override
    protected KMeansMacQueen<V> makeInstance() {
      return new KMeansMacQueen<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
