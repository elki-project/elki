package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Hamerly's fast k-means by exploiting the triangle inequality.
 * 
 * <p>
 * Reference:<br />
 * G. Hamerly<br/>
 * Making k-means even faster<br/>
 * Proc. 2010 SIAM International Conference on Data Mining
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has KMeansModel
 * 
 * @param <V> vector datatype
 */
@Reference(authors = "G. Hamerly", //
title = "Making k-means even faster", //
booktitle = "Proc. 2010 SIAM International Conference on Data Mining", //
url = "http://dx.doi.org/10.1137/1.9781611972801.12")
public class KMeansHamerly<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansHamerly.class);

  /**
   * Constructor.
   * 
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   */
  public KMeansHamerly(int k, int maxiter, KMeansInitialization<? super V> initializer) {
    super(SquaredEuclideanDistanceFunction.STATIC, k, maxiter, initializer);
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    if(relation.size() <= 0) {
      return new Clustering<>("k-Means Clustering", "kmeans-clustering");
    }
    // Choose initial means
    List<Vector> means = initializer.chooseInitialMeans(database, relation, k, getDistanceFunction(), Vector.FACTORY);
    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet((int) (relation.size() * 2. / k)));
    }
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
    // Hamerly bounds
    WritableDoubleDataStore upper = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.POSITIVE_INFINITY);
    WritableDoubleDataStore lower = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0.);

    double[] sep = new double[k];

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Means iteration", LOG) : null;
    LongStatistic varstat = LOG.isStatistics() ? new LongStatistic(this.getClass().getName() + ".reassignments") : null;
    for(int iteration = 0; maxiter <= 0 || iteration < maxiter; iteration++) {
      LOG.incrementProcessed(prog);
      recomputeSeperation(means, sep);
      int changed = assignToNearestCluster(relation, means, clusters, assignment, sep, upper, lower);
      if(varstat != null) {
        varstat.setLong(changed);
        LOG.statistics(varstat);
      }
      // Stop if no cluster assignment changed.
      if(changed == 0) {
        break;
      }
      // Recompute means.
      List<Vector> newmeans = means(clusters, means, relation);
      double delta = maxMoved(means, newmeans, sep);
      means = newmeans;
      updateBounds(relation, assignment, upper, lower, sep, delta);
    }
    LOG.setCompleted(prog);

    // Wrap result
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
      KMeansModel model = new KMeansModel(mean, varsum);
      result.addToplevelCluster(new Cluster<>(ids, model));
    }
    return result;
  }

  /**
   * Recompute the separation of cluster means.
   * 
   * @param means Means
   * @param sep Output array
   */
  private void recomputeSeperation(List<Vector> means, double[] sep) {
    final int k = means.size();
    assert (sep.length == k);
    assert (distanceFunction instanceof SquaredEuclideanDistanceFunction);
    Arrays.fill(sep, Double.POSITIVE_INFINITY);
    for(int i = 1; i < k; i++) {
      Vector m1 = means.get(i);
      for(int j = 0; j < i; j++) {
        double d = distanceFunction.distance(m1, means.get(j));
        sep[i] = (d < sep[i]) ? d : sep[i];
        sep[j] = (d < sep[j]) ? d : sep[j];
      }
    }
    // We need half the Euclidean distance
    for(int i = 0; i < k; i++) {
      sep[i] = Math.sqrt(sep[i]) * .5;
    }
  }

  /**
   * Reassign objects, but only if their bounds indicate it is necessary to do
   * so.
   * 
   * @param relation Data
   * @param means Current means
   * @param clusters Current clusters
   * @param assignment Cluster assignment
   * @param sep Separation of means
   * @param upper Upper bounds
   * @param lower Lower bounds
   * @return true when the object was reassigned
   */
  private int assignToNearestCluster(Relation<V> relation, List<Vector> means, List<ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, double[] sep, WritableDoubleDataStore upper, WritableDoubleDataStore lower) {
    assert (k == means.size());
    int changed = 0;
    final PrimitiveDistanceFunction<? super NumberVector> df = getDistanceFunction();
    assert (df instanceof SquaredEuclideanDistanceFunction);
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      final int cur = assignment.intValue(it);
      // Compute the current bound:
      double z = lower.doubleValue(it);
      double sa = (cur >= 0) ? sep[cur] : 0.;
      z = (sa > z) ? sa : z;
      double u = upper.doubleValue(it);
      if(u <= z) {
        continue;
      }
      // Update the upper bound
      V fv = relation.get(it);
      if(cur >= 0) {
        u = Math.sqrt(df.distance(fv, means.get(cur)));
        upper.putDouble(it, u);
        if(u <= z) {
          continue;
        }
      }
      // Find closest center, and distance to two closest centers
      double min1 = Double.POSITIVE_INFINITY, min2 = Double.POSITIVE_INFINITY;
      int minIndex = -1;
      for(int i = 0; i < k; i++) {
        double dist = df.distance(fv, means.get(i));
        if(dist < min1) {
          minIndex = i;
          min2 = min1;
          min1 = dist;
        }
        else if(dist < min2) {
          min2 = dist;
        }
      }
      if(minIndex != cur) {
        clusters.get(minIndex).add(it);
        assignment.putInt(it, minIndex);
        if(cur >= 0) {
          clusters.get(cur).remove(it);
        }
        ++changed;
        upper.putDouble(it, Math.sqrt(min1));
      }
      lower.putDouble(it, Math.sqrt(min2));
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
    assert (distanceFunction instanceof SquaredEuclideanDistanceFunction);
    double max = 0.;
    for(int i = 0; i < k; i++) {
      double d = dists[i] = Math.sqrt(distanceFunction.distance(means.get(i), newmeans.get(i)));
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
   * @param delta Maximum center movement.
   */
  private void updateBounds(Relation<V> relation, WritableIntegerDataStore assignment, WritableDoubleDataStore upper, WritableDoubleDataStore lower, double[] move, double delta) {
    delta = -delta;
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      upper.increment(it, move[assignment.intValue(it)]);
      lower.increment(it, delta);
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
    @Override
    protected Logging getLogger() {
      return LOG;
    }

    @Override
    protected void makeOptions(Parameterization config) {
      // Would make distance parameterizable: super.makeOptions(config);
      getParameterK(config);
      getParameterInitialization(config);
      getParameterMaxIter(config);
    }

    @Override
    protected KMeansHamerly<V> makeInstance() {
      return new KMeansHamerly<>(k, maxiter, initializer);
    }
  }
}
