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
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.*;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Simplified version of Elkan's k-means by exploiting the triangle inequality.
 * <p>
 * Compared to {@link KMeansElkan}, this uses less pruning, but also does not
 * need to maintain a matrix of pairwise centroid separation.
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
public class KMeansSimplifiedElkan<V extends NumberVector> extends KMeansElkan<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansSimplifiedElkan.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMeansSimplifiedElkan.class.getName();

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public KMeansSimplifiedElkan(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization<? super V> initializer, boolean varstat) {
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
    // Elkan bounds
    WritableDoubleDataStore upper = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.POSITIVE_INFINITY);
    WritableDataStore<double[]> lower = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, double[].class);
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      lower.put(it, new double[k]); // Filled with 0.
    }
    // Storage for updated means:
    final int dim = means[0].length;
    double[][] sums = new double[k][dim], newmeans = new double[k][dim];
    double[] moved = new double[k];

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Means iteration", LOG) : null;
    LongStatistic rstat = LOG.isStatistics() ? new LongStatistic(this.getClass().getName() + ".reassignments") : null;
    int iteration = 0;
    for(; maxiter <= 0 || iteration < maxiter; iteration++) {
      LOG.incrementProcessed(prog);
      int changed;
      if(iteration == 0) {
        changed = initialAssignToNearestCluster(relation, means, sums, clusters, assignment, upper, lower);
      }
      else {
        changed = assignToNearestCluster(relation, means, sums, clusters, assignment, upper, lower);
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
      movedDistance(means, newmeans, moved);
      updateBounds(relation, assignment, upper, lower, moved);
      for(int i = 0; i < k; i++) {
        System.arraycopy(newmeans[i], 0, means[i], 0, dim);
      }
    }
    LOG.setCompleted(prog);
    LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
    upper.destroy();
    lower.destroy();

    // Wrap result
    double totalvariance = 0.;
    Clustering<KMeansModel> result = new Clustering<>("k-Means Clustering", "kmeans-clustering");
    for(int i = 0; i < clusters.size(); i++) {
      DBIDs ids = clusters.get(i);
      if(ids.isEmpty()) {
        continue;
      }
      double[] mean = means[i];
      double varsum = 0.;
      if(varstat) {
        DoubleVector mvec = DoubleVector.wrap(mean);
        for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
          varsum += distanceFunction.distance(mvec, relation.get(it));
        }
        totalvariance += varsum;
      }
      result.addToplevelCluster(new Cluster<>(ids, new KMeansModel(mean, varsum)));
    }
    if(LOG.isStatistics() && varstat) {
      LOG.statistics(new DoubleStatistic(this.getClass().getName() + ".variance-sum", totalvariance));
    }
    return result;
  }

  /**
   * Reassign objects, but avoid unnecessary computations based on their bounds.
   *
   * @param relation Data
   * @param means Current means
   * @param sums New cluster sum
   * @param clusters Current clusters
   * @param assignment Cluster assignment
   * @param upper Upper bounds
   * @param lower Lower bounds
   * @return true when the object was reassigned
   */
  private int assignToNearestCluster(Relation<V> relation, double[][] means, double[][] sums, List<ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, WritableDoubleDataStore upper, WritableDataStore<double[]> lower) {
    assert (k == means.length);
    final boolean issquared = distanceFunction.isSquared();
    int changed = 0;
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      final int orig = assignment.intValue(it);
      double u = upper.doubleValue(it);
      boolean recompute_u = true; // Elkan's r(x)
      V fv = relation.get(it);
      double[] l = lower.get(it);
      // Check all (other) means:
      int cur = orig;
      for(int j = 0; j < k; j++) {
        if(orig == j || u <= l[j]) {
          continue; // Condition #3 i-iii not satisfied
        }
        if(recompute_u) { // Need to update bound? #3a
          u = distanceFunction.distance(fv, DoubleVector.wrap(means[cur]));
          u = issquared ? FastMath.sqrt(u) : u;
          upper.putDouble(it, u);
          recompute_u = false; // Once only
          if(u <= l[j]) { // #3b
            continue;
          }
        }
        double dist = distanceFunction.distance(fv, DoubleVector.wrap(means[j]));
        dist = issquared ? FastMath.sqrt(dist) : dist;
        l[j] = dist;
        if(dist < u) {
          cur = j;
          u = dist;
        }
      }
      // Object is to be reassigned.
      if(cur != orig) {
        upper.putDouble(it, u); // Remember bound.
        clusters.get(cur).add(it);
        clusters.get(orig).remove(it);
        assignment.putInt(it, cur);
        plusMinusEquals(sums[cur], sums[orig], fv);
        ++changed;
      }
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
  public static class Parameterizer<V extends NumberVector> extends KMeansElkan.Parameterizer<V> {
    @Override
    protected Logging getLogger() {
      return LOG;
    }

    @Override
    protected KMeansSimplifiedElkan<V> makeInstance() {
      return new KMeansSimplifiedElkan<>(distanceFunction, k, maxiter, initializer, varstat);
    }
  }
}
