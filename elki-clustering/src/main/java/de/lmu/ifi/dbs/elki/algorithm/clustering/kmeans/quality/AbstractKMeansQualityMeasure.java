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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.quality;

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Base class for evaluating clusterings by information criteria (such as AIC or
 * BIC). Provides helper functions (e.g. max likelihood calculation) to its
 * subclasses.
 * <p>
 * References:
 * <p>
 * The use of information-theoretic criteria for evaluating k-means was
 * popularized by X-means:
 * <p>
 * D. Pelleg, A. Moore<br>
 * X-means: Extending K-means with Efficient Estimation on the Number of
 * Clusters<br>
 * Proc. 17th Int. Conf. on Machine Learning (ICML 2000)
 * <p>
 * A different version of logLikelihood is derived in:
 * <p>
 * Q. Zhao, M. Xu, P. Fränti<br>
 * Knee Point Detection on Bayesian Information Criterion<br>
 * 20th IEEE International Conference on Tools with Artificial Intelligence
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
   * @param <V> Vector type
   * @return Cluster variance
   */
  public static <V extends NumberVector> double varianceOfCluster(Cluster<? extends MeanModel> cluster, NumberVectorDistanceFunction<? super V> distanceFunction, Relation<V> relation) {
    MeanModel model = cluster.getModel();
    if(model instanceof KMeansModel) {
      return ((KMeansModel) model).getVarianceContribution();
    }
    // Re-compute:
    DBIDs ids = cluster.getIDs();
    DoubleVector mean = DoubleVector.wrap(model.getMean());

    boolean squared = distanceFunction.isSquared();
    double variance = 0.;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double dist = distanceFunction.distance(relation.get(iter), mean);
      variance += squared ? dist : dist * dist;
    }
    return variance;
  }

  /**
   * Computes log likelihood of an entire clustering.
   * <p>
   * Version as used in the X-means publication.
   *
   * @param relation Data relation
   * @param clustering Clustering
   * @param distanceFunction Distance function
   * @param <V> Vector type
   * @return Log Likelihood.
   */
  @Reference(authors = "D. Pelleg, A. Moore", //
      title = "X-means: Extending K-means with Efficient Estimation on the Number of Clusters", //
      booktitle = "Proc. 17th Int. Conf. on Machine Learning (ICML 2000)", //
      url = "http://www.pelleg.org/shared/hp/download/xmeans.ps", //
      bibkey = "DBLP:conf/icml/PellegM00")
  public static <V extends NumberVector> double logLikelihood(Relation<V> relation, Clustering<? extends MeanModel> clustering, NumberVectorDistanceFunction<? super V> distanceFunction) {
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
      d += d_i[i] = varianceOfCluster(cluster, distanceFunction, relation);
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
          - (d_i[i] - m) * .5; // (Rn-K)/2
    }
    logLikelihood -= n * FastMath.log(n); // Prior entropy, sum_i Rn log R
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

    // number of free parameters
    return (m - 1) + m * dim + m;
  }
}
