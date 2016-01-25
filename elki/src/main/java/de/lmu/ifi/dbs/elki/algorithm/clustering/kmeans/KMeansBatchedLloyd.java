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
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * An algorithm for k-means, using Lloyd-style bulk iterations.
 *
 * However, in contrast to Lloyd's k-means and similar to MacQueen, we do update
 * the mean vectors multiple times, not only at the very end of the iteration.
 * This should yield faster convergence at little extra cost.
 *
 * To avoid issues with ordered data, we use random sampling to obtain the data
 * blocks.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @apiviz.has KMeansModel
 *
 * @param <V> vector datatype
 */
public class KMeansBatchedLloyd<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansBatchedLloyd.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = KMeansBatchedLloyd.class.getName();

  /**
   * Number of blocks to use.
   */
  int blocks;

  /**
   * Random used for partitioning.
   */
  RandomFactory random;

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   * @param blocks Number of blocks
   * @param random Random factory used for partitioning.
   */
  public KMeansBatchedLloyd(NumberVectorDistanceFunction<? super V> distanceFunction, int k, int maxiter, KMeansInitialization<? super V> initializer, int blocks, RandomFactory random) {
    super(distanceFunction, k, maxiter, initializer);
    this.blocks = blocks;
    this.random = random;
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    final int dim = RelationUtil.dimensionality(relation);
    // Choose initial means
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(KEY + ".initializer", initializer.toString()));
    }
    List<Vector> means = initializer.chooseInitialMeans(database, relation, k, getDistanceFunction(), Vector.FACTORY);

    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet((int) (relation.size() * 2. / k)));
    }
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);

    ArrayDBIDs[] parts = DBIDUtil.randomSplit(relation.getDBIDs(), blocks, random);

    double[][] meanshift = new double[k][dim];
    int[] changesize = new int[k];
    double[] varsum = new double[k];

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("K-Means iteration", LOG) : null;
    DoubleStatistic varstat = LOG.isStatistics() ? new DoubleStatistic(this.getClass().getName() + ".variance-sum") : null;
    int iteration = 0;
    for(; maxiter <= 0 || iteration < maxiter; iteration++) {
      LOG.incrementProcessed(prog);
      boolean changed = false;
      FiniteProgress pprog = LOG.isVerbose() ? new FiniteProgress("Batch", parts.length, LOG) : null;
      for(int p = 0; p < parts.length; p++) {
        // Initialize new means scratch space.
        for(int i = 0; i < k; i++) {
          Arrays.fill(meanshift[i], 0.);
        }
        Arrays.fill(changesize, 0);
        Arrays.fill(varsum, 0.);
        changed |= assignToNearestCluster(relation, parts[p], means, meanshift, changesize, clusters, assignment, varsum);
        // Recompute means.
        updateMeans(means, meanshift, clusters, changesize);
        LOG.incrementProcessed(pprog);
      }
      LOG.ensureCompleted(pprog);
      logVarstat(varstat, varsum);
      // Stop if no cluster assignment changed.
      if(!changed) {
        break;
      }
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
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
   *
   * @param relation the database to cluster
   * @param ids IDs to process
   * @param oldmeans a list of k means
   * @param meanshift delta to apply to each mean
   * @param changesize New cluster sizes
   * @param clusters cluster assignment
   * @param assignment Current cluster assignment
   * @param varsum Sum of variances
   * @return true when the object was reassigned
   */
  protected boolean assignToNearestCluster(Relation<V> relation, DBIDs ids, List<? extends NumberVector> oldmeans, double[][] meanshift, int[] changesize, List<? extends ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, double[] varsum) {
    boolean changed = false;

    final NumberVectorDistanceFunction<? super V> df = getDistanceFunction();
    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      double mindist = Double.POSITIVE_INFINITY;
      V fv = relation.get(iditer);
      int minIndex = 0;
      for(int i = 0; i < k; i++) {
        double dist = df.distance(fv, oldmeans.get(i));
        if(dist < mindist) {
          minIndex = i;
          mindist = dist;
        }
      }
      varsum[minIndex] += mindist;
      changed |= updateAssignment(iditer, fv, clusters, assignment, meanshift, changesize, minIndex);
    }
    return changed;
  }

  /**
   * Update the assignment of a single object.
   *
   * @param id Object to assign
   * @param fv Vector
   * @param clusters Clusters
   * @param assignment Current cluster assignment
   * @param meanshift Current shifting offset
   * @param changesize Size change of the current cluster
   * @param minIndex Index of best cluster.
   * @return {@code true} when assignment changed.
   */
  protected boolean updateAssignment(DBIDIter id, V fv, List<? extends ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, double[][] meanshift, int[] changesize, int minIndex) {
    int cur = assignment.intValue(id);
    if(cur == minIndex) {
      return false;
    }
    // Add to new cluster.
    {
      clusters.get(minIndex).add(id);
      changesize[minIndex]++;
      double[] raw = meanshift[minIndex];
      for(int j = 0; j < fv.getDimensionality(); j++) {
        raw[j] += fv.doubleValue(j);
      }
    }
    // Remove from previous cluster
    if(cur >= 0) {
      clusters.get(cur).remove(id);
      changesize[cur]--;
      double[] raw = meanshift[cur];
      for(int j = 0; j < fv.getDimensionality(); j++) {
        raw[j] -= fv.doubleValue(j);
      }
    }
    assignment.putInt(id, minIndex);
    return true;
  }

  /**
   * Merge changes into mean vectors.
   *
   * @param means Mean vectors
   * @param meanshift Shift offset
   * @param clusters
   * @param changesize Size of change (for weighting!)
   */
  protected void updateMeans(List<Vector> means, double[][] meanshift, List<ModifiableDBIDs> clusters, int[] changesize) {
    for(int i = 0; i < k; i++) {
      int newsize = clusters.get(i).size(), oldsize = newsize - changesize[i];
      if(newsize == 0) {
        continue; // Keep previous mean vector.
      }
      if(oldsize == 0) {
        means.set(i, new Vector(meanshift[i]).times(1. / newsize));
        continue; // Replace with new vector.
      }
      if(oldsize == newsize) {
        means.get(i).plusTimesEquals(new Vector(meanshift[i]), 1. / (double) newsize);
        continue;
      }
      means.get(i).timesEquals(oldsize / (double) newsize).plusTimesEquals(new Vector(meanshift[i]), 1. / (double) newsize);
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
     * Parameter for the number of blocks.
     */
    public static final OptionID BLOCKS_ID = new OptionID("kmeans.blocks", "Number of blocks to use for processing. Means will be recomputed after each block.");

    /**
     * Random source for blocking.
     */
    public static final OptionID RANDOM_ID = new OptionID("kmeans.blocks.random", "Random source for producing blocks.");

    /**
     * Number of blocks.
     */
    int blocks;

    /**
     * Random used for partitioning.
     */
    RandomFactory random;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter blocksP = new IntParameter(BLOCKS_ID, 10);
      blocksP.addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(blocksP)) {
        blocks = blocksP.intValue();
      }
      RandomParameter randomP = new RandomParameter(RANDOM_ID);
      if(config.grab(randomP)) {
        random = randomP.getValue();
      }
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }

    @Override
    protected KMeansBatchedLloyd<V> makeInstance() {
      return new KMeansBatchedLloyd<>(distanceFunction, k, maxiter, initializer, blocks, random);
    }
  }
}
