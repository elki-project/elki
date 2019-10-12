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
 * Different version of the BIC criterion.
 * <p>
 * Reference:
 * <p>
 * Q. Zhao, M. Xu, P. Fränti<br>
 * Knee Point Detection on Bayesian Information Criterion<br>
 * 20th IEEE International Conference on Tools with Artificial Intelligence
 *
 * @author Tibor Goldschwendt
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "Q. Zhao, M. Xu, P. Fränti", //
    title = "Knee Point Detection on Bayesian Information Criterion", //
    booktitle = "20th IEEE International Conference on Tools with Artificial Intelligence", //
    url = "https://doi.org/10.1109/ICTAI.2008.154", //
    bibkey = "DBLP:conf/ictai/ZhaoXF08")
public class BayesianInformationCriterionZhao extends AbstractKMeansQualityMeasure<NumberVector> {
  @Override
  public <V extends NumberVector> double quality(Clustering<? extends MeanModel> clustering, NumberVectorDistance<? super V> distance, Relation<V> relation) {
    final int dim = RelationUtil.dimensionality(relation);
    return logLikelihoodZhao(relation, clustering, distance) //
        - (.5 * clustering.getAllClusters().size()) * FastMath.log(numPoints(clustering)) * (dim + 1);
  }

  /**
   * Computes log likelihood of an entire clustering.
   * <p>
   * Version as used by Zhao et al.
   *
   * @param relation Data relation
   * @param clustering Clustering
   * @param distance Distance function
   * @return Log Likelihood.
   */
  public static double logLikelihoodZhao(Relation<? extends NumberVector> relation, Clustering<? extends MeanModel> clustering, NumberVectorDistance<?> distance) {
    List<? extends Cluster<? extends MeanModel>> clusters = clustering.getAllClusters();
    // number of clusters
    final int m = clusters.size();

    // number of objects in the clustering
    int n = 0;
    // cluster sizes
    int[] n_i = new int[m];
    // variances
    double[] d_i = new double[m];

    // Iterate over clusters:
    Iterator<? extends Cluster<? extends MeanModel>> it = clusters.iterator();
    for(int i = 0; it.hasNext(); ++i) {
      Cluster<? extends MeanModel> cluster = it.next();
      n += n_i[i] = cluster.size();
      // Note: the paper used 1/(n-m) but that is probably a typo
      // as it will cause divisions by zero.
      d_i[i] = varianceContributionOfCluster(cluster, distance, relation) / (double) n_i[i];
    }

    final int dim = RelationUtil.dimensionality(relation);

    // log likelihood of this clustering
    double logLikelihood = 0.;
    // Aggregate
    for(int i = 0; i < m; i++) {
      logLikelihood += n_i[i] * FastMath.log(n_i[i] / (double) n) // ni log ni/n
          - n_i[i] * dim * .5 * MathUtil.LOGTWOPI // ni*d/2 log2pi
          - n_i[i] * .5 * FastMath.log(d_i[i]) // ni/2 log sigma_i
          - (n_i[i] - m) * .5; // (ni-m)/2
    }
    return logLikelihood;
  }

  @Override
  public boolean isBetter(double currentCost, double bestCost) {
    // Careful: bestCost may be NaN!
    return !(currentCost <= bestCost);
  }
}
