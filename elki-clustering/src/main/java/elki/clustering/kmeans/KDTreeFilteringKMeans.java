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
package elki.clustering.kmeans;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;

/**
 * Filtering or "blacklisting" K-means with k-d-tree acceleration.
 * <p>
 * This is an implementation of the slightly later (1999) variant of Pelleg and
 * Moore respectively Kanungo et al. using hyperplanes for filtering instead of
 * a similar approach using minimum and maximum distances published in 1997 by
 * Alsabti et al., see {@link KDTreePruningKMeans}.
 * <p>
 * References:
 * <p>
 * D. Pelleg and A. Moore<br>
 * Accelerating Exact k-means Algorithms with Geometric Reasoning<br>
 * Proc. ACM SIGKDD Int. Conf. Knowledge Discovery and Data Mining
 * <p>
 * T. Kanungo, D. M. Mount, N. S. Netanyahu, C. D. Piatko, R. Silverman, A. Y.
 * Wu<br>
 * Computing Nearest Neighbors for Moving Points and Applications to
 * Clustering<br>
 * Proc. 10th ACM-SIAM Symposium on Discrete Algorithms (SODA'99)
 * <p>
 * A more detailed analysis appeared later in:
 * <p>
 * T. Kanungo, D. M. Mount, N. S. Netanyahu, C. D. Piatko, R. Silverman, A. Y.
 * Wu<br>
 * An Efficient k-Means Clustering Algorithm: Analysis and Implementation<br>
 * IEEE Transactions on Pattern Analysis and Machine Intelligence 24(7)
 *
 * @author Cedrik Lüdicke (initial version)
 * @author Erich Schubert (optimizations, rewrite)
 * @since 0.8.0
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Title("K-d-tree K-means with Filtering")
@Reference(authors = "D. Pelleg, A. Moore", //
    title = "Accelerating Exact k-means Algorithms with Geometric Reasoning", //
    booktitle = "Proc. ACM SIGKDD Int. Conf. Knowledge Discovery and Data Mining", //
    url = "https://doi.org/10.1145/312129.312248", //
    bibkey = "DBLP:conf/kdd/PellegM99")
@Reference(authors = "T. Kanungo, D. M. Mount, N. S. Netanyahu, C. D. Piatko, R. Silverman, A. Y. Wu", //
    title = "Computing Nearest Neighbors for Moving Points and Applications to Clustering", //
    booktitle = "Proc. 10th ACM-SIAM Symposium on Discrete Algorithms (SODA'99)", //
    url = "http://dl.acm.org/citation.cfm?id=314500.315095", //
    bibkey = "DBLP:conf/soda/KanungoMNPSW99")
@Reference(authors = "T. Kanungo, D. M. Mount, N. S. Netanyahu, C. D. Piatko, R. Silverman, A. Y. Wu", //
    title = "An Efficient k-Means Clustering Algorithm: Analysis and Implementation", //
    booktitle = "IEEE Transactions on Pattern Analysis and Machine Intelligence 24(7)", //
    url = "https://doi.org/10.1109/TPAMI.2002.1017616", //
    bibkey = "DBLP:journals/pami/KanungoMNPSW02") //
public class KDTreeFilteringKMeans<V extends NumberVector> extends KDTreePruningKMeans<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KDTreeFilteringKMeans.class);

  /**
   * Constructor.
   *
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param split Splitting strategy
   * @param leafsize Leaf size
   */
  public KDTreeFilteringKMeans(NumberVectorDistance<? super V> distance, int k, int maxiter, KMeansInitialization initializer, Split split, int leafsize) {
    super(distance, k, maxiter, initializer, split, leafsize);
  }

  @Override
  public Clustering<KMeansModel> run(Relation<V> relation) {
    Instance instance = new Instance(relation, distance, initialMeans(relation));
    instance.run(maxiter);
    return instance.buildResult();
  }

  /**
   * Inner instance, storing state for a single data set.
   *
   * @author Cedrik Lüdicke
   * @author Erich Schubert
   */
  protected class Instance extends KDTreePruningKMeans<V>.Instance {
    /**
     * Constructor.
     *
     * @param relation Relation of data points
     * @param df Distance function
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
    }

    @Override
    protected int pruning(KDNode u, int alive) {
      final double[] mid = u.mid, halfwidth = u.halfwidth;
      int nearest = getNearestCenter(mid, alive);
      if(nearest > 0) {
        final int swap = indices[0];
        indices[0] = indices[nearest];
        indices[nearest] = swap;
      }

      // Filter remaining indexes. Disable by swapping after index range.
      final double[] nmean = means[indices[0]];
      for(int i = 1; i < alive;) {
        if(isFarther(nmean, means[indices[i]], mid, halfwidth)) {
          --alive;
          final int swap = indices[i];
          indices[i] = indices[alive];
          indices[alive] = swap;
        } else {
          i++;
        }
      }
      return alive;
    }

    /**
     * Get the nearest (alive) center to a midpoint.
     * 
     * @param mid midpoint
     * @param alive Number of alive centers
     * @return best center
     */
    protected int getNearestCenter(double[] mid, int alive) {
      int best = 0;
      double bestDistance = Double.POSITIVE_INFINITY;
      for(int i = 0; i < alive; i++) {
        double distance = distance(mid, means[indices[i]]);
        if(distance < bestDistance) {
          best = i;
          bestDistance = distance;
        }
      }
      return best;
    }

    /**
     * Check if a cluster mean is farther than another.
     * 
     * Optimized version of the comparison suggested by Kanungo.
     */
    protected boolean isFarther(double[] z_star, double[] z, double[] mid, double[] halfwidth) {
      ++diststat; // Equivalent in effort to a distance computation.
      double diff = 0;
      for(int i = 0; i < z.length; i++) {
        double v = z[i] < z_star[i] ? (mid[i] - halfwidth[i]) : (mid[i] + halfwidth[i]);
        double delta1 = z_star[i] - v, delta2 = z[i] - v;
        diff += delta1 * delta1 - delta2 * delta2;
      }
      return diff < 0;
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
  public static class Par<V extends NumberVector> extends KDTreePruningKMeans.Par<V> {
    @Override
    public KDTreeFilteringKMeans<V> make() {
      return new KDTreeFilteringKMeans<>(distance, k, maxiter, initializer, split, leafsize);
    }
  }
}
