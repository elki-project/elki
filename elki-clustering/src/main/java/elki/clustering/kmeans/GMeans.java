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

import static elki.math.linearalgebra.VMath.*;

import java.util.Arrays;
import java.util.List;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Cluster;
import elki.data.NumberVector;
import elki.data.VectorUtil;
import elki.data.model.MeanModel;
import elki.database.ids.DBIDIter;
import elki.database.relation.ProxyView;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.math.MathUtil;
import elki.math.linearalgebra.CovarianceMatrix;
import elki.math.statistics.tests.AndersonDarlingTest;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.random.RandomFactory;

/**
 * G-Means extends K-Means and estimates the number of centers with Anderson
 * Darling Test.<br>
 * Implemented as specialization of XMeans.
 * <p>
 * Reference:
 * <p>
 * G. Hamerly and C. Elkan<br>
 * Learning the K in K-Means<br>
 * Neural Information Processing Systems
 * 
 * @author Robert Gehde
 * @since 0.8.0
 *
 * @param <V> Vector
 * @param <M> Model
 */
@Title("G-means")
@Reference(authors = "G. Hamerly and C. Elkan", //
    title = "Learning the k in k-means", //
    booktitle = "Neural Information Processing Systems", //
    url = "https://www.researchgate.net/publication/2869155_Learning_the_K_in_K-Means", //
    bibkey = "DBLP:conf/nips/HamerlyE03")
public class GMeans<V extends NumberVector, M extends MeanModel> extends XMeans<V, M> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(GMeans.class);

  /**
   * Critical value
   */
  protected double critical;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param critical Critical value
   * @param k_min Minimum number of clusters
   * @param k_max Maximum number of clusters
   * @param maxiter Maximum number of iterations
   * @param innerKMeans Nested k-means algorithm
   * @param initializer Initialization method
   * @param random Random generator
   */
  public GMeans(NumberVectorDistance<? super V> distance, double critical, int k_min, int k_max, int maxiter, KMeans<V, M> innerKMeans, KMeansInitialization initializer, RandomFactory random) {
    super(distance, k_min, k_max, maxiter, innerKMeans, initializer, null, random);
    this.critical = critical;
  }

  @Override
  protected List<Cluster<M>> splitCluster(Cluster<M> parentCluster, Relation<V> relation) {
    // Transform parent cluster into a clustering
    if(parentCluster.size() <= 1) {
      // Split is not possible
      return Arrays.asList(parentCluster);
    }
    splitInitializer.setInitialMeans(splitCentroid(parentCluster, relation));
    innerKMeans.setK(2);
    List<Cluster<M>> childClustering = innerKMeans.run(new ProxyView<>(parentCluster.getIDs(), relation)).getAllClusters();
    assert childClustering.size() == 2;
    // Evaluation via projection to v = c2 - c1 = 2m
    double[] v = minus(childClustering.get(0).getModel().getMean(), childClustering.get(1).getModel().getMean());
    double[] projectedValues = new double[parentCluster.size()];
    int i = 0;
    for(DBIDIter it = parentCluster.getIDs().iter(); it.valid(); it.advance()) {
      projectedValues[i++] = VectorUtil.dot(relation.get(it), v);
    }
    // Test for normality:
    Arrays.sort(projectedValues);
    double A2 = AndersonDarlingTest.A2Noncentral(projectedValues);
    A2 = AndersonDarlingTest.removeBiasNormalDistribution(A2, projectedValues.length);
    // Check if split is an improvement:
    return A2 > critical ? childClustering : Arrays.asList(parentCluster);
  }

  @Override
  protected double[][] splitCentroid(Cluster<? extends MeanModel> parentCluster, Relation<V> relation) {
    CovarianceMatrix cov = CovarianceMatrix.make(relation, parentCluster.getIDs());
    double[][] mat = cov.destroyToSampleMatrix();
    // Find principal component via power iteration method
    final int dim = RelationUtil.dimensionality(relation);
    double[] s = normalizeEquals(MathUtil.randomDoubleArray(dim, this.rnd.getSingleThreadedRandom()));
    for(int i = 0; i < 30; i++) {
      s = normalizeEquals(times(mat, s));
    }
    // 3: deviation is m = s * sqrt(2l/pi)
    timesEquals(s, Math.sqrt(2 * transposeTimesTimes(s, mat, s) / Math.PI));
    // 4: new centers are c +/- m
    return new double[][] { plus(cov.getMeanVector(), s), minus(cov.getMeanVector(), s) };
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Robert Gehde
   *
   * @param <V> Vector type
   * @param <M> Model type of inner algorithm
   */
  public static class Par<V extends NumberVector, M extends MeanModel> extends XMeans.Par<V, M> {
    /**
     * Critical value for the Anderson-Darling-Test
     */
    public static final OptionID CRITICAL_ID = new OptionID("gmeans.critical", "Critical value for the Anderson Darling test. \u03B1=0.0001 is 1.8692, \u03B1=0.005 is 1.159 \u03B1=0.01 is 1.0348");

    /**
     * Critical value
     */
    protected double critical;

    @Override
    protected void configureInformationCriterion(Parameterization config) {
      // GMeans doesn't use an Information Criterion
      // but the significance level for AD Tests
      new DoubleParameter(CRITICAL_ID, 1.8692) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> critical = x);
    }

    @Override
    public GMeans<V, M> make() {
      return new GMeans<>(distance, critical, k_min, k_max, maxiter, innerKMeans, initializer, random);
    }
  }
}
