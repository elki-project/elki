/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.kmeans.initialization.Predefined;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.database.ids.DBIDIter;
import elki.database.relation.ProxyView;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.math.MathUtil;
import elki.math.linearalgebra.CovarianceMatrix;
import elki.math.linearalgebra.VMath;
import elki.math.statistics.tests.AndersonDarlingTest;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.documentation.Reference;
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
 *
 * @param <V> Vector
 * @param <M> Model
 */
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
   * Significance level.
   */
  double alpha;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param alpha
   * @param k_min
   * @param k_max
   * @param maxiter
   * @param innerKMeans
   * @param initializer
   * @param random
   */
  public GMeans(NumberVectorDistance<? super V> distance, double alpha, int k_min, int k_max, int maxiter, KMeans<V, M> innerKMeans, KMeansInitialization initializer, RandomFactory random) {
    super(distance, k_min, k_max, maxiter, innerKMeans, initializer, null, random);
    this.alpha = alpha;
  }

  @Override
  protected List<Cluster<M>> splitCluster(Cluster<M> parentCluster, Relation<V> relation) {
    // Transform parent cluster into a clustering
    ArrayList<Cluster<M>> parentClusterList = new ArrayList<>(1);
    parentClusterList.add(parentCluster);
    if(parentCluster.size() <= 1) {
      // Split is not possible
      return parentClusterList;
    }
    // splitting
    final int dim = RelationUtil.dimensionality(relation);
    final int n = parentCluster.size();
    // New covariance matrix and center:
    CovarianceMatrix cov = CovarianceMatrix.make(relation, parentCluster.getIDs());
    double[][] mat = cov.destroyToSampleMatrix();
    // Find principal component via power iteration method
    double[] s = normalizeEquals(MathUtil.randomDoubleArray(dim, this.rnd.getSingleThreadedRandom()));
    for(int i = 0; i < 30; i++) {
      s = normalizeEquals(times(mat, s));
    }
    // 2.3: Eigenvalue
    double l = transposeTimesTimes(s, mat, s);
    // 3: deviation is m = s * sqrt(2l/pi)
    double[] m = times(s, Math.sqrt(2 * l / Math.PI));
    // 4: new centers are c +/- m
    double[][] newCenters = new double[][] { plus(cov.getMeanVector(), m), minus(cov.getMeanVector(), m) };
    Predefined init = new Predefined(newCenters);

    // run it a bit
    innerKMeans.setK(2);
    innerKMeans.setInitializer(init);
    Clustering<M> childClustering = innerKMeans.run(new ProxyView<>(parentCluster.getIDs(), relation));
    int nc = 0;
    for(It<Cluster<M>> it = childClustering.iterToplevelClusters(); it.valid(); it.advance()) {
      if(it.get().size() > 0) {
        newCenters[nc++] = it.get().getModel().getMean();
      }
    }
    if(nc < 2) {
      return parentClusterList; // one cluster empty.
    }
    // Evaluation via v = c2 - c1 = 2m
    double[] v = VMath.minus(newCenters[1], newCenters[0]);
    // double ilength = 1. / euclideanLength(v);
    double[] projectedValues = new double[n];
    int i = 0;
    for(DBIDIter it = parentCluster.getIDs().iter(); it.valid(); it.advance()) {
      projectedValues[i++] = transposeTimes(relation.get(it).toArray(), v);
    }
    Arrays.sort(projectedValues);
    double A2 = AndersonDarlingTest.A2Noncentral(projectedValues);
    A2 = AndersonDarlingTest.removeBiasNormalDistribution(A2, projectedValues.length);
    double pValue = AndersonDarlingTest.pValueCase4(A2);
    if(LOG.isDebugging()) {
      LOG.debug("AndersonDarlingValue: " + A2 + " p-value: " + pValue);
    }
    // Check if split is an improvement:
    return pValue > alpha ? parentClusterList : childClustering.getAllClusters();
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
     * Significance level.
     */
    public static final OptionID ALPHA_ID = new OptionID("gmeans.alpha", "Significance level for the Anderson Darling test.");

    /**
     * Significance level.
     */
    protected double alpha;

    @Override
    protected void configureInformationCriterion(Parameterization config) {
      // GMeans doesn't need an Information Criterion
      // but the significance level for AD Tests
      new DoubleParameter(ALPHA_ID, 0.0001) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .grab(config, x -> alpha = x);
    }

    @Override
    public GMeans<V, M> make() {
      return new GMeans<>(distance, alpha, k_min, k_max, maxiter, innerKMeans, initializer, random);
    }
  }
}
