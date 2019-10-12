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
package elki.index.preprocessed.knn;

import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MeanVariance;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 *
 * Used for example by {@link elki.outlier.lof.LOF}.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @param <O> the type of database objects the preprocessor can be applied to
 */
@Title("Partitioning Approximate kNN Preprocessor")
@Description("Caterializes the (approximate) k nearest neighbors of objects of a database by partitioning and only computing kNN within each partition.")
public class PartitionApproximationMaterializeKNNPreprocessor<O> extends AbstractMaterializeKNNPreprocessor<O> {
  /**
   * Logger to use
   */
  private static final Logging LOG = Logging.getLogger(PartitionApproximationMaterializeKNNPreprocessor.class);

  /**
   * Number of partitions to use.
   */
  private final int partitions;

  /**
   * Random generator
   */
  private final RandomFactory rnd;

  /**
   * Constructor
   *
   * @param relation Relation to process
   * @param distance the distance function to use
   * @param k query k
   * @param partitions Number of partitions
   * @param rnd Random number generator
   */
  public PartitionApproximationMaterializeKNNPreprocessor(Relation<O> relation, Distance<? super O> distance, int k, int partitions, RandomFactory rnd) {
    super(relation, distance, k);
    this.partitions = partitions;
    this.rnd = rnd;
  }

  @Override
  protected void preprocess() {
    DistanceQuery<O> distanceQuery = new QueryBuilder<>(relation, distance).distanceQuery();
    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, KNNList.class);
    MeanVariance ksize = new MeanVariance();
    if(LOG.isVerbose()) {
      LOG.verbose("Approximating nearest neighbor lists to database objects");
    }

    // Produce a random shuffling of the IDs:
    ArrayDBIDs[] parts = DBIDUtil.randomSplit(relation.getDBIDs(), partitions, rnd);

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Processing partitions", partitions, LOG) : null;
    for(int part = 0; part < partitions; part++) {
      final ArrayDBIDs ids = parts[part];
      final int size = ids.size();
      Object2DoubleOpenHashMap<DBIDPair> cache = new Object2DoubleOpenHashMap<>((size * size * 3) >> 3);
      cache.defaultReturnValue(Double.NaN);
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        KNNHeap kNN = DBIDUtil.newHeap(k);
        for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
          DBIDPair key = DBIDUtil.newPair(iter, iter2);
          double d = cache.removeDouble(key);
          if(d == d) { // Not NaN
            // consume the previous result.
            kNN.insert(d, iter2);
          }
          else {
            // compute new and store the previous result.
            d = distanceQuery.distance(iter, iter2);
            kNN.insert(d, iter2);
            // put it into the cache, but with the keys reversed
            key = DBIDUtil.newPair(iter2, iter);
            cache.put(key, d);
          }
        }
        ksize.put(kNN.size());
        storage.put(iter, kNN.toKNNList());
      }
      if(LOG.isDebugging() && cache.size() > 0) {
        LOG.warning("Cache should be empty after each run, but still has " + cache.size() + " elements.");
      }
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);
    if(LOG.isVerbose()) {
      LOG.verbose("On average, " + ksize.getMean() + " +- " + ksize.getSampleStddev() + " neighbors returned.");
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public String getLongName() {
    return "Random partition kNN approximation";
  }

  @Override
  public String getShortName() {
    return "random-partition-knn";
  }

  @Override
  public void logStatistics() {
    // No statistics to log.
  }

  /**
   * The parameterizable factory.
   *
   * @author Erich Schubert
   *
   * @stereotype factory
   * @navassoc - creates - PartitionApproximationMaterializeKNNPreprocessor
   *
   * @param <O> The object type
   */
  public static class Factory<O> extends AbstractMaterializeKNNPreprocessor.Factory<O> {
    /**
     * The number of partitions to use
     */
    int partitions;

    /**
     * Random generator
     */
    private final RandomFactory rnd;

    /**
     * Constructor.
     *
     * @param k k
     * @param distance distance function
     * @param partitions number of partitions
     * @param rnd
     */
    public Factory(int k, Distance<? super O> distance, int partitions, RandomFactory rnd) {
      super(k, distance);
      this.partitions = partitions;
      this.rnd = rnd;
    }

    @Override
    public PartitionApproximationMaterializeKNNPreprocessor<O> instantiate(Relation<O> relation) {
      PartitionApproximationMaterializeKNNPreprocessor<O> instance = new PartitionApproximationMaterializeKNNPreprocessor<>(relation, distance, k, partitions, rnd);
      return instance;
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
     */
    public static class Par<O> extends AbstractMaterializeKNNPreprocessor.Factory.Par<O> {
      /**
       * Parameter to specify the number of partitions to use for materializing
       * the kNN. Must be an integer greater than 1.
       */
      public static final OptionID PARTITIONS_ID = new OptionID("partknn.p", "The number of partitions to use for approximate kNN.");

      /**
       * Parameter to specify the random number generator.
       */
      public static final OptionID SEED_ID = new OptionID("partknn.seed", "The random number generator seed.");

      /**
       * Number of partitions
       */
      protected int partitions = 0;

      /**
       * Random generator
       */
      private RandomFactory rnd;

      @Override
      public void configure(Parameterization config) {
        super.configure(config);
        new IntParameter(PARTITIONS_ID) //
            .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
            .grab(config, x -> partitions = x);
        new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
      }

      @Override
      public Factory<O> make() {
        return new Factory<>(k, distance, partitions, rnd);
      }
    }
  }
}
