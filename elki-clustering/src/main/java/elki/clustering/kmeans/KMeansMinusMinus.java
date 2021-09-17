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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.kmeans.quality.BayesianInformationCriterion;
import elki.clustering.kmeans.quality.KMeansQualityMeasure;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.math.linearalgebra.VMath;
import elki.result.Metadata;
import elki.utilities.datastructures.heap.DoubleMinHeap;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

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
   * Constructor.
   *
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param noiseFlag Create a noise cluster instead of assigning to the nearest
   *        cluster
   */
  public KMeansMinusMinus(NumberVectorDistance<? super V> distance, int k, int maxiter, KMeansInitialization initializer, double rate) {
    super(distance, k, maxiter, initializer);
    this.rate = rate;
  }

  @Override
  public Clustering<KMeansModel> run(Relation<V> relation) {
    Instance instance = new Instance(relation, distance, initialMeans(relation));
    instance.run(maxiter);
//    return instance.buildResultWithNoise();
    {
      Clustering<KMeansModel> result = instance.buildResultWithNoise();
      KMeansQualityMeasure<NumberVector> informationCriterion = new BayesianInformationCriterion();
      double bic = informationCriterion.quality(result, distance, relation);
      System.out.println("bic: " + bic);
      return result;
    }
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
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
      heapsize = (int) (rate < 1. ? Math.ceil(relation.size() * rate) : rate);
      minHeap = new DoubleMinHeap(heapsize);
      // Setup cluster assignment store
      clusters = new ArrayList<>();
      for(int i = 0; i < k; i++) {
        clusters.add(DBIDUtil.newDistanceDBIDList((int) (relation.size() * 2. / k)));
      }
      super.clusters = null; // Invalidate
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
      // create noisecluster
      ModifiableDoubleDBIDList noiseids = null;
      if(heapsize > 0) {
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
      Clustering<KMeansModel> result = new Clustering<>();
      Metadata.of(result).setLongName("k-Means-- Clustering");
      for(int i = 0; i < k; i++) {
        DBIDs ids = clusters.get(i);
        if(ids.isEmpty()) {
          continue;
        }
        result.addToplevelCluster(new Cluster<>(ids, new KMeansModel(means[i], varsum[i])));
      }

      // Noise Cluster
      if(noiseids != null && !noiseids.isEmpty()) {
        double[] dummyMeans = new double[RelationUtil.dimensionality(relation)];
        Arrays.fill(dummyMeans, 0.);
        result.addToplevelCluster(new Cluster<>(noiseids, true, new KMeansModel(dummyMeans, 0)));
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
          double dist = distance(fv, means[i]);
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
  public static class Par<V extends NumberVector> extends AbstractKMeans.Par<V> {
    /**
     * Parameter to specify the number of neighbors to ignore.
     */
    public static final OptionID RATE_ID = new OptionID("kmeansmm.l", "Number of outliers to ignore, or (if less than 1) a relative rate.");

    /**
     * Outlier rate.
     */
    private double rate;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new DoubleParameter(RATE_ID, 0.05) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> rate = x);
    }

    @Override
    public KMeansMinusMinus<V> make() {
      return new KMeansMinusMinus<V>(distance, k, maxiter, initializer, rate);
    }
  }
}
