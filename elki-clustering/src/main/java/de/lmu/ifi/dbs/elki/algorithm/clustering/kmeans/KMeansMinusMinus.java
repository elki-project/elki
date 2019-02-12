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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

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
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
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
 * <p>
 * Similar to Lloyds K-means algorithm, but ignores the farthest points when
 * updating the means, considering them to be outliers.
 * <p>
 * Reference:
 * <p>
 * S. Chawla, A. Gionis<br>
 * k-means--: A Unified Approach to Clustering and Outlier Detection<br>
 * Proc. 13th SIAM Int. Conf. on Data Mining (SDM 2013)
 *
 * @author Jonas Steinke
 * @since 0.7.5
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Title("K-Means--")
@Reference(authors = "S. Chawla, A. Gionis", //
    title = "k-means--: A Unified Approach to Clustering and Outlier Detection", //
    booktitle = "Proc. 13th SIAM Int. Conf. on Data Mining (SDM 2013)", //
    url = "https://doi.org/10.1137/1.9781611972832.21", //
    bibkey = "DBLP:conf/sdm/ChawlaG13")
public class KMeansMinusMinus<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansMinusMinus.class);

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
  public KMeansMinusMinus(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer, double rate, boolean noiseFlag) {
    super(distanceFunction, k, maxiter, initializer);
    this.rate = rate;
    this.noiseFlag = noiseFlag;
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    Instance instance = new Instance(relation, getDistanceFunction(), initialMeans(database, relation));
    instance.run(maxiter);
    return instance.buildResultWithNoise();
  }

  /**
   * Inner instance, storing state for a single data set.
   *
   * @author Erich Schubert
   */
  protected class Instance extends AbstractKMeans.Instance {
    /**
     * Heap of the noise candidates.
     */
    DoubleMinHeap minHeap;

    /**
     * Desired size of the heap.
     */
    int heapsize;

    /**
     * Variance of the previous iteration
     */
    double prevvartotal = Double.POSITIVE_INFINITY;

    /**
     * Cluster storage.
     */
    List<ModifiableDoubleDBIDList> clusters;

    /**
     * Constructor.
     *
     * @param relation Relation
     * @param df Distance function
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistanceFunction<?> df, double[][] means) {
      super(relation, df, means);
      heapsize = (int) (rate < 1. ? Math.ceil(relation.size() * rate) : rate);
      minHeap = new DoubleMinHeap(heapsize);
      // Setup cluster assignment store
      clusters = new ArrayList<>();
      for(int i = 0; i < k; i++) {
        clusters.add(DBIDUtil.newDistanceDBIDList((int) (relation.size() * 2. / k)));
      }
      ((AbstractKMeans.Instance) this).clusters = null; // Invalidate
    }

    @Override
    protected int iterate(int iteration) {
      if(iteration > 1) {
        means = meansWithTreshhold(heapsize == 0 || minHeap.size() < heapsize ? Double.POSITIVE_INFINITY : minHeap.peek());
      }
      minHeap.clear();
      for(int i = 0; i < k; i++) {
        clusters.get(i).clear();
      }
      int changed = assignToNearestCluster();
      double vartotal = VMath.sum(varsum);
      // Stop if nothing changed, or the new varsum is higher than the previous
      if(vartotal > prevvartotal) {
        return -changed;
      }
      prevvartotal = vartotal;
      return changed;
    }

    protected Clustering<KMeansModel> buildResultWithNoise() {
      // create noisecluster if wanted
      ModifiableDoubleDBIDList noiseids = null;
      if(noiseFlag && heapsize > 0) {
        clusters.add(noiseids = DBIDUtil.newDistanceDBIDList((minHeap.size())));
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

      // Wrap result
      Clustering<KMeansModel> result = new Clustering<>("k-Means-- Clustering", "kmeans-minus-minus-clustering");
      for(int i = 0; i < k; i++) {
        DBIDs ids = clusters.get(i);
        if(ids.isEmpty()) {
          continue;
        }
        result.addToplevelCluster(new Cluster<>(ids, new KMeansModel(means[i], varsum[i])));
      }

      // Noise Cluster
      if(noiseFlag && noiseids != null && !noiseids.isEmpty()) {
        result.addToplevelCluster(new Cluster<>(noiseids, true, new KMeansModel(null, 0)));
      }
      return result;
    }

    /**
     * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids
     * of those FeatureVectors, that are nearest to the k<sup>th</sup> mean. And
     * saves the distance in a MinHeap.
     *
     * @return the number of reassigned objects
     */
    protected int assignToNearestCluster() {
      assert (k == means.length);
      int changed = 0;
      Arrays.fill(varsum, 0.);
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        NumberVector fv = relation.get(iditer);
        double mindist = Double.POSITIVE_INFINITY;
        int minIndex = 0;
        for(int i = 0; i < k; i++) {
          double dist = distance(fv, DoubleVector.wrap(means[i]));
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
        if(assignment.putInt(iditer, minIndex) != minIndex) {
          ++changed;
        }
      }
      return changed;
    }

    /**
     * Returns the mean vectors of the given clusters in the given database.
     *
     * @param tresh Threshold
     * @return the mean vectors of the given clusters in the given database
     */
    protected double[][] meansWithTreshhold(double tresh) {
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
          NumberVector vec = relation.get(iter);
          if(raw == null) {
            raw = vec.toArray();
          }
          else {
            plusEquals(raw, vec);
          }
          count++;
        }
        newMeans[i] = (raw != null) ? VMath.timesEquals(raw, 1. / count) : means[i];
      }
      return newMeans;
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Jonas Steinke
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
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter rateP = new DoubleParameter(RATE_ID, 0.05) //
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
