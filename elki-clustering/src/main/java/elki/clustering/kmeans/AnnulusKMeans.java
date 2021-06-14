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

import java.util.Arrays;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.math.linearalgebra.VMath;
import elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;
import elki.utilities.documentation.Reference;

/**
 * Annulus k-means algorithm. A variant of Hamerly with an additional bound,
 * based on comparing the norm of the mean and the norm of the points.
 * <p>
 * This implementation could be further improved by precomputing and storing the
 * norms of all points (at the cost of O(n) memory additionally).
 * <p>
 * Reference:
 * <p>
 * J. Drake<br>
 * Faster k-means clustering<br>
 * Masters Thesis
 * <p>
 * G. Hamerly and J. Drake<br>
 * Accelerating Lloyd’s Algorithm for k-Means Clustering<br>
 * Partitional Clustering Algorithms
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @navassoc - - - KMeansModel
 * @param <V> vector datatype
 */
@Reference(authors = "J. Drake", //
    title = "Faster k-means clustering", //
    booktitle = "Faster k-means clustering", //
    url = "http://hdl.handle.net/2104/8826", //
    bibkey = "mathesis/Drake13")
@Reference(authors = "G. Hamerly and J. Drake", //
    title = "Accelerating Lloyd’s Algorithm for k-Means Clustering", //
    booktitle = "Partitional Clustering Algorithms", //
    url = "https://doi.org/10.1007/978-3-319-09259-1_2", //
    bibkey = "doi:10.1007/978-3-319-09259-1_2")
public class AnnulusKMeans<V extends NumberVector> extends HamerlyKMeans<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(AnnulusKMeans.class);

  /**
   * Constructor.
   *
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public AnnulusKMeans(NumberVectorDistance<? super V> distance, int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
    super(distance, k, maxiter, initializer, varstat);
  }

  @Override
  public Clustering<KMeansModel> run(Relation<V> relation) {
    Instance instance = new Instance(relation, distance, initialMeans(relation));
    instance.run(maxiter);
    return instance.buildResult(varstat, relation);
  }

  /**
   * Inner instance, storing state for a single data set.
   *
   * @author Erich Schubert
   */
  protected static class Instance extends HamerlyKMeans.Instance {
    /**
     * Second nearest cluster.
     */
    WritableIntegerDataStore second;

    /**
     * Cluster center distances.
     */
    double[] cdist;

    /**
     * Sorted neighbors
     */
    int[] cnum;

    /**
     * Constructor.
     *
     * @param relation Relation
     * @param df Distance function
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
      second = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
      cdist = new double[k];
      cnum = new int[k];
    }

    @Override
    protected int initialAssignToNearestCluster() {
      assert k == means.length;
      double[][] sep2 = new double[k][k];
      computeSquaredSeparation(sep2);
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        NumberVector fv = relation.get(it);
        // Find closest center, and distance to two closest centers
        double min1 = distance(fv, means[0]), min2 = k > 1 ? distance(fv, means[1]) : min1;
        int minIndex = 0, secIndex = 1;
        if(min2 < min1) {
          double tmp = min1;
          min1 = min2;
          min2 = tmp;
          minIndex = 1;
          secIndex = 0;
        }
        for(int i = 2; i < k; i++) {
          if(min2 > sep2[minIndex][i]) {
            double dist = distance(fv, means[i]);
            if(dist < min1) {
              secIndex = minIndex;
              minIndex = i;
              min2 = min1;
              min1 = dist;
            }
            else if(dist < min2) {
              secIndex = i;
              min2 = dist;
            }
          }
        }
        // Assign to nearest cluster.
        clusters.get(minIndex).add(it);
        assignment.putInt(it, minIndex);
        second.putInt(it, secIndex);
        plusEquals(sums[minIndex], fv);
        upper.putDouble(it, isSquared ? Math.sqrt(min1) : min1);
        lower.putDouble(it, isSquared ? Math.sqrt(min2) : min2);
      }
      return relation.size();
    }

    /**
     * Recompute the separation of cluster means.
     */
    protected void orderMeans() {
      final int k = cdist.length;
      assert sep.length == k;
      Arrays.fill(sep, Double.POSITIVE_INFINITY);
      for(int i = 0; i < k; i++) {
        double[] mi = means[i];
        cdist[i] = VMath.euclideanLength(mi);
        cnum[i] = i;
        for(int j = 0; j < i; j++) {
          double halfd = 0.5 * sqrtdistance(mi, means[j]);
          sep[i] = halfd < sep[i] ? halfd : sep[i];
          sep[j] = halfd < sep[j] ? halfd : sep[j];
        }
      }
      DoubleIntegerArrayQuickSort.sort(cdist, cnum, k);
    }

    @Override
    protected int assignToNearestCluster() {
      assert (k == means.length);
      orderMeans();
      int changed = 0;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int orig = assignment.intValue(it);
        // Compute the current bound:
        final double z = lower.doubleValue(it);
        final double sa = sep[orig];
        double u = upper.doubleValue(it);
        if(u <= z || u <= sa) {
          continue;
        }
        // Update the upper bound
        NumberVector fv = relation.get(it);
        double curd2 = distance(fv, means[orig]);
        upper.putDouble(it, u = isSquared ? Math.sqrt(curd2) : curd2);
        if(u <= z || u <= sa) {
          continue;
        }
        final int sec = second.intValue(it);
        double secd2 = distance(fv, means[sec]);
        double secd = isSquared ? Math.sqrt(secd2) : secd2;
        double r = u > secd ? u : secd;
        final double norm = EuclideanDistance.STATIC.norm(fv);
        // Find closest center, and distance to two closest centers
        double min1 = curd2, min2 = secd2;
        int cur = orig, secIndex = sec;
        if(curd2 > secd2) {
          min1 = secd2;
          min2 = curd2;
          cur = sec;
          secIndex = orig;
        }
        for(int i = 0; i < k; i++) {
          final int c = cnum[i]; // Optimized ordering
          if(c == orig || c == sec) {
            continue;
          }
          double d = cdist[i] - norm;
          if(-d > r) {
            continue; // Not yet a candidate
          }
          if(d > r) {
            break; // No longer a candidate
          }
          double dist = distance(fv, means[c]);
          if(dist < min1) {
            secIndex = cur;
            cur = c;
            min2 = min1;
            min1 = dist;
          }
          else if(dist < min2) {
            secIndex = c;
            min2 = dist;
          }
        }
        // Object has to be reassigned.
        if(cur != orig) {
          clusters.get(cur).add(it);
          clusters.get(orig).remove(it);
          assignment.putInt(it, cur);
          second.putInt(it, secIndex);
          plusMinusEquals(sums[cur], sums[orig], fv);
          ++changed;
          upper.putDouble(it, min1 == curd2 ? u : isSquared ? Math.sqrt(min1) : min1);
        }
        lower.putDouble(it, min2 == curd2 ? u : isSquared ? Math.sqrt(min2) : min2);
      }
      return changed;
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
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> extends HamerlyKMeans.Par<V> {
    @Override
    public AnnulusKMeans<V> make() {
      return new AnnulusKMeans<>(distance, k, maxiter, initializer, varstat);
    }
  }
}
