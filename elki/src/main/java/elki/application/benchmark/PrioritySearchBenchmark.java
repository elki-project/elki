/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.application.benchmark;

import java.util.Arrays;

import elki.application.AbstractDistanceBasedApplication;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.ids.*;
import elki.database.query.PrioritySearcher;
import elki.database.query.QueryBuilder;
import elki.database.relation.Relation;
import elki.datasource.DatabaseConnection;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.distance.Distance;
import elki.index.Index;
import elki.logging.Logging;
import elki.logging.Logging.Level;
import elki.logging.LoggingConfiguration;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.*;
import elki.math.MathUtil;
import elki.math.MeanVariance;
import elki.result.Metadata;
import elki.utilities.Util;
import elki.utilities.datastructures.arrays.ArrayUtil;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.exceptions.IncompatibleDataException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;
import elki.workflow.InputStep;

/**
 * Benchmarking experiment that computes the k nearest neighbors for each query
 * point. The query points can either come from a separate data source, or from
 * the original database.
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @param <O> Object type
 *
 * @assoc - - - PrioritySearcher
 */
public class PrioritySearchBenchmark<O> extends AbstractDistanceBasedApplication<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(PrioritySearchBenchmark.class);

  /**
   * Number of neighbors to retrieve.
   */
  protected int k = 10;

  /**
   * The alternate query point source. Optional.
   */
  protected DatabaseConnection queries = null;

  /**
   * Sampling size.
   */
  protected double sampling = -1;

  /**
   * Random generator factory
   */
  protected RandomFactory random;

  /**
   * Constructor.
   *
   * @param inputstep Input step
   * @param distance Distance function to use
   * @param k K parameter
   * @param queries Query data set (may be null!)
   * @param sampling Sampling rate
   * @param random Random factory
   */
  public PrioritySearchBenchmark(InputStep inputstep, Distance<? super O> distance, int k, DatabaseConnection queries, double sampling, RandomFactory random) {
    super(inputstep, distance);
    this.k = k;
    this.queries = queries;
    this.sampling = sampling;
    this.random = random;
  }

  @Override
  public void run() {
    if(!LOG.isStatistics()) {
      LOG.error("Logging level should be at least level STATISTICS (parameter -time) to see any output.");
    }
    Database database = inputstep.getDatabase();
    Relation<O> relation = database.getRelation(distance.getInputTypeRestriction());
    final String key = getClass().getName();
    Duration dur = LOG.newDuration(key + ".duration");
    int hash;
    MeanVariance mv = new MeanVariance(), mvdist = new MeanVariance();
    // No query set - use original database.
    if(queries == null) {
      PrioritySearcher<DBIDRef> priQuery = new QueryBuilder<>(relation, distance).priorityByDBID();
      logIndexStatistics(database);
      hash = run(priQuery, relation, dur, mv, mvdist);
    }
    else { // Separate query set.
      PrioritySearcher<O> priQuery = new QueryBuilder<>(relation, distance).priorityByObject(k);
      logIndexStatistics(database);
      hash = run(priQuery, dur, mv, mvdist);
    }
    LOG.statistics(dur.end());
    if(dur instanceof MillisTimeDuration) {
      LOG.statistics(new StringStatistic(key + ".duration.avg", dur.getDuration() / mv.getCount() * 1000. + " ns"));
    }
    LOG.statistics(new DoubleStatistic(key + ".results.mean", mv.getMean()));
    LOG.statistics(new DoubleStatistic(key + ".results.std", mv.getPopulationStddev()));
    LOG.statistics(new DoubleStatistic(key + ".kdist.mean", mvdist.getMean()));
    LOG.statistics(new DoubleStatistic(key + ".kdist.std", mvdist.getPopulationStddev()));
    logIndexStatistics(database);
    LOG.statistics(new LongStatistic(key + ".checksum", hash));
  }

  /**
   * Log index statistics before and after querying.
   * 
   * @param database Database
   */
  private void logIndexStatistics(Database database) {
    for(It<Index> it = Metadata.hierarchyOf(database).iterDescendants().filter(Index.class); it.valid(); it.advance()) {
      it.get().logStatistics();
    }
  }

  /**
   * Run with the database as query source
   *
   * @param priQuery Query object
   * @param relation Input data
   * @param dur Duration
   * @param mv statistics collector
   * @param mvdist statistics collector
   * @return hash code of the results
   */
  private int run(PrioritySearcher<DBIDRef> priQuery, Relation<O> relation, Duration dur, MeanVariance mv, MeanVariance mvdist) {
    int hash = 0;
    final DBIDs sample = DBIDUtil.randomSample(relation.getDBIDs(), sampling, random);
    FiniteProgress prog = LOG.isVeryVerbose() ? new FiniteProgress("kNN queries", sample.size(), LOG) : null;
    dur.begin();
    KNNHeap heap = DBIDUtil.newHeap(k);
    for(DBIDIter iditer = sample.iter(); iditer.valid(); iditer.advance()) {
      heap.clear();
      for(priQuery.search(iditer); priQuery.valid(); priQuery.advance()) {
        heap.insert(priQuery.computeExactDistance(), priQuery);
        if(heap.size() >= k) {
          priQuery.decreaseCutoff(heap.getKNNDistance());
        }
      }
      KNNList knns = heap.toKNNList();
      int ichecksum = 0;
      for(DBIDIter it = knns.iter(); it.valid(); it.advance()) {
        ichecksum += DBIDUtil.asInteger(it);
      }
      hash = Util.mixHashCodes(hash, ichecksum);
      mv.put(knns.size());
      mvdist.put(knns.getKNNDistance());
      LOG.incrementProcessed(prog);
    }
    dur.end();
    LOG.ensureCompleted(prog);
    return hash;
  }

  /**
   * Run using a second database as query source
   *
   * @param priQuery Query object
   * @param dur Duration
   * @param mv statistics collector
   * @param mvdist statistics collector
   * @return hash code of the results
   */
  private int run(PrioritySearcher<O> priQuery, Duration dur, MeanVariance mv, MeanVariance mvdist) {
    int hash = 0;
    TypeInformation res = distance.getInputTypeRestriction();
    MultipleObjectsBundle bundle = queries.loadData();
    int col = -1;
    for(int i = 0; i < bundle.metaLength(); i++) {
      if(res.isAssignableFromType(bundle.meta(i))) {
        col = i;
        break;
      }
    }
    if(col < 0) {
      throw new IncompatibleDataException("No compatible data type in query input was found. Expected: " + res.toString());
    }
    // Random sampling from the query data set:
    int[] sample = MathUtil.sequence(0, bundle.dataLength());
    int samplesize = (int) (sampling <= 1 ? sampling * sample.length : sampling);
    ArrayUtil.randomShuffle(sample, random.getSingleThreadedRandom(), samplesize);
    sample = Arrays.copyOf(sample, samplesize);
    FiniteProgress prog = LOG.isVeryVerbose() ? new FiniteProgress("kNN queries", sample.length, LOG) : null;
    dur.begin();
    KNNHeap heap = DBIDUtil.newHeap(k);
    for(int off : sample) {
      @SuppressWarnings("unchecked")
      O o = (O) bundle.data(off, col);
      heap.clear();
      for(priQuery.search(o); priQuery.valid(); priQuery.advance()) {
        heap.insert(priQuery.computeExactDistance(), priQuery);
        if(heap.size() >= k) {
          priQuery.decreaseCutoff(heap.getKNNDistance());
        }
      }
      KNNList knns = heap.toKNNList();
      int ichecksum = 0;
      for(DBIDIter it = knns.iter(); it.valid(); it.advance()) {
        ichecksum += DBIDUtil.asInteger(it);
      }
      hash = Util.mixHashCodes(hash, ichecksum);
      mv.put(knns.size());
      mvdist.put(knns.getKNNDistance());
      LOG.incrementProcessed(prog);
    }
    dur.end();
    LOG.ensureCompleted(prog);
    return hash;
  }

  /**
   * Runs the benchmark
   * 
   * @param args parameter list according to description
   */
  public static void main(String[] args) {
    LoggingConfiguration.setDefaultLevel(Level.STATISTICS);
    runCLIApplication(PrioritySearchBenchmark.class, args);
  }

  /**
   * Parameterization class
   *
   * @hidden
   *
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  public static class Par<O> extends AbstractDistanceBasedApplication.Par<O> {
    /**
     * Parameter for the number of neighbors.
     */
    public static final OptionID K_ID = new OptionID("prioritybench.k", "Number of neighbors to retreive for kNN benchmarking.");

    /**
     * Parameter for the query dataset.
     */
    public static final OptionID QUERY_ID = new OptionID("prioritybench.query", "Data source for the queries. If not set, the queries are taken from the database.");

    /**
     * Parameter for the sampling size.
     */
    public static final OptionID SAMPLING_ID = new OptionID("prioritybench.sampling", "Sampling size parameter. If the value is less or equal 1, it is assumed to be the relative share. Larger values will be interpreted as integer sizes. By default, all data will be used.");

    /**
     * Parameter for the random generator
     */
    public static final OptionID RANDOM_ID = new OptionID("prioritybench.random", "Random generator for sampling.");

    /**
     * K parameter
     */
    protected int k = 10;

    /**
     * The alternate query point source. Optional.
     */
    protected DatabaseConnection queries = null;

    /**
     * Sampling size.
     */
    protected double sampling = -1;

    /**
     * Random generator factory
     */
    protected RandomFactory random;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new ObjectParameter<DatabaseConnection>(QUERY_ID, DatabaseConnection.class) //
          .setOptional(true) //
          .grab(config, x -> queries = x);
      new DoubleParameter(SAMPLING_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .setOptional(true) //
          .grab(config, x -> sampling = x);
      new RandomParameter(RANDOM_ID, RandomFactory.DEFAULT).grab(config, x -> random = x);
    }

    @Override
    public PrioritySearchBenchmark<O> make() {
      return new PrioritySearchBenchmark<>(inputstep, distance, k, queries, sampling, random);
    }
  }
}
