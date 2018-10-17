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
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Newlings's exponion k-means algorithm, exploiting the triangle inequality.
 * <p>
 * This is <b>not</b> a complete implementation, the approximative sorting part
 * is missing. We also had to guess on the paper how to make best use of F.
 * <p>
 * Reference:
 * <p>
 * J. Newling<br>
 * Fast k-means with accurate bounds<br>
 * Proc. 33nd Int. Conf. on Machine Learning, ICML 2016
 *
 * @author Erich Schubert
 *
 * @apiviz.has KMeansModel
 *
 * @param <V> vector datatype
 */
@Reference(authors = "J. Newling", //
    title = "Fast k-means with accurate bounds", //
    booktitle = "Proc. 33nd Int. Conf. on Machine Learning, ICML 2016", //
    url = "http://jmlr.org/proceedings/papers/v48/newling16.html", //
    bibkey = "DBLP:conf/icml/NewlingF16")
public class KMeansExponion<V extends NumberVector> extends KMeansHamerly<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansExponion.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMeansExponion.class.getName();

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public KMeansExponion(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization<? super V> initializer, boolean varstat) {
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
    // Hamerly bounds
    WritableDoubleDataStore upper = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.POSITIVE_INFINITY);
    WritableDoubleDataStore lower = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0.);
    // Storage for updated means:
    final int dim = means[0].length;
    double[][] sums = new double[k][dim], newmeans = new double[k][dim];
    // Separation of means / distance moved.
    double[] sep = new double[k];
    // Cluster distances
    double[][] cdist = new double[k][k];
    int[][] cnum = new int[k][k - 1];

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Means iteration", LOG) : null;
    LongStatistic rstat = LOG.isStatistics() ? new LongStatistic(KEY + ".reassignments") : null;
    LongStatistic diststat = LOG.isStatistics() ? new LongStatistic(KEY + ".distance-computations") : null;
    int iteration = 0;
    for(; maxiter <= 0 || iteration < maxiter; iteration++) {
      LOG.incrementProcessed(prog);
      int changed;
      if(iteration == 0) {
        changed = initialAssignToNearestCluster(relation, means, sums, clusters, assignment, upper, lower, diststat);
      }
      else {
        recomputeSeperation(means, sep, cdist, diststat);
        nearestMeans(cdist, cnum);
        changed = assignToNearestCluster(relation, means, sums, clusters, assignment, sep, cdist, cnum, upper, lower, diststat);
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
   * Reassign objects, but avoid unnecessary computations based on their bounds.
   *
   * @param relation Data
   * @param means Current means
   * @param sums New means as running sums
   * @param clusters Current clusters
   * @param assignment Cluster assignment
   * @param sep Separation of means
   * @param cdist Center-to-center distances
   * @param cnum Sorted nearest centers
   * @param upper Upper bounds
   * @param lower Lower bounds
   * @param diststat Distance statistics
   * @return true when the object was reassigned
   */
  protected int assignToNearestCluster(Relation<V> relation, double[][] means, double[][] sums, List<ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, double[] sep, double[][] cdist, int[][] cnum, WritableDoubleDataStore upper, WritableDoubleDataStore lower, LongStatistic diststat) {
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
      double cdis2 = distanceFunction.distance(fv, DoubleVector.wrap(means[cur]));
      ++dists;
      u = issquared ? FastMath.sqrt(cdis2) : cdis2;
      upper.putDouble(it, u);
      if(u <= z || u <= sa) {
        continue;
      }
      double r = u + 0.5 * sa; // Our cdist are scaled 0.5
      // Find closest center, and distance to two closest centers
      double min1 = cdis2, min2 = Double.POSITIVE_INFINITY;
      int minIndex = cur;
      for(int i = 0; i < k - 1; i++) {
        int c = cnum[cur][i];
        if(cdist[cur][c] > r) {
          break;
        }
        double dist = distanceFunction.distance(fv, DoubleVector.wrap(means[c]));
        ++dists;
        if(dist < min1) {
          minIndex = c;
          min2 = min1;
          min1 = dist;
        }
        else if(dist < min2) {
          min2 = dist;
        }
      }
      if(minIndex != cur) {
        clusters.get(minIndex).add(it);
        clusters.get(cur).remove(it);
        assignment.putInt(it, minIndex);
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
    protected KMeansExponion<V> makeInstance() {
      return new KMeansExponion<>(distanceFunction, k, maxiter, initializer, varstat);
    }
  }
}
