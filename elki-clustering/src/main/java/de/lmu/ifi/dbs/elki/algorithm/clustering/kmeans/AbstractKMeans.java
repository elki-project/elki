/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
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

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.sum;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.timesEquals;

import java.util.Arrays;
import java.util.List;

import com.sun.istack.internal.logging.Logger;

import de.lmu.ifi.dbs.elki.algorithm.AbstractNumberVectorDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.RandomlyChosenInitialMeans;
import de.lmu.ifi.dbs.elki.data.*;
import de.lmu.ifi.dbs.elki.data.VectorUtil.SortDBIDsBySingleDimension;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.CombinedTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Abstract base class for k-means implementations.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @apiviz.composedOf KMeansInitialization
 *
 * @param <V> Vector type
 * @param <M> Cluster model type
 */
public abstract class AbstractKMeans<V extends NumberVector, M extends Model> extends AbstractNumberVectorDistanceBasedAlgorithm<V, Clustering<M>> implements KMeans<V, M>, ClusteringAlgorithm<Clustering<M>> {
  /**
   * Number of cluster centers to initialize.
   */
  protected int k;

  /**
   * Maximum number of iterations
   */
  protected int maxiter;

  /**
   * Method to choose initial means.
   */
  protected KMeansInitialization<? super V> initializer;

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public AbstractKMeans(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization<? super V> initializer) {
    super(distanceFunction);
    this.k = k;
    this.maxiter = maxiter;
    this.initializer = initializer;
  }

