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
package elki.clustering.kmeans;

import static elki.math.linearalgebra.VMath.sum;
import static elki.math.linearalgebra.VMath.timesEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import elki.AbstractDistanceBasedAlgorithm;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.data.*;
import elki.data.model.KMeansModel;
import elki.data.model.Model;
import elki.data.type.CombinedTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.PrimitiveDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.IndefiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.math.linearalgebra.VMath;
import elki.result.Metadata;
import elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Abstract base class for k-means implementations.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @composed - - - KMeansInitialization
 *
 * @param <V> Vector type
 * @param <M> Cluster model type
 */
public abstract class AbstractKMeans<V extends NumberVector, M extends Model> extends AbstractDistanceBasedAlgorithm<NumberVectorDistance<? super V>, Clustering<M>> implements KMeans<V, M> {
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
  protected KMeansInitialization initializer;

  /**
   * Constructor.
   *
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public AbstractKMeans(NumberVectorDistance<? super V> distance, int k, int maxiter, KMeansInitialization initializer) {
    super(distance);
    this.k = k;
    this.maxiter = maxiter > 0 ? maxiter : Integer.MAX_VALUE;
    this.initializer = initializer;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(new CombinedTypeInformation(TypeUtil.NUMBER_VECTOR_FIELD, getDistance().getInputTypeRestriction()));
  }

  /**
   * Choose the initial means.
   *
   * @param database Database
   * @param relation Relation
   * @return Means
   */
  protected double[][] initialMeans(Database database, Relation<V> relation) {
    Duration inittime = getLogger().newDuration(initializer.getClass() + ".time").begin();
    double[][] means = initializer.chooseInitialMeans(relation, k, getDistance());
    getLogger().statistics(inittime.end());
    return means;
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
        // TODO: allow the user to choose the behavior.
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
   * Similar to VMath.plusEquals, but for sparse number vectors.
   *
   * @param sum Aggregation array
   * @param vec Vector to add
   */
  public static void sparsePlusEquals(double[] sum, SparseNumberVector vec) {
    for(int j = vec.iter(); vec.iterValid(j); j = vec.iterAdvance(j)) {
      sum[vec.iterDim(j)] += vec.iterDoubleValue(j);
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
      double[] mean = new double[means[i].length];
      // Add remaining vectors (sparse):
      for(DBIDIter iter = list.iter(); iter.valid(); iter.advance()) {
        sparsePlusEquals(mean, relation.get(iter));
      }
      newMeans[i] = timesEquals(mean, 1. / list.size());
    }
    return newMeans;
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

  @Override
  public void setK(int k) {
    this.k = k;
  }

  @Override
  public void setDistance(NumberVectorDistance<? super V> distance) {
    this.distance = distance;
  }

  @Override
  public void setInitializer(KMeansInitialization init) {
    this.initializer = init;
  }

  /**
   * Inner instance for a run, for better encapsulation, that encapsulates the
   * standard flow of most (but not all) k-means variations.
   *
   * @author Erich Schubert
   */
  protected abstract static class Instance {
    /**
     * Cluster means.
     */
    double[][] means;

    /**
     * Store the elements per cluster.
     */
    protected List<ModifiableDBIDs> clusters;

    /**
     * A mapping of elements to cluster ids.
     */
    protected WritableIntegerDataStore assignment;

    /**
     * Sum of squared deviations in each cluster.
     */
    protected double[] varsum;

    /**
     * Data relation.
     */
    protected Relation<? extends NumberVector> relation;

    /**
     * Number of distance computations
     */
    protected long diststat = 0;

    /**
     * Distance function.
     */
    private final NumberVectorDistance<?> df;

    /**
     * Number of clusters.
     */
    protected final int k;

    /**
     * Indicates whether the distance function is squared.
     */
    protected final boolean isSquared;

    /**
     * Key for statistics logging.
     */
    protected String key;

    /**
     * Constructor.
     *
     * @param relation Relation to process
     * @param means Initial mean
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      this.relation = relation;
      this.df = df;
      this.isSquared = df.isSquared();
      this.means = means;
      this.k = means.length;
      final int guessedsize = (int) (relation.size() * 2. / k);
      this.clusters = new ArrayList<>(k);
      for(int i = 0; i < k; i++) {
        this.clusters.add(DBIDUtil.newHashSet(guessedsize));
      }
      this.assignment = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
      this.varsum = new double[k];
      this.key = this.getClass().getName().replace("$Instance", "");
    }

    /**
     * Compute a distance (and count the distance computations).
     *
     * @param x First object
     * @param y Second object
     * @return Distance
     */
    protected double distance(NumberVector x, NumberVector y) {
      ++diststat;
      return df.distance(x, y);
    }

    /**
     * Compute a distance (and count the distance computations).
     *
     * @param x First object
     * @param y Second object
     * @return Distance
     */
    protected double distance(NumberVector x, double[] y) {
      ++diststat;
      if(df.getClass() == SquaredEuclideanDistance.class) {
        if(y.length != x.getDimensionality()) {
          throw new IllegalArgumentException("Objects do not have the same dimensionality.");
        }
        double v = 0;
        for(int i = 0; i < y.length; i++) {
          double d = x.doubleValue(i) - y[i];
          v += d * d;
        }
        return v;
      }
      return df.distance(x, DoubleVector.wrap(y));
    }

    /**
     * Compute a distance (and count the distance computations).
     *
     * @param x First object
     * @param y Second object
     * @return Distance
     */
    protected double distance(double[] x, double[] y) {
      ++diststat;
      if(df.getClass() == SquaredEuclideanDistance.class) {
        if(y.length != x.length) {
          throw new IllegalArgumentException("Objects do not have the same dimensionality.");
        }
        double v = 0;
        for(int i = 0; i < x.length; i++) {
          double d = x[i] - y[i];
          v += d * d;
        }
        return v;
      }
      return df.distance(DoubleVector.wrap(x), DoubleVector.wrap(y));
    }

    /**
     * Run the clustering.
     *
     * @param maxiter Maximum number of iterations
     */
    protected void run(int maxiter) {
      final Logging log = getLogger();
      IndefiniteProgress prog = log.isVerbose() ? new IndefiniteProgress("Iteration") : null;
      int iteration = 0;
      while(++iteration <= maxiter) {
        log.incrementProcessed(prog);
        int changed = iterate(iteration);
        if(changed == 0) {
          break;
        }
        if(log.isStatistics()) {
          log.statistics(new LongStatistic(key + "." + iteration + ".reassignments", Math.abs(changed)));
          final double s = sum(varsum);
          if(s > 0) {
            log.statistics(new DoubleStatistic(key + "." + iteration + ".variance-sum", s));
          }
        }
      }
      log.setCompleted(prog);
      log.statistics(new LongStatistic(key + ".iterations", iteration));
      log.statistics(new LongStatistic(key + ".distance-computations", diststat));
    }

    /**
     * Main loop function.
     *
     * @param iteration Iteration number (beginning at 1)
     * @return Number of reassigned points
     */
    protected abstract int iterate(int iteration);

    /**
     * Compute means from cluster sums by averaging.
     * 
     * @param dst Output means
     * @param sums Input sums
     */
    protected void meansFromSums(double[][] dst, double[][] sums) {
      for(int i = 0; i < k; i++) {
        VMath.overwriteTimes(dst[i], sums[i], 1. / clusters.get(i).size());
      }
    }

    /**
     * Copy means
     *
     * @param src Source values
     * @param dst Destination values
     */
    protected void copyMeans(double[][] src, double[][] dst) {
      for(int i = 0; i < k; i++) {
        System.arraycopy(src[i], 0, dst[i], 0, src[i].length);
      }
    }

    /**
     * Assign each object to the nearest cluster.
     *
     * @return number of objects reassigned
     */
    protected int assignToNearestCluster() {
      assert (k == means.length);
      int changed = 0;
      // Reset all clusters
      Arrays.fill(varsum, 0.);
      for(ModifiableDBIDs cluster : clusters) {
        cluster.clear();
      }
      boolean issquared = isSquared();
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        double mindist = Double.POSITIVE_INFINITY;
        NumberVector fv = relation.get(iditer);
        int minIndex = 0;
        for(int i = 0; i < k; i++) {
          double dist = distance(fv, means[i]);
          if(dist < mindist) {
            minIndex = i;
            mindist = dist;
          }
        }
        varsum[minIndex] += issquared ? mindist : (mindist * mindist);
        clusters.get(minIndex).add(iditer);
        if(assignment.putInt(iditer, minIndex) != minIndex) {
          ++changed;
        }
      }
      return changed;
    }

    /**
     * Recompute the separation of cluster means.
     * <p>
     * Used by Elkan's variant and Exponion.
     *
     * @param sep Output array of separation
     * @param cdist Center-to-Center distances (half-sqrt scaled)
     */
    protected void recomputeSeperation(double[] sep, double[][] cdist) {
      final int k = means.length;
      final boolean issquared = df.isSquared();
      assert (sep.length == k);
      Arrays.fill(sep, Double.POSITIVE_INFINITY);
      for(int i = 1; i < k; i++) {
        double[] mi = means[i];
        for(int j = 0; j < i; j++) {
          double d = distance(mi, means[j]);
          d = .5 * (issquared ? FastMath.sqrt(d) : d);
          cdist[i][j] = cdist[j][i] = d;
          sep[i] = (d < sep[i]) ? d : sep[i];
          sep[j] = (d < sep[j]) ? d : sep[j];
        }
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
      assert (newmeans.length == means.length);
      assert (dists.length == means.length);
      boolean issquared = df.isSquared();
      double max = 0.;
      for(int i = 0; i < means.length; i++) {
        double d = distance(means[i], newmeans[i]);
        dists[i] = d = issquared ? FastMath.sqrt(d) : d;
        max = (d > max) ? d : max;
      }
      return max;
    }

    /**
     * Build a standard k-means result, with known cluster variance sums.
     *
     * @return Clustering result
     */
    protected Clustering<KMeansModel> buildResult() {
      Clustering<KMeansModel> result = new Clustering<>();
      Metadata.of(result).setLongName("k-Means Clustering");
      for(int i = 0; i < clusters.size(); i++) {
        DBIDs ids = clusters.get(i);
        if(ids.isEmpty()) {
          getLogger().warning("K-Means produced an empty cluster - bad initialization?");
        }
        result.addToplevelCluster(new Cluster<>(ids, new KMeansModel(means[i], varsum[i])));
      }
      return result;
    }

    /**
     * Build the result, recomputing the cluster variance if {@code varstat} is
     * set to true.
     * 
     * @param varstat Recompute cluster variance
     * @param relation Data relation (only needed if varstat is set)
     * @return Clustering result
     */
    protected Clustering<KMeansModel> buildResult(boolean varstat, Relation<? extends NumberVector> relation) {
      Clustering<KMeansModel> result = new Clustering<>();
      Metadata.of(result).setLongName("k-Means Clustering");
      if(relation.size() <= 0) {
        return result;
      }
      if(!varstat) {
        for(int i = 0; i < clusters.size(); i++) {
          DBIDs ids = clusters.get(i);
          if(ids.isEmpty()) {
            continue;
          }
          result.addToplevelCluster(new Cluster<>(ids, new KMeansModel(means[i], Double.NaN)));
        }
      }
      else {
        double totalvariance = 0.;
        for(int i = 0; i < clusters.size(); i++) {
          DBIDs ids = clusters.get(i);
          if(ids.isEmpty()) {
            continue;
          }
          double varsum = 0;
          double[] mean = means[i];
          for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
            varsum += distance(relation.get(it), mean);
          }
          totalvariance += varsum;
          result.addToplevelCluster(new Cluster<>(ids, new KMeansModel(mean, varsum)));
        }
        Logging log = getLogger();
        if(log.isStatistics()) {
          log.statistics(new DoubleStatistic(key + ".variance-sum", totalvariance));
          log.statistics(new LongStatistic(key + ".distance-computations", diststat));
        }
      }
      return result;
    }

    /**
     * Get if the distance function is squared.
     *
     * @return {@code true} when squared
     */
    protected boolean isSquared() {
      return df.isSquared();
    }

    /**
     * Get the class logger.
     *
     * @return Logger
     */
    abstract Logging getLogger();
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public abstract static class Par<V extends NumberVector> extends AbstractDistanceBasedAlgorithm.Par<NumberVectorDistance<? super V>> {
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
    protected KMeansInitialization initializer;

    /**
     * Compute the final variance statistic (not used by all).
     */
    protected boolean varstat = false;

    @Override
    public void configure(Parameterization config) {
      getParameterK(config);
      getParameterInitialization(config);
      getParameterDistance(config);
      getParameterMaxIter(config);
    }

    /**
     * Get the k parameter.
     *
     * @param config Parameterization
     */
    protected void getParameterK(Parameterization config) {
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
    }

    /**
     * Get the distance function parameter.
     *
     * @param config Parameterization
     */
    protected void getParameterDistance(Parameterization config) {
      new ObjectParameter<NumberVectorDistance<? super V>>(DISTANCE_FUNCTION_ID, PrimitiveDistance.class, SquaredEuclideanDistance.class) //
          .grab(config, x -> {
            this.distance = x;
            if(x instanceof SquaredEuclideanDistance || x instanceof EuclideanDistance) {
              return;
            }
            else if(needsMetric() && !x.isMetric()) {
              Logging.getLogger(this.getClass()).warning("This k-means variants requires the triangle inequality, and thus should only be used with squared Euclidean distance!");
            }
            else {
              Logging.getLogger(this.getClass()).warning("k-means optimizes the sum of squares - it should be used with squared euclidean distance and may stop converging otherwise!");
            }
          });
    }

    /**
     * Users could use other non-metric distances at their own risk; but some
     * k-means variants make explicit use of the triangle inequality, we emit
     * extra warnings then.
     *
     * @return {@code true} if the algorithm uses triangle inequality
     */
    protected boolean needsMetric() {
      return false;
    }

    /**
     * Get the initialization method parameter.
     *
     * @param config Parameterization
     */
    protected void getParameterInitialization(Parameterization config) {
      new ObjectParameter<KMeansInitialization>(INIT_ID, KMeansInitialization.class, RandomlyChosen.class) //
          .grab(config, x -> initializer = x);
    }

    /**
     * Get the max iterations parameter.
     *
     * @param config Parameterization
     */
    protected void getParameterMaxIter(Parameterization config) {
      new IntParameter(MAXITER_ID, 0)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> maxiter = x);
    }

    /**
     * Get the variance statistics parameter.
     * 
     * @param config Parameterization
     */
    protected void getParameterVarstat(Parameterization config) {
      new Flag(VARSTAT_ID).grab(config, x -> varstat = x);
    }

    @Override
    abstract public AbstractKMeans<V, ?> make();
  }
}
