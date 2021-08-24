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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.kmeans.initialization.Predefined;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.database.ids.DBIDIter;
import elki.database.relation.ProxyView;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
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
 * 
 * <p>
 * Reference:
 * <p>
 * Greg Hamerly and Charles Elkan<br>
 * Learning the K in K-Means<br>
 * Advances in Neural Information Processing Systems 17 (NIPS 2004)
 * <p>
 * Lorentz Jäntschi and Sorana D. Bolboacă<br>
 * Computation of Probability Associated with Anderson–Darling Statistic<br>
 * Mathematics (MDPI, 2018) <br>
 * 
 * @author Robert Gehde
 *
 * @param <V> Vector
 * @param <M> Model
 */
@Reference(authors = "Greg Hamerly and Charles Elkan", //
    booktitle = "Advances in Neural Information Processing Systems 17 (NIPS 2004)", //
    title = "Learning the k in k-means", //
    url = "https://www.researchgate.net/publication/2869155_Learning_the_K_in_K-Means")
@Reference(authors = "Jäntschi, Lorentz and Bolboacă, Sorana D.", //
    booktitle = "Mathematics", //
    title = "Computation of Probability Associated with Anderson–Darling Statistic", //
    url = "https://www.mdpi.com/2227-7390/6/6/88")
public class GMeans<V extends NumberVector, M extends MeanModel> extends XMeans<V, M> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(GMeans.class);

  /**
   * Random object.
   */
  Random rand;

  /**
   * Significance level.
   */
  double alpha;

  public GMeans(NumberVectorDistance<? super V> distance, double alpha, int k_min, int k_max, int maxiter, KMeans<V, M> innerKMeans, KMeansInitialization initializer, RandomFactory random) {
    super(distance, k_min, k_max, maxiter, innerKMeans, initializer, null, GMeans.class.getName(), random);
    this.alpha = alpha;
    this.rand = this.rnd.getRandom();
  }

  @Override
  protected List<Cluster<M>> splitCluster(Cluster<M> parentCluster, Relation<V> relation) {
    // Transform parent cluster into a clustering
    ArrayList<Cluster<M>> parentClusterList = new ArrayList<>(1);
    parentClusterList.add(parentCluster);
    if(parentCluster.size() <= 1) {
      // Split is not possbile
      return parentClusterList;
    }
    // splitting
    ProxyView<V> parentview = new ProxyView<V>(parentCluster.getIDs(), relation);
    int dim = relation.get(relation.iterDBIDs()).getDimensionality();
    int n = parentCluster.size();
    // calculate new centers
    // 0: get points vectors
    double[][] points = new double[n][];
    int c = 0;
    for(DBIDIter it = parentview.iterDBIDs(); it.valid(); it.advance()) {
      points[c++] = relation.get(it).toArray();
    }
    // 1: calc old center c
    double[] center = new double[dim];
    for(int i = 0; i < points.length; i++) {
      for(int j = 0; j < center.length; j++) {
        center[j] += points[i][j];
      }
    }
    for(int j = 0; j < center.length; j++) {
      center[j] /= n;
    }
    // 2: calculate eigenvector
    // 2.1: calc cov
    for(int i = 0; i < points.length; i++) {
      points[i] = VMath.minusEquals(points[i], center);
    }
    double[][] cov = VMath.timesEquals(VMath.transposeTimes(points, points), 1.0 / (n - (1.0)));
    // 2.2: main principal component via power method
    double[] s = new double[dim];
    for(int i = 0; i < s.length; i++) {
      s[i] = rand.nextDouble();
    }
    VMath.normalize(s);
    for(int i = 0; i < 100; i++) {
      s = VMath.times(cov, s);
      s = VMath.normalize(s);
    }
    // 2.3: Eigenvalue
    double l = VMath.transposeTimesTimes(s, cov, s);
    // 3: deviation is m = s * sqrt(2l/pi)
    double[] m = VMath.times(s, Math.sqrt(2 * l / Math.PI));
    // 4: new centers are c +/- m
    double[][] newCenters = new double[2][dim];
    newCenters[0] = VMath.plus(center, m);
    newCenters[1] = VMath.minus(center, m);
    Predefined init = new Predefined(newCenters);

    // run it a bit
    innerKMeans.setK(2);
    innerKMeans.setInitializer(init);
    // ich würde das gerne nur 1mal laufen lassen....
    Clustering<M> childClustering = innerKMeans.run(parentview);
    c = 0;
    for(It<Cluster<M>> it = childClustering.iterToplevelClusters(); it.valid(); it.advance()) {
      newCenters[c++] = it.get().getModel().getMean();
    }
    // evaluation
    // v = c2 - c1 = 2m
    double[] v = VMath.minus(newCenters[1], newCenters[0]);
    double length = VMath.euclideanLength(v);
    double[] projectedValues = new double[n];
    for(int i = 0; i < projectedValues.length; i++) {
      projectedValues[i] = VMath.dot(points[i], v) / length;
    }
    // transform data to center 0 and var 1
    normalize(projectedValues, n);
    // test
    Arrays.sort(projectedValues);
    double A2 = AndersonDarlingTest.A2StandardNormal(projectedValues);
    A2 = AndersonDarlingTest.removeBiasNormalDistribution(A2, n);
    double pValue = pValueAdjA2(A2);
    if(LOG.isDebugging()) {
      LOG.debug("AndersonDarlingValue: " + A2);
      LOG.debug("p-value: " + pValue);
    }
    // Check if split is an improvement:
    return pValue > alpha ? parentClusterList : childClustering.getAllClusters();
  }

  /**
   * normalizes the values such that mean is 0 and variance is 1
   */
  private void normalize(double[] data, int n) {
    double mean = 0;
    for(int i = 0; i < data.length; i++) {
      mean += data[i];
    }
    mean /= n;
    double sig = 0;
    for(int i = 0; i < data.length; i++) {
      sig += Math.pow(data[i] - mean, 2);
    }
    sig = Math.sqrt(1.0 / (n - 1.0) * sig);
    for(int i = 0; i < data.length; i++) {
      data[i] = (data[i] - mean) / sig;
    }
  }

  /**
   * calculate p-value for adjusted Anderson Darling test and case 3
   * 
   * @param A2
   * @return
   */
  private double pValueAdjA2(double A2) {
    if(A2 >= 0.6) {
      return Math.exp(1.2937 - 5.709 * A2 + 0.0186 * A2 * A2);
    }
    else if(A2 >= 0.34) {
      return Math.exp(0.9177 - 4.279 * A2 - 1.38 * A2 * A2);
    }
    else if(A2 >= 0.2) {
      return 1 - Math.exp(-8.318 + 42.796 * A2 - 59.938 * A2 * A2);
    }
    else {
      return 1 - Math.exp(-13.436 - 101.14 * A2 + 223.73 * A2 * A2);
    }
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
   * @hidden
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
      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, 0.0001) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
      alphaP.grab(config, x -> alpha = x);
    }

    @Override
    public GMeans<V, M> make() {
      return new GMeans<>(distance, alpha, k_min, k_max, maxiter, innerKMeans, initializer, random);
    }
  }
}