  /**
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
   *
   * @param relation the database to cluster
   * @param means a list of k means
   * @param clusters cluster assignment
   * @param assignment Current cluster assignment
   * @param varsum Variance sum output
   * @return true when the object was reassigned
   */
  protected boolean assignToNearestCluster(Relation<? extends NumberVector> relation, double[][] means, List<? extends ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, double[] varsum) {
    assert (k == means.length);
    boolean changed = false;
    // Reset all clusters
    Arrays.fill(varsum, 0.);
    for(ModifiableDBIDs cluster : clusters) {
      cluster.clear();
    }
    final NumberVectorDistanceFunction<?> df = getDistanceFunction();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double mindist = Double.POSITIVE_INFINITY;
      NumberVector fv = relation.get(iditer);
      int minIndex = 0;
      for(int i = 0; i < k; i++) {
        double dist = df.distance(fv, DoubleVector.wrap(means[i]));
        if(dist < mindist) {
          minIndex = i;
          mindist = dist;
        }
      }
      varsum[minIndex] += mindist;
      clusters.get(minIndex).add(iditer);
      changed |= assignment.putInt(iditer, minIndex) != minIndex;
    }
    return changed;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(new CombinedTypeInformation(TypeUtil.NUMBER_VECTOR_FIELD, getDistanceFunction().getInputTypeRestriction()));
  }

  /**
   * Returns the mean vectors of the given clusters in the given database.
   *
   * @param clusters the clusters to compute the means
   * @param means the recent means
   * @param relation the database containing the vectors
   * @return the mean vectors of the given clusters in the given database
   */
  protected static double[][] means(List<? extends DBIDs> clusters, double[][] means, Relation<? extends NumberVector> relation) {
    if(TypeUtil.SPARSE_VECTOR_FIELD.isAssignableFromType(relation.getDataTypeInformation())) {
      @SuppressWarnings("unchecked")
      Relation<? extends SparseNumberVector> sparse = (Relation<? extends SparseNumberVector>) relation;
      return sparseMeans(clusters, means, sparse);
    }
    return denseMeans(clusters, means, relation);
  }

  /**
   * Returns the mean vectors of the given clusters in the given database.
   *
   * @param clusters the clusters to compute the means
   * @param means the recent means
   * @param relation the database containing the vectors
   * @return the mean vectors of the given clusters in the given database
   */
  private static double[][] denseMeans(List<? extends DBIDs> clusters, double[][] means, Relation<? extends NumberVector> relation) {
    final int k = means.length;
    double[][] newMeans = new double[k][];
    for(int i = 0; i < newMeans.length; i++) {
      DBIDs list = clusters.get(i);
      if(list.isEmpty()) {
        // Keep degenerated means as-is for now.
        newMeans[i] = means[i];
        continue;
      }
      DBIDIter iter = list.iter();
      // Initialize with first.
      double[] sum = relation.get(iter).toArray();
      // Update with remaining instances
      for(iter.advance(); iter.valid(); iter.advance()) {
        plusEquals(sum, relation.get(iter));
      }
      newMeans[i] = timesEquals(sum, 1.0 / list.size());
    }
    return newMeans;
  }

  /**
   * Similar to VMath.plusEquals, but accepts a number vector.
   *
   * @param sum Aggregation array
   * @param vec Vector to add
   */
  public static void plusEquals(double[] sum, NumberVector vec) {
    for(int d = 0; d < sum.length; d++) {
      sum[d] += vec.doubleValue(d);
    }
  }

  /**
   * Similar to VMath.minusEquals, but accepts a number vector.
   *
   * @param sum Aggregation array
   * @param vec Vector to subtract
   */
  public static void minusEquals(double[] sum, NumberVector vec) {
    for(int d = 0; d < sum.length; d++) {
      sum[d] -= vec.doubleValue(d);
    }
  }

  /**
   * Add to one, remove from another.
   *
   * @param add Array to add to
   * @param sub Array to remove from
   * @param vec Vector to subtract
   */
  public static void plusMinusEquals(double[] add, double[] sub, NumberVector vec) {
    for(int d = 0; d < add.length; d++) {
      final double v = vec.doubleValue(d);
      add[d] += v;
      sub[d] -= v;
    }
  }

  /**
   * Returns the mean vectors of the given clusters in the given database.
   *
   * @param clusters the clusters to compute the means
   * @param means the recent means
   * @param relation the database containing the vectors
   * @return the mean vectors of the given clusters in the given database
   */
  private static double[][] sparseMeans(List<? extends DBIDs> clusters, double[][] means, Relation<? extends SparseNumberVector> relation) {
    final int k = means.length;
    double[][] newMeans = new double[k][];
    for(int i = 0; i < k; i++) {
      DBIDs list = clusters.get(i);
      if(list.isEmpty()) {
        // Keep degenerated means as-is for now.
        newMeans[i] = means[i];
        continue;
      }
      DBIDIter iter = list.iter();
      // Initialize with first.
      double[] mean = relation.get(iter).toArray();
      // Update with remaining instances
      for(iter.advance(); iter.valid(); iter.advance()) {
        SparseNumberVector vec = relation.get(iter);
        for(int j = vec.iter(); vec.iterValid(j); j = vec.iterAdvance(j)) {
          mean[vec.iterDim(j)] += vec.iterDoubleValue(j);
        }
      }
      newMeans[i] = timesEquals(mean, 1.0 / list.size());
    }
    return newMeans;
  }

  /**
   * Returns the median vectors of the given clusters in the given database.
   *
   * @param clusters the clusters to compute the means
   * @param medians the recent medians
   * @param database the database containing the vectors
   * @return the mean vectors of the given clusters in the given database
   */
  protected double[][] medians(List<? extends DBIDs> clusters, double[][] medians, Relation<? extends NumberVector> database) {
    final int dim = medians[0].length;
    final SortDBIDsBySingleDimension sorter = new SortDBIDsBySingleDimension(database);
    double[][] newMedians = new double[k][];
    for(int i = 0; i < k; i++) {
      DBIDs clu = clusters.get(i);
      if(clu.size() <= 0) {
        newMedians[i] = medians[i];
        continue;
      }
      ArrayModifiableDBIDs list = DBIDUtil.newArray(clu);
      DBIDArrayIter it = list.iter();
      double[] mean = new double[dim];
      for(int d = 0; d < dim; d++) {
        sorter.setDimension(d);
        mean[d] = database.get(it.seek(QuickSelectDBIDs.median(list, sorter))).doubleValue(d);
      }
      newMedians[i] = mean;
    }
    return newMedians;
  }

  /**
   * Compute an incremental update for the mean.
   *
   * @param mean Mean to update
   * @param vec Object vector
   * @param newsize (New) size of cluster
   * @param op Cluster size change / Weight change
   */
  protected static void incrementalUpdateMean(double[] mean, NumberVector vec, int newsize, double op) {
    if(newsize == 0) {
      return; // Keep old mean
    }
    // Note: numerically stabilized version:
    VMath.plusTimesEquals(mean, VMath.minusEquals(vec.toArray(), mean), op / newsize);
  }

  /**
   * Perform a MacQueen style iteration.
   *
   * @param relation Relation
   * @param means Means
   * @param clusters Clusters
   * @param assignment Current cluster assignment
   * @param varsum Variance sum output
   * @return true when the means have changed
   */
  protected boolean macQueenIterate(Relation<? extends NumberVector> relation, double[][] means, List<ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, double[] varsum) {
    boolean changed = false;
    Arrays.fill(varsum, 0.);

    // Raw distance function
    final NumberVectorDistanceFunction<?> df = getDistanceFunction();

    // Incremental update
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double mindist = Double.POSITIVE_INFINITY;
      NumberVector fv = relation.get(iditer);
      int minIndex = 0;
      for(int i = 0; i < k; i++) {
        double dist = df.distance(fv, DoubleVector.wrap(means[i]));
        if(dist < mindist) {
          minIndex = i;
          mindist = dist;
        }
      }
      varsum[minIndex] += mindist;
      changed |= updateMeanAndAssignment(clusters, means, minIndex, fv, iditer, assignment);
    }
    return changed;
  }

  /**
   * Try to update the cluster assignment.
   *
   * @param clusters Current clusters
   * @param means Means to update
   * @param minIndex Cluster to assign to
   * @param fv Vector
   * @param iditer Object ID
   * @param assignment Current cluster assignment
   * @return {@code true} when assignment changed
   */
  private boolean updateMeanAndAssignment(List<ModifiableDBIDs> clusters, double[][] means, int minIndex, NumberVector fv, DBIDIter iditer, WritableIntegerDataStore assignment) {
    int cur = assignment.intValue(iditer);
    if(cur == minIndex) {
      return false;
    }
    final ModifiableDBIDs curclus = clusters.get(minIndex);
    curclus.add(iditer);
    incrementalUpdateMean(means[minIndex], fv, curclus.size(), +1);

    if(cur >= 0) {
      ModifiableDBIDs ci = clusters.get(cur);
      ci.remove(iditer);
      incrementalUpdateMean(means[cur], fv, ci.size() + 1, -1);
    }

    assignment.putInt(iditer, minIndex);
    return true;
  }

  /**
   * Recompute the separation of cluster means.
   * <p>
   * Used by Hamerly.
   *
   * @param means Means
   * @param sep Output array of separation (half-sqrt scaled)
   * @param diststat Distance counting statistic
   */
  protected void recomputeSeperation(double[][] means, double[] sep, LongStatistic diststat) {
    final int k = means.length;
    final boolean issquared = distanceFunction.isSquared();
    assert (sep.length == k);
    Arrays.fill(sep, Double.POSITIVE_INFINITY);
    for(int i = 1; i < k; i++) {
      DoubleVector m1 = DoubleVector.wrap(means[i]);
      for(int j = 0; j < i; j++) {
        double d = distanceFunction.distance(m1, DoubleVector.wrap(means[j]));
        sep[i] = (d < sep[i]) ? d : sep[i];
        sep[j] = (d < sep[j]) ? d : sep[j];
      }
    }
    // We need half the Euclidean distance
    for(int i = 0; i < k; i++) {
      sep[i] = .5 * (issquared ? FastMath.sqrt(sep[i]) : sep[i]);
    }
    if(diststat != null) {
      diststat.increment((k * (k - 1)) >> 1);
    }
  }

  /**
   * Recompute the separation of cluster means.
   * <p>
   * Used by Elkan's variant and Exponion.
   *
   * @param means Means
   * @param sep Output array of separation
   * @param cdist Center-to-Center distances (half-sqrt scaled)
   * @param diststat Distance counting statistic
   */
  protected void recomputeSeperation(double[][] means, double[] sep, double[][] cdist, LongStatistic diststat) {
    final int k = means.length;
    final boolean issquared = distanceFunction.isSquared();
    assert (sep.length == k);
    Arrays.fill(sep, Double.POSITIVE_INFINITY);
    for(int i = 1; i < k; i++) {
      DoubleVector mi = DoubleVector.wrap(means[i]);
      for(int j = 0; j < i; j++) {
        double d = distanceFunction.distance(mi, DoubleVector.wrap(means[j]));
        d = .5 * (issquared ? FastMath.sqrt(d) : d);
        cdist[i][j] = cdist[j][i] = d;
        sep[i] = (d < sep[i]) ? d : sep[i];
        sep[j] = (d < sep[j]) ? d : sep[j];
      }
    }
    if(diststat != null) {
      diststat.increment((k * (k - 1)) >> 1);
    }
  }

  /**
   * Recompute the separation of cluster means.
   * <p>
   * Used by Sort and Compare variants.
   *
   * @param means Means
   * @param cdist Center-to-Center distances (half-sqrt scaled)
   * @param diststat Distance counting statistic
   */
  protected void recomputeSeperation(double[][] means, double[][] cdist, LongStatistic diststat) {
    final int k = means.length;
    final boolean issquared = distanceFunction.isSquared();
    for(int i = 1; i < k; i++) {
      DoubleVector mi = DoubleVector.wrap(means[i]);
      for(int j = 0; j < i; j++) {
        double d = distanceFunction.distance(mi, DoubleVector.wrap(means[j]));
        cdist[i][j] = cdist[j][i] = .5 * (issquared ? FastMath.sqrt(d) : d);
      }
    }
    if(diststat != null) {
      diststat.increment((k * (k - 1)) >> 1);
    }
  }

  /**
   * Maximum distance moved.
   * <p>
   * Used by Hamerly, Elkan (not using the maximum).
   *
   * @param means Old means
   * @param newmeans New means
   * @param dists Distances moved (output)
   * @return Maximum distance moved
   */
  protected double movedDistance(double[][] means, double[][] newmeans, double[] dists) {
    assert (means.length == k);
    assert (newmeans.length == k);
    assert (dists.length == k);
    boolean issquared = distanceFunction.isSquared();
    double max = 0.;
    for(int i = 0; i < k; i++) {
      double d = distanceFunction.distance(DoubleVector.wrap(means[i]), DoubleVector.wrap(newmeans[i]));
      dists[i] = d = issquared ? FastMath.sqrt(d) : d;
      max = (d > max) ? d : max;
    }
    return max;
  }

  /**
   * Recompute the separation of cluster means.
   * <p>
   * Used by sort, and our exponion implementation.
   *
   * @param cdist Center-to-Center distances
   * @param cnum Center numbers
   */
  protected static void nearestMeans(double[][] cdist, int[][] cnum) {
    final int k = cdist.length;
    double[] buf = new double[k - 1];
    for(int i = 0; i < k; i++) {
      System.arraycopy(cdist[i], 0, buf, 0, i);
      System.arraycopy(cdist[i], i + 1, buf, i, k - i - 1);
      for(int j = 0; j < buf.length; j++) {
        cnum[i][j] = j < i ? j : (j + 1);
      }
      DoubleIntegerArrayQuickSort.sort(buf, cnum[i], k - 1);
    }
  }

  /**
   * Build a standard k-means result, with known cluster variance sums.
   *
   * @param clusters Cluster assignment
   * @param means Cluster means
   * @param varsum Variance sums
   * @return
   */
  protected Clustering<KMeansModel> buildResult(List<ModifiableDBIDs> clusters, double[][] means, double[] varsum) {
    Clustering<KMeansModel> result = new Clustering<>("k-Means Clustering", "kmeans-clustering");
    for(int i = 0; i < clusters.size(); i++) {
      DBIDs ids = clusters.get(i);
      if(ids.isEmpty()) {
        continue;
      }
      result.addToplevelCluster(new Cluster<>(ids, new KMeansModel(means[i], varsum[i])));
    }
    return result;
  }

  /**
   * Build the result, recomputing the cluster variance if {@code varstat} is
   * set to true.
   * 
   * @param clusters Cluster assignments
   * @param means Cluster means
   * @param varstat Recompute cluster variance
   * @param relation Data relation (only needed if varstat is set)
   * @param diststat Distance computations counter (only needed for varstat)
   * @return Clustering result
   */
  protected Clustering<KMeansModel> buildResult(List<ModifiableDBIDs> clusters, double[][] means, boolean varstat, Relation<V> relation, LongStatistic diststat) {
    double totalvariance = 0.;
    Clustering<KMeansModel> result = new Clustering<>("k-Means Clustering", "kmeans-clustering");
    for(int i = 0; i < clusters.size(); i++) {
      DBIDs ids = clusters.get(i);
      if(ids.isEmpty()) {
        continue;
      }
      double varsum = 0;
      if(varstat) {
        DoubleVector mvec = DoubleVector.wrap(means[i]);
        for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
          varsum += distanceFunction.distance(mvec, relation.get(it));
        }
        if(diststat != null) {
          diststat.increment(ids.size());
        }
        totalvariance += varsum;
      }
      result.addToplevelCluster(new Cluster<>(ids, new KMeansModel(means[i], varsum)));
    }
    if(varstat && getLogger().isStatistics()) {
      getLogger().statistics(new DoubleStatistic(this.getClass().getName() + ".variance-sum", totalvariance));
      getLogger().statistics(diststat);
    }
    return result;
  }

  @Override
  public void setK(int k) {
    this.k = k;
  }

  @Override
  public void setDistanceFunction(NumberVectorDistanceFunction<? super V> distanceFunction) {
    this.distanceFunction = distanceFunction;
  }

  @Override
  public void setInitializer(KMeansInitialization<? super V> init) {
    this.initializer = init;
  }

  /**
   * Log statistics on the variance sum.
   *
   * @param varstat Statistics log instance
   * @param varsum Variance sum per cluster
   * @return Total varsum (or {@code Double.NaN}, if {@code varstat == null})
   */
  protected double logVarstat(DoubleStatistic varstat, double[] varsum) {
    if(varstat == null) {
      return Double.NaN;
    }
    double s = sum(varsum);
    getLogger().statistics(varstat.setDouble(s));
    return s;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public abstract static class Parameterizer<V extends NumberVector> extends AbstractNumberVectorDistanceBasedAlgorithm.Parameterizer<V> {
    /**
     * k Parameter.
     */
    protected int k;

    /**
     * Maximum number of iterations.
     */
    protected int maxiter;

    /**
     * Initialization method.
     */
    protected KMeansInitialization<V> initializer;

    /**
     * Compute the final variance statistic (not used by all).
     */
    protected boolean varstat = false;

    @Override
    protected void makeOptions(Parameterization config) {
      getParameterK(config);
      getParameterInitialization(config);
      getParameterDistanceFunction(config);
      getParameterMaxIter(config);
    }

    /**
     * Get the k parameter.
     *
     * @param config Parameterization
     */
    protected void getParameterK(Parameterization config) {
      IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    /**
     * Get the distance function parameter.
     *
     * @param config Parameterization
     */
    protected void getParameterDistanceFunction(Parameterization config) {
      ObjectParameter<NumberVectorDistanceFunction<? super V>> distanceFunctionP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, PrimitiveDistanceFunction.class, SquaredEuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
        if(distanceFunction != null //
            && !(distanceFunction instanceof SquaredEuclideanDistanceFunction) //
            && !(distanceFunction instanceof EuclideanDistanceFunction)) {
          Logger.getLogger(this.getClass()).warning("k-means optimizes the sum of squares - it should be used with squared euclidean distance and may stop converging otherwise!");
        }
      }
    }

    /**
     * Get the initialization method parameter.
     *
     * @param config Parameterization
     */
    protected void getParameterInitialization(Parameterization config) {
      ObjectParameter<KMeansInitialization<V>> initialP = new ObjectParameter<>(INIT_ID, KMeansInitialization.class, RandomlyChosenInitialMeans.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }
    }

    /**
     * Get the max iterations parameter.
     *
     * @param config Parameterization
     */
    protected void getParameterMaxIter(Parameterization config) {
      IntParameter maxiterP = new IntParameter(MAXITER_ID, 0)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.getValue();
      }
    }

    /**
     * Get the variance statistics parameter.
     * 
     * @param config Parameterization
     */
    protected void getParameterVarstat(Parameterization config) {
      Flag varF = new Flag(VARSTAT_ID);
      varstat = config.grab(varF) && varF.isTrue();
    }

    @Override
    abstract protected AbstractKMeans<V, ?> makeInstance();
  }
}
