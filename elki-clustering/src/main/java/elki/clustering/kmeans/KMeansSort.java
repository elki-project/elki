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
package elki.clustering.kmeans;

import java.util.Arrays;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.distance.distancefunction.NumberVectorDistance;
import elki.logging.Logging;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;

/**
 * Sort-Means: Accelerated k-means by exploiting the triangle inequality and
 * pairwise distances of means to prune candidate means (with sorting).
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
@Title("Sort-Means")
@Reference(authors = "S. J. Phillips", //
    title = "Acceleration of k-means and related clustering algorithms", //
    booktitle = "Proc. 4th Int. Workshop on Algorithm Engineering and Experiments (ALENEX 2002)", //
    url = "https://doi.org/10.1007/3-540-45643-0_13", //
    bibkey = "DBLP:conf/alenex/Phillips02")
@Priority(Priority.RECOMMENDED)
public class KMeansSort<V extends NumberVector> extends KMeansCompare<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansSort.class);

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   */
  public KMeansSort(NumberVectorDistance<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer) {
    super(distanceFunction, k, maxiter, initializer);
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    Instance instance = new Instance(relation, getDistance(), initialMeans(database, relation));
    instance.run(maxiter);
    return instance.buildResult();
  }

  /**
   * Inner instance, storing state for a single data set.
   *
   * @author Erich Schubert
   */
  protected static class Instance extends KMeansCompare.Instance {
    /**
     * Sorted neighbors
     */
    int[][] cnum;

    /**
     * Constructor.
     *
     * @param relation Relation
     * @param df Distance function
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
      cnum = new int[k][k - 1];
    }

    @Override
    protected int assignToNearestCluster() {
      nearestMeans(cdist, cnum);
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
        double mindist = distance(fv, means[ini]);
        final double threshold = mult * mindist;
        int minIndex = ini;
        for(int i : cnum[ini]) {
          if(cdist[minIndex][i] >= threshold) { // Sort pruning
            break; // All following can only be worse.
          }
          double dist = distance(fv, means[i]);
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
    protected KMeansSort<V> makeInstance() {
      return new KMeansSort<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
