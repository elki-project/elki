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
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Compare-Means: Accelerated k-means by exploiting the triangle inequality and
 * pairwise distances of means to prune candidate means.
 * <p>
 * Reference:
 * <p>
 * S. J. Phillips<br>
 * Acceleration of k-means and related clustering algorithms<br>
 * Proc. 4th Int. W. on Algorithm Engineering and Experiments (ALENEX 2002)
 *
 * @author Erich Schubert
 * @since 0.7.1
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Title("Compare-Means")
@Reference(authors = "S. J. Phillips", //
    title = "Acceleration of k-means and related clustering algorithms", //
    booktitle = "Proc. 4th Int. Workshop on Algorithm Engineering and Experiments (ALENEX 2002)", //
    url = "https://doi.org/10.1007/3-540-45643-0_13", //
    bibkey = "DBLP:conf/alenex/Phillips02")
@Priority(Priority.RECOMMENDED - 1)
public class KMeansCompare<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansCompare.class);

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   */
  public KMeansCompare(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer) {
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
     * Cluster center distances.
     */
    double[][] cdist;

    /**
     * Constructor.
     *
     * @param relation Relation
     * @param df Distance function
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistanceFunction<?> df, double[][] means) {
      super(relation, df, means);
      cdist = new double[k][k];
    }

    @Override
    protected int iterate(int iteration) {
      if(iteration > 1) {
        means = means(clusters, means, relation);
      }
      recomputeSeperation(means, cdist);
      return assignToNearestCluster();
    }

    /**
     * Recompute the separation of cluster means.
     * <p>
     * Used by Sort and Compare variants.
     *
     * @param means Means
     * @param cdist Center-to-Center distances (half-sqrt scaled)
     */
    protected void recomputeSeperation(double[][] means, double[][] cdist) {
      final int k = means.length;
      for(int i = 1; i < k; i++) {
        DoubleVector mi = DoubleVector.wrap(means[i]);
        for(int j = 0; j < i; j++) {
          double d = distance(mi, DoubleVector.wrap(means[j]));
          cdist[i][j] = cdist[j][i] = .5 * d;
        }
      }
    }

    @Override
    protected int assignToNearestCluster() {
      int changed = 0;
      // Reset all clusters
      Arrays.fill(varsum, 0.);
      for(ModifiableDBIDs cluster : clusters) {
        cluster.clear();
      }
      final double mult = isSquared ? 4 : 2;
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        final int cur = assignment.intValue(iditer), ini = cur >= 0 ? cur : 0;
        // Distance to current mean:
        NumberVector fv = relation.get(iditer);
        double mindist = distance(fv, DoubleVector.wrap(means[ini]));
        final double threshold = mult * mindist;
        int minIndex = ini;
        for(int i = 0; i < k; i++) {
          if(i == ini || cdist[minIndex][i] >= threshold) { // Compare pruning
            continue;
          }
          double dist = distance(fv, DoubleVector.wrap(means[i]));
          if(dist < mindist) {
            minIndex = i;
            mindist = dist;
          }
        }
        varsum[minIndex] += mindist;
        clusters.get(minIndex).add(iditer);
        if(assignment.putInt(iditer, minIndex) != minIndex) {
          ++changed;
        }
      }
      return changed;
    }

    @Override
    protected Logging getLogger() {
      return LOG;
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
    protected boolean needsMetric() {
      return true;
    }

    @Override
    protected KMeansCompare<V> makeInstance() {
      return new KMeansCompare<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
