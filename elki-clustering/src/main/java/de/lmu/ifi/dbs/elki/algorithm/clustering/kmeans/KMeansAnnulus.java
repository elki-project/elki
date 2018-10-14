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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
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
 *
 * @apiviz.has KMeansModel
 *
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
    bibkey = "book/partclust15/HamerlyD15")
public class KMeansAnnulus<V extends NumberVector> extends KMeansHamerly<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansAnnulus.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMeansAnnulus.class.getName();

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public KMeansAnnulus(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization<? super V> initializer, boolean varstat) {
    super(distanceFunction, k, maxiter, initializer, varstat);
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    if(relation.size() <= 0) {
      return new Clustering<>("k-Means Clustering", "kmeans-clustering");
    }
    // Choose initial means
    LOG.statistics(new StringStatistic(KEY + ".initialization", initializer.toString()));
    double[][] means = initializer.chooseInitialMeans(database, relation, k, getDistanceFunction());
    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet((int) (relation.size() * 2. / k)));
    }
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
    WritableIntegerDataStore second = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
    // Hamerly bounds
    WritableDoubleDataStore upper = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.POSITIVE_INFINITY);
    WritableDoubleDataStore lower = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0.);
    // Storage for updated means:
    final int dim = means[0].length;
    double[][] sums = new double[k][dim], newmeans = new double[k][dim];
    // Separation of means / distance moved.
    double[] sep = new double[k];
    // Cluster distances from origin
    double[] cdist = new double[k];
    int[] cnum = new int[k];

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Means iteration", LOG) : null;
    LongStatistic rstat = LOG.isStatistics() ? new LongStatistic(KEY + ".reassignments") : null;
    LongStatistic diststat = LOG.isStatistics() ? new LongStatistic(KEY + ".distance-computations") : null;
    int iteration = 0;
    for(; maxiter <= 0 || iteration < maxiter; iteration++) {
      LOG.incrementProcessed(prog);
      int changed;
      if(iteration == 0) {
        changed = initialAssignToNearestCluster(relation, means, sums, clusters, assignment, second, upper, lower, diststat);
      }
      else {
        orderMeans(means, sep, cdist, cnum);
        changed = assignToNearestCluster(relation, means, sums, clusters, assignment, second, sep, cdist, cnum, upper, lower, diststat);
      }
      LOG.statistics(rstat != null ? rstat.setLong(changed) : null);
      // Stop if no cluster assignment changed.
      if(changed == 0) {
        break;
      }
      // Recompute means.
      for(int i = 0; i < k; i++) {
        VMath.overwriteTimes(newmeans[i], sums[i], 1. / clusters.get(i).size());
      }
      double delta = movedDistance(means, newmeans, sep);
      updateBounds(relation, assignment, upper, lower, sep, delta);
      for(int i = 0; i < k; i++) {
        System.arraycopy(newmeans[i], 0, means[i], 0, dim);
      }
    }
    LOG.setCompleted(prog);
    LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
    LOG.statistics(diststat);
    upper.destroy();
    lower.destroy();

    return buildResult(clusters, means, varstat, relation, diststat);
  }

  /**
   * Recompute the separation of cluster means.
   * <p>
   * Used by sort, and our exponion implementation.
   *
   * @param means Cluster means
   * @param cdist Distance from origin
   * @param cnum Center numbers
   */
  protected void orderMeans(double[][] means, double[] sep, double[] cdist, int[] cnum) {
    final int k = cdist.length;
    final boolean issquared = distanceFunction.isSquared();
    assert (sep.length == k);
    Arrays.fill(sep, Double.POSITIVE_INFINITY);
    for(int i = 0; i < k; i++) {
      cdist[i] = VMath.euclideanLength(means[i]);
      cnum[i] = i;
      DoubleVector mi = DoubleVector.wrap(means[i]);
      for(int j = 0; j < i; j++) {
        double d = distanceFunction.distance(mi, DoubleVector.wrap(means[j]));
        d = 0.5 * (issquared ? FastMath.sqrt(d) : d);
        sep[i] = (d < sep[i]) ? d : sep[i];
        sep[j] = (d < sep[j]) ? d : sep[j];
      }
    }
    DoubleIntegerArrayQuickSort.sort(cdist, cnum, k);
  }

  /**
   * Perform initial cluster assignment.
   *
   * @param relation Data
   * @param means Current means
   * @param sums Running sums of the new means
   * @param clusters Current clusters
   * @param assignment Cluster assignment
   * @param second Second best cluster assignment
   * @param upper Upper bounds
   * @param lower Lower boundsO
   * @return Number of changes (i.e. relation size)
   */
  protected int initialAssignToNearestCluster(Relation<V> relation, double[][] means, double[][] sums, List<ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, WritableIntegerDataStore second, WritableDoubleDataStore upper, WritableDoubleDataStore lower, LongStatistic diststat) {
    assert (k == means.length);
    boolean issquared = distanceFunction.isSquared();
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      V fv = relation.get(it);
      // Find closest center, and distance to two closest centers
      double min1 = Double.POSITIVE_INFINITY, min2 = Double.POSITIVE_INFINITY;
      int minIndex = -1, secIndex = -1;
      for(int i = 0; i < k; i++) {
        double dist = distanceFunction.distance(fv, DoubleVector.wrap(means[i]));
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
      upper.putDouble(it, issquared ? FastMath.sqrt(min1) : min1);
      lower.putDouble(it, issquared ? FastMath.sqrt(min2) : min2);
    }
    if(diststat != null) {
      diststat.increment(k * relation.size());
    }
    return relation.size();
  }

  /**
   * Reassign objects, but avoid unnecessary computations based on their bounds.
   *
   * @param relation Data
   * @param means Current means
   * @param sums New means as running sums
   * @param clusters Current clusters
   * @param assignment Cluster assignment
   * @param second Second closest cluster assignment
   * @param sep Separation of means
   * @param cdist Center-to-center distances
   * @param cnum Sorted nearest centers
   * @param upper Upper bounds
   * @param lower Lower bounds
   * @param diststat Distance statistics
   * @return true when the object was reassigned
   */
  protected int assignToNearestCluster(Relation<V> relation, double[][] means, double[][] sums, List<ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, WritableIntegerDataStore second, double[] sep, double[] cdist, int[] cnum, WritableDoubleDataStore upper, WritableDoubleDataStore lower, LongStatistic diststat) {
    assert (k == means.length);
    final boolean issquared = distanceFunction.isSquared();
    int changed = 0, dists = 0;
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
      V fv = relation.get(it);
      double curd2 = distanceFunction.distance(fv, DoubleVector.wrap(means[cur]));
      ++dists;
      u = issquared ? FastMath.sqrt(curd2) : curd2;
      upper.putDouble(it, u);
      if(u <= z || u <= sa) {
        continue;
      }
      final int sec = second.intValue(it);
      double secd2 = distanceFunction.distance(fv, DoubleVector.wrap(means[sec]));
      double secd = issquared ? FastMath.sqrt(secd2) : secd2;
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
        double dist = distanceFunction.distance(fv, DoubleVector.wrap(means[c]));
        ++dists;
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
        upper.putDouble(it, issquared ? FastMath.sqrt(min1) : min1);
      }
      lower.putDouble(it, issquared ? FastMath.sqrt(min2) : min2);
    }
    if(diststat != null) {
      diststat.increment(dists);
    }
    return changed;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends KMeansHamerly.Parameterizer<V> {
    @Override
    protected Logging getLogger() {
      return LOG;
    }

    @Override
    protected KMeansAnnulus<V> makeInstance() {
      return new KMeansAnnulus<>(distanceFunction, k, maxiter, initializer, varstat);
    }
  }
}
