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
package elki.clustering.kmeans.quality;

import java.util.Iterator;
import java.util.List;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.math.MathUtil;
import elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Bayesian Information Criterion (BIC), also known as Schwarz criterion (SBC,
 * SBIC) for the use with evaluating k-means results.
 * <p>
 * This version tries to be close to the version used in X-means, although
 * people have argued that there are errors in this formulation.
 * <p>
 * Reference:
 * <p>
 * D. Pelleg, A. Moore:<br>
 * X-means: Extending K-means with Efficient Estimation on the Number of
 * Clusters<br>
 * Proc. 17th Int. Conf. on Machine Learning (ICML 2000)
 *
 * @author Tibor Goldschwendt
 * @author Erich Schubert
 */
@Reference(authors = "D. Pelleg, A. Moore", //
    title = "X-means: Extending K-means with Efficient Estimation on the Number of Clusters", //
    booktitle = "Proc. 17th Int. Conf. on Machine Learning (ICML 2000)", //
    url = "http://www.pelleg.org/shared/hp/download/xmeans.ps", //
    bibkey = "DBLP:conf/icml/PellegM00")
public class BayesianInformationCriterionXMeans extends AbstractKMeansQualityMeasure<NumberVector> {
  @Override
  public <V extends NumberVector> double quality(Clustering<? extends MeanModel> clustering, NumberVectorDistance<? super V> distanceFunction, Relation<V> relation) {
    return logLikelihoodXMeans(relation, clustering, distanceFunction) //
        - (.5 * numberOfFreeParameters(relation, clustering)) * FastMath.log(numPoints(clustering));
  }

  /**
   * Computes log likelihood of an entire clustering.
   * <p>
   * Version as used in the X-means publication.
   *
   * @param relation Data relation
   * @param clustering Clustering
   * @param distanceFunction Distance function
   * @return Log Likelihood.
   */
  public static double logLikelihoodXMeans(Relation<? extends NumberVector> relation, Clustering<? extends MeanModel> clustering, NumberVectorDistance<?> distanceFunction) {
    List<? extends Cluster<? extends MeanModel>> clusters = clustering.getAllClusters();
    // number of clusters
    final int m = clusters.size();

    // number of objects in the clustering
    int n = 0;
    // cluster sizes
    int[] n_i = new int[m];
    // total variance
    double d = 0.;
    // variances
    double[] d_i = new double[m];

    // Iterate over clusters:
    Iterator<? extends Cluster<? extends MeanModel>> it = clusters.iterator();
    for(int i = 0; it.hasNext(); ++i) {
      Cluster<? extends MeanModel> cluster = it.next();
      n += n_i[i] = cluster.size();
      d += d_i[i] = varianceContributionOfCluster(cluster, distanceFunction, relation);
    }

    // No remaining variance, if every point is on its own:
    if(n <= m) {
      return Double.NEGATIVE_INFINITY;
    }

    // Total variance (corrected for bias)
    final double logv = FastMath.log(d / (n - m));

    final int dim = RelationUtil.dimensionality(relation);
    // log likelihood of this clustering
    double logLikelihood = 0.;

    // Aggregate
    for(int i = 0; i < m; i++) {
      logLikelihood += n_i[i] * FastMath.log(n_i[i]) // Post. entropy Rn log Rn
          - n_i[i] * .5 * MathUtil.LOGTWOPI // Rn/2 log2pi
          - n_i[i] * dim * .5 * logv // Rn M/2 log sigma^2
          - (d_i[i] - m) * .5 // (Rn-K)/2
          - n_i[i] * FastMath.log(n); // Prior entropy, sum_i Rn log R
    }
    return logLikelihood;
  }

  @Override
  public boolean isBetter(double currentCost, double bestCost) {
    // Careful: bestCost may be NaN!
    return !(currentCost <= bestCost);
  }
}
