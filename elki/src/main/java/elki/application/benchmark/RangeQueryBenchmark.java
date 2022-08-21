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
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.database.Database;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
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
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;
import elki.workflow.InputStep;

/**
 * Benchmarking algorithm that computes a range query for each point. The query
 * points can either come from a separate data source, or from the original
 * database. In the latter case, the database is expected to have an additional,
 * 1-dimensional vector field. For the separate data source, the last dimension
 * will be cut off and used as query radius.
 * <p>
 * The simplest data setup clearly is to have an input file:
 *
 * <pre>
 * x y z label
 * 1 2 3 Example1
 * 4 5 6 Example2
 * 7 8 9 Example3
 * </pre>
 *
 * and a query file:
 * 
 * <pre>
 * x y z radius
 * 1 2 3 1.2
 * 4 5 6 3.3
 * 7 8 9 4.1
 * </pre>
 *
 * where the additional column is the radius.
 * <p>
 * Alternatively, if you work with a single file, you need to use the filter
 * command <tt>-dbc.filter SplitNumberVectorFilter -split.dims 1,2,3</tt> to
 * split the relation into a 3-dimensional data vector, and 1 dimensional radius
 * vector.
 * <p>
 * TODO: alternatively, allow using a fixed radius?
 * <p>
 * TODO: use an InputStream instead of a DatabaseConnection for the query set?
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @param <O> Vector type
 *
 * @assoc - - - RangeSearcher
 */
