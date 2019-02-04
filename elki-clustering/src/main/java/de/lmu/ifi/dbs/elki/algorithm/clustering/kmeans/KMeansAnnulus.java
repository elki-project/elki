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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

import net.jafama.FastMath;

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
public class KMeansAnnulus<V extends NumberVector> extends KMeansHamerly<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansAnnulus.class);

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public KMeansAnnulus(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer, boolean varstat) {
    super(distanceFunction, k, maxiter, initializer, varstat);
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    Instance instance = new Instance(relation, getDistanceFunction(), initialMeans(database, relation));
    instance.run(maxiter);
    return instance.buildResult(varstat, relation);
  }

  /**
   * Inner instance, storing state for a single data set.
   *
   * @author Erich Schubert
   */
  protected static class Instance extends KMeansHamerly.Instance {
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

    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistanceFunction<?> df, double[][] means) {
      super(relation, df, means);
      second = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
      cdist = new double[k];
      cnum = new int[k];
    }

    @Override
    protected int initialAssignToNearestCluster() {
      assert (k == means.length);
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        NumberVector fv = relation.get(it);
        // Find closest center, and distance to two closest centers
        double min1 = Double.POSITIVE_INFINITY, min2 = Double.POSITIVE_INFINITY;
        int minIndex = -1, secIndex = -1;
        for(int i = 0; i < k; i++) {
          double dist = distance(fv, DoubleVector.wrap(means[i]));
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
        // Assign to nearest cluster.
        clusters.get(minIndex).add(it);
        assignment.putInt(it, minIndex);
        second.putInt(it, secIndex);
        plusEquals(sums[minIndex], fv);
        upper.putDouble(it, isSquared ? FastMath.sqrt(min1) : min1);
        lower.putDouble(it, isSquared ? FastMath.sqrt(min2) : min2);
      }
      return relation.size();
    }

    /**
     * Recompute the separation of cluster means.
     */
    protected void orderMeans() {
      final int k = cdist.length;
      assert (sep.length == k);
      Arrays.fill(sep, Double.POSITIVE_INFINITY);
      for(int i = 0; i < k; i++) {
        cdist[i] = VMath.euclideanLength(means[i]);
        cnum[i] = i;
        DoubleVector mi = DoubleVector.wrap(means[i]);
        for(int j = 0; j < i; j++) {
          double d = distance(mi, DoubleVector.wrap(means[j]));
          d = 0.5 * (isSquared ? FastMath.sqrt(d) : d);
          sep[i] = (d < sep[i]) ? d : sep[i];
          sep[j] = (d < sep[j]) ? d : sep[j];
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
        final int cur = assignment.intValue(it);
        // Compute the current bound:
        final double z = lower.doubleValue(it);
        final double sa = sep[cur];
        double u = upper.doubleValue(it);
        if(u <= z || u <= sa) {
          continue;
        }
        // Update the upper bound
        NumberVector fv = relation.get(it);
        double curd2 = distance(fv, DoubleVector.wrap(means[cur]));
        u = isSquared ? FastMath.sqrt(curd2) : curd2;
        upper.putDouble(it, u);
        if(u <= z || u <= sa) {
          continue;
        }
        final int sec = second.intValue(it);
        double secd2 = distance(fv, DoubleVector.wrap(means[sec]));
        double secd = isSquared ? FastMath.sqrt(secd2) : secd2;
        double r = u > secd ? u : secd;
        final double norm = EuclideanDistanceFunction.STATIC.norm(fv);
        // Find closest center, and distance to two closest centers
        double min1 = curd2, min2 = secd2;
        int minIndex = cur, secIndex = sec;
        if(curd2 > secd2) {
          min1 = secd2;
          min2 = curd2;
          minIndex = sec;
          secIndex = cur;
        }
        for(int i = 0; i < k; i++) {
          int c = cnum[i];
          if(c == cur || c == sec) {
            continue;
          }
          double d = cdist[i] - norm;
          if(-d > r) {
            continue; // Not yet a candidate
          }
          if(d > r) {
            break; // No longer a candidate
          }
          double dist = distance(fv, DoubleVector.wrap(means[c]));
          if(dist < min1) {
            secIndex = minIndex;
            minIndex = c;
            min2 = min1;
            min1 = dist;
          }
          else if(dist < min2) {
            secIndex = c;
            min2 = dist;
          }
        }
        if(minIndex != cur) {
          clusters.get(minIndex).add(it);
          clusters.get(cur).remove(it);
          assignment.putInt(it, minIndex);
          second.putInt(it, secIndex);
          plusMinusEquals(sums[minIndex], sums[cur], fv);
          ++changed;
          upper.putDouble(it, min1 == curd2 ? u : isSquared ? FastMath.sqrt(min1) : min1);
        }
        lower.putDouble(it, min2 == curd2 ? u : isSquared ? FastMath.sqrt(min2) : min2);
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
  public static class Parameterizer<V extends NumberVector> extends KMeansHamerly.Parameterizer<V> {
    @Override
    protected KMeansAnnulus<V> makeInstance() {
      return new KMeansAnnulus<>(distanceFunction, k, maxiter, initializer, varstat);
    }
  }
}
