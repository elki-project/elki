package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.minusEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.plusTimesEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.timesEquals;

import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractNumberVectorDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.RandomlyChosenInitialMeans;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil.SortDBIDsBySingleDimension;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.CombinedTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
  protected boolean assignToNearestCluster(Relation<? extends V> relation, double[][] means, List<? extends ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, double[] varsum) {
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
      V fv = relation.get(iditer);
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
   * @param database the database containing the vectors
   * @return the mean vectors of the given clusters in the given database
   */
  protected double[][] means(List<? extends DBIDs> clusters, double[][] means, Relation<V> database) {
    // TODO: use Kahan summation for better numerical precision?
    double[][] newMeans = new double[k][];
    for(int i = 0; i < k; i++) {
      DBIDs list = clusters.get(i);
      if(list.size() == 0) {
        // Keep degenerated means as-is for now.
        newMeans[i] = means[i];
        continue;
      }
      DBIDIter iter = list.iter();
      // Initialize with first.
      double[] mean = database.get(iter).toArray();
      iter.advance();
      // Update with remaining instances
      for(; iter.valid(); iter.advance()) {
        NumberVector vec = database.get(iter);
        for(int j = 0; j < mean.length; j++) {
          mean[j] += vec.doubleValue(j);
        }
      }
      timesEquals(mean, 1.0 / list.size());
      newMeans[i] = mean;
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
  protected double[][] medians(List<? extends DBIDs> clusters, double[][] medians, Relation<V> database) {
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
        it.seek(QuickSelect.median(list, sorter));
        mean[d] = database.get(it).doubleValue(d);
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
  protected void incrementalUpdateMean(double[] mean, V vec, int newsize, double op) {
    if(newsize == 0) {
      return; // Keep old mean
    }
    // Note: numerically stabilized version:
    double[] delta = minusEquals(vec.toArray(), mean);
    plusTimesEquals(mean, delta, op / newsize);
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
  protected boolean macQueenIterate(Relation<V> relation, double[][] means, List<ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, double[] varsum) {
    boolean changed = false;
    Arrays.fill(varsum, 0.);

    // Raw distance function
    final NumberVectorDistanceFunction<?> df = getDistanceFunction();

    // Incremental update
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double mindist = Double.POSITIVE_INFINITY;
      V fv = relation.get(iditer);
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
  private boolean updateMeanAndAssignment(List<ModifiableDBIDs> clusters, double[][] means, int minIndex, V fv, DBIDIter iditer, WritableIntegerDataStore assignment) {
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
   * @return Total varsum
   */
  protected double logVarstat(DoubleStatistic varstat, double[] varsum) {
    if(varstat == null) {
      return Double.NaN;
    }
    double s = 0.;
    for(double v : varsum) {
      s += v;
    }
    varstat.setDouble(s);
    getLogger().statistics(varstat);
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
      IntParameter kP = new IntParameter(K_ID);
      kP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
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
      ObjectParameter<NumberVectorDistanceFunction<? super V>> distanceFunctionP = makeParameterDistanceFunction(SquaredEuclideanDistanceFunction.class, PrimitiveDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
        if(!(distanceFunction instanceof EuclideanDistanceFunction) && !(distanceFunction instanceof SquaredEuclideanDistanceFunction)) {
          getLogger().warning("k-means optimizes the sum of squares - it should be used with squared euclidean distance and may stop converging otherwise!");
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
      IntParameter maxiterP = new IntParameter(MAXITER_ID, 0);
      maxiterP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.getValue();
      }
    }

    /**
     * Get class logger.
     *
     * @return Logger
     */
    abstract protected Logging getLogger();

    @Override
    abstract protected AbstractKMeans<V, ?> makeInstance();
  }
}