public class RangeQueryBenchmark<O extends NumberVector> extends AbstractDistanceBasedApplication<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(RangeQueryBenchmark.class);

  /**
   * Query radius.
   */
  protected double radius = Double.NaN;

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
   * @param input Data input
   * @param distance Distance function to use
   * @param radius Query radius to use
   * @param sampling Sampling rate
   * @param random Random factory
   */
  public RangeQueryBenchmark(InputStep input, Distance<? super O> distance, double radius, double sampling, RandomFactory random) {
    super(input, distance);
    this.radius = radius;
    this.sampling = sampling;
    this.random = random;
  }

  /**
   * Constructor.
   *
   * @param input Data input
   * @param distance Distance function to use
   * @param queries Query data set (may be null!)
   * @param sampling Sampling rate
   * @param random Random factory
   */
  public RangeQueryBenchmark(InputStep input, Distance<? super O> distance, DatabaseConnection queries, double sampling, RandomFactory random) {
    super(input, distance);
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
    MeanVariance mv = new MeanVariance(); // result statistics to collect.
    if(!Double.isNaN(radius)) {
      RangeSearcher<DBIDRef> rangeQuery = new QueryBuilder<>(relation, distance).rangeByDBID(radius);
      logIndexStatistics(database);
      hash = run(rangeQuery, relation, radius, dur, mv);
    }
    else if(queries != null) {
      RangeSearcher<O> rangeQuery = new QueryBuilder<>(relation, distance).rangeByObject();
      logIndexStatistics(database);
      hash = run(rangeQuery, relation, queries, dur, mv);
    }
    else {
      RangeSearcher<DBIDRef> rangeQuery = new QueryBuilder<>(relation, distance).rangeByDBID();
      logIndexStatistics(database);
      // Get a query radius relation:
      Relation<NumberVector> qrad = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_1D);
      hash = run(rangeQuery, relation, qrad, dur, mv);
    }
    LOG.statistics(dur.end());
    if(dur instanceof MillisTimeDuration) {
      LOG.statistics(new StringStatistic(key + ".duration.avg", dur.getDuration() / mv.getCount() * 1000. + " ns"));
    }
    LOG.statistics(new DoubleStatistic(key + ".results.mean", mv.getMean()));
    LOG.statistics(new DoubleStatistic(key + ".results.std", mv.getPopulationStddev()));
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
   * Run the algorithm, with constant radius
   *
   * @param rangeQuery query to test
   * @param relation Relation
   * @param radius Radius
   * @param mv Mean and variance statistics
   * @return hash code over all results
   */
  protected int run(RangeSearcher<DBIDRef> rangeQuery, Relation<O> relation, double radius, Duration dur, MeanVariance mv) {
    final DBIDs sample = DBIDUtil.randomSample(relation.getDBIDs(), sampling, random);
    int hash = 0;
    FiniteProgress prog = LOG.isVeryVerbose() ? new FiniteProgress("kNN queries", sample.size(), LOG) : null;
    dur.begin();
    for(DBIDIter iditer = sample.iter(); iditer.valid(); iditer.advance()) {
      DoubleDBIDList rres = rangeQuery.getRange(iditer, radius);
      hash = Util.mixHashCodes(hash, processResult(rres, mv));
      LOG.incrementProcessed(prog);
    }
    dur.end();
    LOG.ensureCompleted(prog);
    return hash;
  }

  /**
   * Run the algorithm, with separate radius relation
   *
   * @param rangeQuery query to test
   * @param relation Relation
   * @param radrel Radius relation
   * @param mv Mean and variance statistics
   * @return hash code over all results
   */
  protected int run(RangeSearcher<DBIDRef> rangeQuery, Relation<O> relation, Relation<NumberVector> radrel, Duration dur, MeanVariance mv) {
    final DBIDs sample = DBIDUtil.randomSample(relation.getDBIDs(), sampling, random);
    int hash = 0;
    FiniteProgress prog = LOG.isVeryVerbose() ? new FiniteProgress("kNN queries", sample.size(), LOG) : null;
    dur.begin();
    for(DBIDIter iditer = sample.iter(); iditer.valid(); iditer.advance()) {
      DoubleDBIDList rres = rangeQuery.getRange(iditer, radrel.get(iditer).doubleValue(0));
      hash = Util.mixHashCodes(hash, processResult(rres, mv));
      LOG.incrementProcessed(prog);
    }
    dur.end();
    LOG.ensureCompleted(prog);
    return hash;
  }

  /**
   * Run the algorithm, with a separate query set.
   *
   * @param rangeQuery query to test
   * @param relation Relation
   * @param queries Queries database connection
   * @param mv Statistics output
   * @return result hashcode
   */
  protected int run(RangeSearcher<O> rangeQuery, Relation<O> relation, DatabaseConnection queries, Duration dur, MeanVariance mv) {
    NumberVector.Factory<O> ofactory = RelationUtil.getNumberVectorFactory(relation);
    int dim = RelationUtil.dimensionality(relation);

    // Separate query set.
    TypeInformation res = VectorFieldTypeInformation.typeRequest(NumberVector.class, dim + 1, dim + 1);
    MultipleObjectsBundle bundle = queries.loadData();
    int col = -1;
    for(int i = 0; i < bundle.metaLength(); i++) {
      if(res.isAssignableFromType(bundle.meta(i))) {
        col = i;
        break;
      }
    }
    if(col < 0) {
      StringBuilder buf = new StringBuilder(1000) //
          .append("No compatible data type in query input was found. Expected: ") //
          .append(res.toString()).append(" have:");
      for(int i = 0; i < bundle.metaLength(); i++) {
        buf.append(' ').append(bundle.meta(i).toString());
      }
      throw new IncompatibleDataException(buf.toString());
    }
    // Random sampling from the query data set:
    int[] sample = MathUtil.sequence(0, bundle.dataLength());
    int samplesize = (int) (sampling <= 1 ? sampling * sample.length : sampling);
    ArrayUtil.randomShuffle(sample, random.getSingleThreadedRandom(), samplesize);
    sample = Arrays.copyOf(sample, samplesize);

    FiniteProgress prog = LOG.isVeryVerbose() ? new FiniteProgress("kNN queries", sample.length, LOG) : null;
    int hash = 0;
    dur.begin();
    double[] buf = new double[dim];
    for(int off : sample) {
      // Split query data into object + radius
      NumberVector o = (NumberVector) bundle.data(off, col);
      for(int i = 0; i < dim; i++) {
        buf[i] = o.doubleValue(i);
      }
      DoubleDBIDList rres = rangeQuery.getRange(ofactory.newNumberVector(buf), o.doubleValue(dim));
      hash = Util.mixHashCodes(hash, processResult(rres, mv));
      LOG.incrementProcessed(prog);
    }
    dur.end();
    LOG.ensureCompleted(prog);
    return hash;
  }

  /**
   * Method to test a result.
   *
   * @param rres Result to process
   * @param mv Statistics output
   * @return hash code
   */
  protected int processResult(DoubleDBIDList rres, MeanVariance mv) {
    mv.put(rres.size());
    int ichecksum = 0;
    for(DBIDIter it = rres.iter(); it.valid(); it.advance()) {
      // intentionally NOT order dependent, because of ties:
      ichecksum += DBIDUtil.asInteger(it);
    }
    return ichecksum;
  }

  /**
   * Runs the benchmark
   * 
   * @param args parameter list according to description
   */
  public static void main(String[] args) {
    LoggingConfiguration.setDefaultLevel(Level.STATISTICS);
    runCLIApplication(RangeQueryBenchmark.class, args);
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
  public static class Par<O extends NumberVector> extends AbstractDistanceBasedApplication.Par<O> {
    /**
     * Parameter for the query radius
     */
    public static final OptionID RADIUS_ID = new OptionID("rangebench.radius", "Query radius to use a constant radius.");

    /**
     * Parameter for the query data set.
     */
    public static final OptionID QUERY_ID = new OptionID("rangebench.query", "Data source for the queries. If not set, the queries are taken from the database.");

    /**
     * Parameter for the sampling size.
     */
    public static final OptionID SAMPLING_ID = new OptionID("rangebench.sampling", "Sampling size parameter. If the value is less or equal 1, it is assumed to be the relative share. Larger values will be interpreted as integer sizes. By default, all data will be used.");

    /**
     * Parameter for the random generator
     */
    public static final OptionID RANDOM_ID = new OptionID("rangebench.random", "Random generator for sampling.");

    /**
     * The alternate query point source. Optional.
     */
    protected DatabaseConnection queries = null;

    /**
     * Sampling size.
     */
    protected double sampling = -1;

    /**
     * Query radius.
     */
    protected double radius = Double.NaN;

    /**
     * Random generator factory
     */
    protected RandomFactory random;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new DoubleParameter(RADIUS_ID) //
          .setOptional(true) //
          .grab(config, x -> radius = x);
      if(Double.isNaN(radius)) {
        new ObjectParameter<DatabaseConnection>(QUERY_ID, DatabaseConnection.class) //
            .setOptional(true) //
            .grab(config, x -> queries = x);
      }
      new DoubleParameter(SAMPLING_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .setOptional(true) //
          .grab(config, x -> sampling = x);
      new RandomParameter(RANDOM_ID, RandomFactory.DEFAULT).grab(config, x -> random = x);
    }

    @Override
    public RangeQueryBenchmark<O> make() {
      return Double.isNaN(radius) //
          ? new RangeQueryBenchmark<>(inputstep, distance, queries, sampling, random) //
          : new RangeQueryBenchmark<>(inputstep, distance, radius, sampling, random);
    }
  }
}
