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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListMIter;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleMinHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * k-means--: A Unified Approach to Clustering and Outlier Detection.
 *
 * Similar to Lloyds K-means algorithm, but ignores the farthest points when
 * updating the means, considering them to be outliers.
 *
 * Reference:
 * <p>
 * S. Chawla and A. Gionis<br />
 * k-means--: A Unified Approach to Clustering and Outlier Detection<br />
 * In Proc. 13th SIAM International Conference on Data Mining
 * </p>
 *
 * @author Jonas Steinke
 * @since 0.7.2
 *
 * @apiviz.landmark
 * @apiviz.has KMeansModel
 *
 * @param <V> vector datatype
 */
@Title("K-Means--")
@Reference(authors = "S. Chawla and A. Gionis", //
    title = "k-means--: A Unified Approach to Clustering and Outlier Detection", //
    booktitle = "Proc. 13th SIAM International Conference on Data Mining", //
    url = "")
public class KMeansMinusMinus<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansMinusMinus.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMeansMinusMinus.class.getName();

  /**
   * Outlier rate.
   */
  public double rate;

  /**
   * Create a noise cluster, otherwise assign to the nearest cluster.
   */
  public boolean noiseFlag;

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param noiseFlag Create a noise cluster instead of assigning to the nearest
   *        cluster
   */
  public KMeansMinusMinus(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization<? super V> initializer, double rate, boolean noiseFlag) {
    super(distanceFunction, k, maxiter, initializer);
    this.rate = rate;
    this.noiseFlag = noiseFlag;
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    if(relation.size() <= 0) {
      return new Clustering<>("k-Means Clustering", "kmeans-clustering");
    }
    // Choose initial means
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(KEY + ".initialization", initializer.toString()));
    }

    // Intialisieren der means
    double[][] means = initializer.chooseInitialMeans(database, relation, k, getDistanceFunction());

    // initialisieren vom Heap
    final int heapsize = (int) (rate < 1. ? Math.ceil(relation.size() * rate) : rate);
    DoubleMinHeap minHeap = new DoubleMinHeap(heapsize);

    // Setup cluster assignment store
    List<ModifiableDoubleDBIDList> clusters = new ArrayList<>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newDistanceDBIDList((int) (relation.size() * 2. / k)));
    }

    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
    double[] varsum = new double[k];

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Means iteration", LOG) : null;
    DoubleStatistic varstat = LOG.isStatistics() ? new DoubleStatistic(this.getClass().getName() + ".variance-sum") : null;

    int iteration = 0;
    double prevvartotal = 0.;
    for(; maxiter <= 0 || iteration < maxiter; iteration++) {
      minHeap.clear();
      for(int i = 0; i < k; i++) {
        clusters.get(i).clear();
      }
      LOG.incrementProcessed(prog);
      boolean changed = assignToNearestCluster(relation, means, clusters, assignment, varsum, minHeap, heapsize);
      double vartotal = logVarstat(varstat, varsum);
      // Stop if no cluster assignment changed, or the new varsum is higher
      // than the previous value.
      if(!changed || vartotal > prevvartotal) {
        break;
      }
      prevvartotal = vartotal;

      // Recompute means.
      means = meansWithTreshhold(clusters, means, relation, heapsize > 0 ? minHeap.peek() : Double.POSITIVE_INFINITY);
    }

    // create noisecluster if wanted
    ModifiableDoubleDBIDList noiseids = null;
    if(noiseFlag && heapsize > 0) {
      clusters.add(noiseids = DBIDUtil.newDistanceDBIDList((int) (relation.size() * 2. / k)));
      double tresh = minHeap.peek();
      for(int i = 0; i < k; i++) {
        for(DoubleDBIDListMIter it = clusters.get(i).iter(); it.valid(); it.advance()) {
          final double dist = it.doubleValue();
          // Add to the noise cluster:
          if(dist >= tresh) {
            noiseids.add(dist, it);
            assignment.putInt(it, k);
            it.remove();
          }
        }
      }
    }

    LOG.setCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
    }

    // Wrap result
    Clustering<KMeansModel> result = new Clustering<>("k-Means Clustering", "kmeans-clustering");
    for(int i = 0; i < k; i++) {
      DBIDs ids = clusters.get(i);
      if(ids.size() == 0) {
        continue;
      }
      KMeansModel model = new KMeansModel(means[i], varsum[i]);
      result.addToplevelCluster(new Cluster<>(ids, model));
    }

    // Noise Cluster
    if(noiseFlag) {
      KMeansModel model = new KMeansModel(null, 0);
      DBIDs ids = noiseids;
      if(ids.size() == 0) {
        return result;
      }
      result.addToplevelCluster(new Cluster<>(ids, true, model));
    }
    return result;
  }

  /**
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean. And
   * saves the distance in a MinHeap
   *
   * @param relation the database to cluster
   * @param means a list of k means
   * @param clusters cluster assignment
   * @param assignment Current cluster assignment
   * @param varsum Variance sum output
   * @param minHeap Heap for minimum values
   * @param heapsize the size of the minheap
   * @return true when the object was reassigned
   */
  protected boolean assignToNearestCluster(Relation<? extends V> relation, double[][] means, List<? extends ModifiableDoubleDBIDList> clusters, WritableIntegerDataStore assignment, double[] varsum, DoubleMinHeap minHeap, int heapsize) {
    assert (k == means.length);
    boolean changed = false;
    Arrays.fill(varsum, 0.);
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

      if(heapsize > 0) {
        minHeap.add(mindist, heapsize);
      }

      varsum[minIndex] += mindist;
      clusters.get(minIndex).add(mindist, iditer);
      changed |= assignment.putInt(iditer, minIndex) != minIndex;
    }
    return changed;
  }

  /**
   * Returns the mean vectors of the given clusters in the given database.
   *
   * @param clusters the clusters to compute the means
   * @param means the recent means
   * @param database the database containing the vectors
   * @return the mean vectors of the given clusters in the given database
   */
  protected double[][] meansWithTreshhold(List<? extends ModifiableDoubleDBIDList> clusters, double[][] means, Relation<V> database, Double tresh) {
    // TODO: use Kahan summation for better numerical precision?
    double[][] newMeans = new double[k][];
    for(int i = 0; i < k; i++) {
      DoubleDBIDList list = clusters.get(i);
      double[] raw = null;
      int count = 0;
      // Update with remaining instances
      for(DoubleDBIDListIter iter = list.iter(); iter.valid(); iter.advance()) {
        if(iter.doubleValue() >= tresh) {
          continue;
        }
        NumberVector vec = database.get(iter);
        if(raw == null) { // Initialize:
          raw = vec.toArray();
        }
        for(int j = 0; j < raw.length; j++) {
          raw[j] += vec.doubleValue(j);
        }
        count++;
      }
      newMeans[i] = (raw != null) ? VMath.timesEquals(raw, 1.0 / count) : means[i];
    }
    return newMeans;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Jonas Steinke
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractKMeans.Parameterizer<V> {
    /**
     * Parameter to specify the number of neighbors to ignore.
     */
    public static final OptionID RATE_ID = new OptionID("kmeansmm.l", "Number of outliers to ignore, or (if less than 1) a relative rate.");

    /**
     * Flag to produce a "noise" cluster, instead of assigning them to the
     * nearest neighbor.
     */
    public static final OptionID NOISE_FLAG_ID = new OptionID("kmeansmm.noisecluster", "Create a noise cluster, instead of assigning the noise objects.");

    /**
     * Outlier rate.
     */
    private double rate;

    /**
     * Noise cluster flag.
     */
    private boolean noiseFlag;

    @Override
    protected Logging getLogger() {
      return LOG;
    }

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter rateP = new DoubleParameter(RATE_ID, 0.05)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(rateP)) {
        rate = rateP.doubleValue();
      }
      Flag createNoiseCluster = new Flag(NOISE_FLAG_ID);
      if(config.grab(createNoiseCluster)) {
        noiseFlag = createNoiseCluster.getValue();
      }
    }

    @Override
    protected KMeansMinusMinus<V> makeInstance() {
      return new KMeansMinusMinus<V>(distanceFunction, k, maxiter, initializer, rate, noiseFlag);
    }
  }
}
