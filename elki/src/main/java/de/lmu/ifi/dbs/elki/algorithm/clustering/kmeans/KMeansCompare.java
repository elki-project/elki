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
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Compare-Means: Accelerated k-means by exploiting the triangle inequality and
 * pairwise distances of means to prune candidate means.
 *
 * Reference:
 * <p>
 * S. J. Phillips<br />
 * Acceleration of k-means and related clustering algorithms<br />
 * Proc. 4th Int. Workshop on Algorithm Engineering and Experiments (ALENEX
 * 2002)
 * </p>
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @apiviz.has KMeansModel
 *
 * @param <V> vector datatype
 */
@Title("Compare-Means")
@Reference(authors = "S. J. Phillips", //
title = "Acceleration of k-means and related clustering algorithms", //
booktitle = "Proc. 4th Int. Workshop on Algorithm Engineering and Experiments (ALENEX 2002)", //
url = "http://dx.doi.org/10.1007/3-540-45643-0_13")
public class KMeansCompare<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansCompare.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMeansCompare.class.getName();

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   */
  public KMeansCompare(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization<? super V> initializer) {
    super(distanceFunction, k, maxiter, initializer);
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
    double[] varsum = new double[k];

    // Cluster distances
    double[][] cdist = new double[k][k];

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Means iteration", LOG) : null;
    DoubleStatistic varstat = LOG.isStatistics() ? new DoubleStatistic(this.getClass().getName() + ".variance-sum") : null;
    LongStatistic diststat = LOG.isStatistics() ? new LongStatistic(KEY + ".distance-computations") : null;
    int iteration = 0;
    for(; maxiter <= 0 || iteration < maxiter; iteration++) {
      LOG.incrementProcessed(prog);
      recomputeSeperation(means, cdist, diststat);
      boolean changed = assignToNearestCluster(relation, means, clusters, assignment, varsum, cdist, diststat);
      logVarstat(varstat, varsum);
      if(LOG.isStatistics()) {
        LOG.statistics(diststat);
      }
      // Stop if no cluster assignment changed.
      if(!changed) {
        break;
      }
      // Recompute means.
      means = means(clusters, means, relation);
    }
    LOG.setCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
    }

    // Wrap result
    Clustering<KMeansModel> result = new Clustering<>("k-Means Clustering", "kmeans-clustering");
    for(int i = 0; i < clusters.size(); i++) {
      DBIDs ids = clusters.get(i);
      if(ids.size() == 0) {
        continue;
      }
      KMeansModel model = new KMeansModel(means.get(i), varsum[i]);
      result.addToplevelCluster(new Cluster<>(ids, model));
    }
    return result;
  }

  /**
   * Recompute the separation of cluster means.
   *
   * @param means Means
   * @param cdist Center-to-Center distances
   * @param diststat Distance counting statistic
   */
  private void recomputeSeperation(List<Vector> means, double[][] cdist, LongStatistic diststat) {
    final int k = means.size();
    for(int i = 1; i < k; i++) {
      Vector mi = means.get(i);
      for(int j = 0; j < i; j++) {
        double d = distanceFunction.distance(mi, means.get(j));
        cdist[i][j] = d;
        cdist[j][i] = d;
      }
    }
    if(diststat != null) {
      diststat.increment((k * (k - 1)) >> 1);
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
   * @param varsum Variance sum counter
   * @param cdist Centroid distances
   * @param diststat Distance statistics
   * @return true when the object was reassigned
   */
  private boolean assignToNearestCluster(Relation<V> relation, List<Vector> means, List<ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, double[] varsum, double[][] cdist, LongStatistic diststat) {
    assert (k == means.size());
    long dists = 0;
    boolean changed = false;
    // Reset all clusters
    Arrays.fill(varsum, 0.);
    for(ModifiableDBIDs cluster : clusters) {
      cluster.clear();
    }
    final NumberVectorDistanceFunction<?> df = getDistanceFunction();
    double mult = (df instanceof SquaredEuclideanDistanceFunction) ? 4 : 2;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final int cur = assignment.intValue(iditer), ini = cur >= 0 ? cur : 0;
      // Distance to current mean:
      V fv = relation.get(iditer);
      double mindist = df.distance(fv, means.get(ini));
      ++dists;
      final double thresh = mult * mindist;
      int minIndex = ini;
      for(int i = 0; i < k; i++) {
        if(i == ini || cdist[minIndex][i] >= thresh) { // Compare pruning
          continue;
        }
        double dist = df.distance(fv, means.get(i));
        ++dists;
        if(dist < mindist) {
          minIndex = i;
          mindist = dist;
        }
      }
      varsum[minIndex] += mindist;
      clusters.get(minIndex).add(iditer);
      changed |= assignment.putInt(iditer, minIndex) != minIndex;
    }
    // Increment distance computations counter.
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
  public static class Parameterizer<V extends NumberVector> extends AbstractKMeans.Parameterizer<V> {
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
        LOG.warning("Compare k-means requires a metric distance, and k-means should only be used with squared Euclidean distance!");
      }
    }

    @Override
    protected KMeansCompare<V> makeInstance() {
      return new KMeansCompare<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
