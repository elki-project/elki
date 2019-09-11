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
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.data.model.MeanModel;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.math.MathUtil;
import elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Base class for evaluating clusterings by information criteria (such as AIC or
 * BIC). Provides helper functions (e.g. max likelihood calculation) to its
 * subclasses.
 * <p>
 * References:
 * <p>
 * The use of information-theoretic criteria for evaluating k-means was
 * popularized by X-means (see {@link BayesianInformationCriterionXMeans}):
 * <p>
 * D. Pelleg, A. Moore<br>
 * X-means: Extending K-means with Efficient Estimation on the Number of
 * Clusters<br>
 * Proc. 17th Int. Conf. on Machine Learning (ICML 2000)
 * <p>
 * A different version of logLikelihood is derived in (see
 * {@link BayesianInformationCriterionZhao}):
 * <p>
 * Q. Zhao, M. Xu, P. Fränti<br>
 * Knee Point Detection on Bayesian Information Criterion<br>
 * 20th IEEE International Conference on Tools with Artificial Intelligence
 * <p>
 * A longer derivation (but with a sign mistake) can be found in:
 * <p>
 * A. Foglia, B. Hancock<br>
 * Notes on Bayesian Information Criterion Calculation for X-Means
 * Clustering<br>
 * https://github.com/bobhancock/goxmeans/blob/master/doc/BIC_notes.pdf
 *
 * @author Tibor Goldschwendt
 * @author Erich Schubert
 * @since 0.7.0
 */
public abstract class AbstractKMeansQualityMeasure<O extends NumberVector> implements KMeansQualityMeasure<O> {
  /**
   * Compute the number of points in a given set of clusters (which may be
   * <i>less</i> than the complete data set for X-means!)
   *
   * @param clustering Clustering to analyze
   * @return Number of points
   */
  public static int numPoints(Clustering<? extends MeanModel> clustering) {
    int n = 0;
    for(Cluster<? extends MeanModel> aCluster : clustering.getAllClusters()) {
      n += aCluster.size();
    }
    return n;
  }

  /**
   * Variance contribution of a single cluster.
   * <p>
   * If possible, this information is reused from the clustering process (when a
   * KMeansModel is returned).
   *
   * @param cluster Cluster to access
   * @param distanceFunction Distance function
   * @param relation Data relation
   * @return Cluster variance
   */
  public static double varianceContributionOfCluster(Cluster<? extends MeanModel> cluster, NumberVectorDistance<?> distanceFunction, Relation<? extends NumberVector> relation) {
    if(cluster.size() <= 1) {
      return 0.;
    }
    MeanModel model = cluster.getModel();
    double v = 0;
    if(model instanceof KMeansModel) {
      v = ((KMeansModel) model).getVarianceContribution();
      // Some k-means variants do not provide this value!
      if(!Double.isNaN(v)) {
        return v;
      }
    }
    // Re-compute:
    DoubleVector mean = DoubleVector.wrap(model.getMean());
    boolean squared = distanceFunction.isSquared();
    double variance = 0.;
    for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
      double dist = distanceFunction.distance(relation.get(iter), mean);
      variance += squared ? dist : (dist * dist);
    }
    return variance;
  }

  /**
   * Computes log likelihood of an entire clustering.
   * <p>
   * A version that is supposed to correct some mistakes in the X-means
   * publication, but experimentally they do not make much of a difference.
   *
   * @param relation Data relation
   * @param clustering Clustering
   * @param distanceFunction Distance function
   * @return Log Likelihood.
   */
  @Reference(authors = "A. Foglia, B. Hancock", //
      title = "Notes on Bayesian Information Criterion Calculation for X-Means Clustering", //
      booktitle = "Online", //
      url = "https://github.com/bobhancock/goxmeans/blob/master/doc/BIC_notes.pdf", //
      bibkey = "")
  public static double logLikelihood(Relation<? extends NumberVector> relation, Clustering<? extends MeanModel> clustering, NumberVectorDistance<?> distanceFunction) {
    List<? extends Cluster<? extends MeanModel>> clusters = clustering.getAllClusters();
    // number of clusters
    final int m = clusters.size();

    // number of objects in the clustering
    int n = 0;
    // cluster sizes
    int[] n_i = new int[m];
    // total variance
    double d = 0.;

    // Iterate over clusters:
    Iterator<? extends Cluster<? extends MeanModel>> it = clusters.iterator();
    for(int i = 0; it.hasNext(); ++i) {
      Cluster<? extends MeanModel> cluster = it.next();
      n += n_i[i] = cluster.size();
      d += varianceContributionOfCluster(cluster, distanceFunction, relation);
    }

    // No remaining variance, if every point is on its own:
    if(n <= m) {
      return Double.NEGATIVE_INFINITY;
    }

    final int dim = RelationUtil.dimensionality(relation);
    // Total variance (corrected for bias)
    final double logv = FastMath.log(d > 0 ? d / ((n - m) * dim) : Double.MIN_NORMAL);

    // log likelihood of this clustering
    double logLikelihood = 0.;
    // Aggregate
    for(int i = 0; i < m; i++) {
      logLikelihood += n_i[i] * FastMath.log(n_i[i]);
    }
    logLikelihood -= n * FastMath.log(n) //
        + n * dim * .5 * (MathUtil.LOGTWOPI + logv) //
        + (n - m) * dim * .5;
    return logLikelihood;
  }

  /**
   * Compute the number of free parameters.
   *
   * @param relation Data relation (for dimensionality)
   * @param clustering Set of clusters
   * @return Number of free parameters
   */
  public static int numberOfFreeParameters(Relation<? extends NumberVector> relation, Clustering<? extends MeanModel> clustering) {
    // number of clusters
    int m = clustering.getAllClusters().size(); // num_ctrs

    // dimensionality of data points
    int dim = RelationUtil.dimensionality(relation); // num_dims

    // number of free parameters: m-1 class probabilities, m*dim centroids,
    // 1 variance estimate (c.f., XMeans paper).
    return (m + 1) * dim;
  }
}
