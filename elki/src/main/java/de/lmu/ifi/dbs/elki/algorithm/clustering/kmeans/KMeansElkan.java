package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Elkan's fast k-means by exploiting the triangle inequality.
 *
 * This variant needs O(n*k) additional memory to store bounds.
 *
 * See {@link KMeansHamerly} for a close variant that only uses O(n*2)
 * additional memory for bounds.
 *
 * <p>
 * Reference:<br />
 * C. Elkan<br/>
 * Using the triangle inequality to accelerate k-means<br/>
 * Proc. 20th International Conference on Machine Learning, ICML 2003
 * </p>
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @apiviz.has KMeansModel
 *
 * @param <V> vector datatype
 */
@Reference(authors = "C. Elkan", //
title = "Using the triangle inequality to accelerate k-means", //
booktitle = "Proc. 20th International Conference on Machine Learning, ICML 2003", //
url = "http://www.aaai.org/Library/ICML/2003/icml03-022.php")
public class KMeansElkan<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansElkan.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMeansElkan.class.getName();

  /**
   * Flag whether to compute the final variance statistic.
   */
  private boolean varstat = false;

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param varstat Compute the variance statistic
   */
  public KMeansElkan(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization<? super V> initializer, boolean varstat) {
    super(distanceFunction, k, maxiter, initializer);
    this.varstat = varstat;
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
    List<Vector> means = initializer.chooseInitialMeans(database, relation, k, getDistanceFunction(), Vector.FACTORY);
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
    final int dim = means.get(0).getDimensionality();
    List<Vector> sums = new ArrayList<>(k);
    for(int i = 0; i < k; i++) {
      sums.add(new Vector(dim));
    }

    // Cluster separation
    double[] sep = new double[k];
    // Cluster distances
    double[][] cdist = new double[k][k];

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
        recomputeSeperation(means, sep, cdist); // #1
        changed = assignToNearestCluster(relation, means, sums, clusters, assignment, sep, cdist, upper, lower);
      }
      if(rstat != null) {
        rstat.setLong(changed);
        LOG.statistics(rstat);
      }
      // Stop if no cluster assignment changed.
      if(changed == 0) {
        break;
      }
      // Recompute means.
      for(int i = 0; i < k; i++) {
        final int s = clusters.get(i).size();
        sums.get(i).timesEquals(s > 0 ? 1. / s : 1.);
      }
      maxMoved(means, sums, sep); // Overwrites sep
      updateBounds(relation, assignment, upper, lower, sep);
      for(int i = 0; i < k; i++) {
        final int s = clusters.get(i).size();
        means.get(i).set(sums.get(i));
        // Restore to sum for next iteration
        sums.get(i).timesEquals(s > 0 ? s : 1.);
      }
    }
    LOG.setCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
    }
    upper.destroy();
    lower.destroy();

    // Wrap result
    double totalvariance = 0.;
    Clustering<KMeansModel> result = new Clustering<>("k-Means Clustering", "kmeans-clustering");
    for(int i = 0; i < clusters.size(); i++) {
      DBIDs ids = clusters.get(i);
      if(ids.size() == 0) {
        continue;
      }
      double varsum = 0;
      Vector mean = means.get(i);
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        varsum += distanceFunction.distance(mean, relation.get(it));
      }
      totalvariance += varsum;
      KMeansModel model = new KMeansModel(mean, varsum);
      result.addToplevelCluster(new Cluster<>(ids, model));
    }
    if(LOG.isStatistics() && varstat) {
      LOG.statistics(new DoubleStatistic(this.getClass().getName() + ".variance-sum", totalvariance));
    }
    return result;
  }

  /**
   * Recompute the separation of cluster means.
   *
   * @param means Means
   * @param sep Output array of separation
   * @param cdist Center-to-Center distances
   */
  private void recomputeSeperation(List<Vector> means, double[] sep, double[][] cdist) {
    final int k = means.size();
    assert (sep.length == k);
    boolean issquared = (distanceFunction instanceof SquaredEuclideanDistanceFunction);
    Arrays.fill(sep, Double.POSITIVE_INFINITY);
    for(int i = 1; i < k; i++) {
      Vector mi = means.get(i);
      for(int j = 0; j < i; j++) {
        double d = distanceFunction.distance(mi, means.get(j));
        d = issquared ? Math.sqrt(d) : d;
        d *= .5;
        cdist[i][j] = d;
        cdist[j][i] = d;
        sep[i] = (d < sep[i]) ? d : sep[i];
        sep[j] = (d < sep[j]) ? d : sep[j];
      }
    }
  }

  /**
   * Reassign objects, but only if their bounds indicate it is necessary to do
   * so.
   *
   * @param relation Data
   * @param means Current means
   * @param sums New means
   * @param clusters Current clusters
   * @param assignment Cluster assignment
   * @param upper Upper bounds
   * @param lower Lower bounds
   * @return Number of changes (i.e. relation size)
   */
  private int initialAssignToNearestCluster(Relation<V> relation, List<Vector> means, List<Vector> sums, List<ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, WritableDoubleDataStore upper, WritableDataStore<double[]> lower) {
    assert (k == means.size());
    final NumberVectorDistanceFunction<? super V> df = getDistanceFunction();
    final boolean issquared = (df instanceof SquaredEuclideanDistanceFunction);
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      V fv = relation.get(it);
      double[] l = lower.get(it);
      // Check all (other) means:
      double best = Double.POSITIVE_INFINITY;
      int cur = -1;
      for(int j = 0; j < k; j++) {
        double dist = df.distance(fv, means.get(j));
        dist = issquared ? Math.sqrt(dist) : dist;
        l[j] = dist;
        if(dist < best) {
          cur = j;
          best = dist;
        }
      }
      // Assign to nearest cluster.
      ModifiableDBIDs newc = clusters.get(cur);
      newc.add(it);
      assignment.putInt(it, cur);
      upper.putDouble(it, best);
      double[] newmean = sums.get(cur).getArrayRef();
      for(int d = 0; d < fv.getDimensionality(); d++) {
        newmean[d] += fv.doubleValue(d);
      }
    }
    return relation.size();
  }

  /**
   * Reassign objects, but only if their bounds indicate it is necessary to do
   * so.
   *
   * @param relation Data
   * @param means Current means
   * @param sums New means
   * @param clusters Current clusters
   * @param assignment Cluster assignment
   * @param sep Separation of means
   * @param cdist Center-to-center distances
   * @param upper Upper bounds
   * @param lower Lower bounds
   * @return true when the object was reassigned
   */
  private int assignToNearestCluster(Relation<V> relation, List<Vector> means, List<Vector> sums, List<ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, double[] sep, double[][] cdist, WritableDoubleDataStore upper, WritableDataStore<double[]> lower) {
    assert (k == means.size());
    final NumberVectorDistanceFunction<? super V> df = getDistanceFunction();
    final boolean issquared = (df instanceof SquaredEuclideanDistanceFunction);
    int changed = 0;
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      final int orig = assignment.intValue(it);
      double u = upper.doubleValue(it);
      // Upper bound check (#2):
      if(u <= sep[orig]) {
        continue;
      }
      boolean recompute_u = true; // Elkan's r(x)
      V fv = relation.get(it);
      double[] l = lower.get(it);
      // Check all (other) means:
      int cur = orig;
      for(int j = 0; j < k; j++) {
        if(orig == j || u <= l[j] || u <= cdist[cur][j]) {
          continue; // Condition #3 i-iii not satisfied
        }
        if(recompute_u) { // Need to update bound? #3a
          u = df.distance(fv, means.get(cur));
          u = issquared ? Math.sqrt(u) : u;
          upper.putDouble(it, u);
          recompute_u = false; // Once only
          if(u <= l[j] || u <= cdist[cur][j]) { // #3b
            continue;
          }
        }
        double dist = df.distance(fv, means.get(j));
        dist = issquared ? Math.sqrt(dist) : dist;
        l[j] = dist;
        if(dist < u) {
          cur = j;
          u = dist;
        }
      }
      // Object is to be reassigned.
      if(cur != orig) {
        upper.putDouble(it, u); // Remember bound.
        ModifiableDBIDs newc = clusters.get(cur);
        newc.add(it);
        assignment.putInt(it, cur);
        double[] newmean = sums.get(cur).getArrayRef();
        ModifiableDBIDs oldc = clusters.get(orig);
        oldc.remove(it);
        double[] oldmean = sums.get(orig).getArrayRef();
        for(int d = 0; d < fv.getDimensionality(); d++) {
          final double v = fv.doubleValue(d);
          newmean[d] += v;
          oldmean[d] -= v;
        }
        ++changed;
      }
    }
    return changed;
  }

  /**
   * Maximum distance moved.
   *
   * @param means Old means
   * @param newmeans New means
   * @param dists Distances moved
   * @return Maximum distance moved
   */
  private double maxMoved(List<Vector> means, List<Vector> newmeans, double[] dists) {
    assert (means.size() == k);
    assert (newmeans.size() == k);
    assert (dists.length == k);
    boolean issquared = (distanceFunction instanceof SquaredEuclideanDistanceFunction);
    double max = 0.;
    for(int i = 0; i < k; i++) {
      double d = distanceFunction.distance(means.get(i), newmeans.get(i));
      d = issquared ? Math.sqrt(d) : d;
      dists[i] = d;
      max = (d > max) ? d : max;
    }
    return max;
  }

  /**
   * Update the bounds for k-means.
   *
   * @param relation Relation
   * @param assignment Cluster assignment
   * @param upper Upper bounds
   * @param lower Lower bounds
   * @param move Movement of centers
   */
  private void updateBounds(Relation<V> relation, WritableIntegerDataStore assignment, WritableDoubleDataStore upper, WritableDataStore<double[]> lower, double[] move) {
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      upper.increment(it, move[assignment.intValue(it)]);
      double[] l = lower.get(it);
      for(int i = 0; i < k; i++) {
        l[i] -= move[i];
      }
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
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractKMeans.Parameterizer<V> {
    /**
     * Flag to compute the final clustering variance statistic.
     */
    public static final OptionID VARSTAT_ID = new OptionID("kmeans.varstat", "Compute the final clustering variance statistic. Needs an additional full pass over the data set.");

    /**
     * Compute the final variance statisic.
     */
    protected boolean varstat = false;

    @Override
    protected Logging getLogger() {
      return LOG;
    }

    @Override
    protected void getParameterDistanceFunction(Parameterization config) {
      super.getParameterDistanceFunction(config);
      if(distanceFunction instanceof SquaredEuclideanDistanceFunction) {
        return; // Proper choice.
      }
      if(distanceFunction != null && !distanceFunction.isMetric()) {
        LOG.warning("Elkan k-means requires a metric distance, and k-means should only be used with squared Euclidean distance!");
      }
    }

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag varF = new Flag(VARSTAT_ID);
      if(config.grab(varF)) {
        varstat = varF.isTrue();
      }
    }

    @Override
    protected KMeansElkan<V> makeInstance() {
      return new KMeansElkan<>(distanceFunction, k, maxiter, initializer, varstat);
    }
  }
}
