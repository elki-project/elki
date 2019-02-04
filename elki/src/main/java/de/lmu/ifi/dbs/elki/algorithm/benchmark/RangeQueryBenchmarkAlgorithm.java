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
package de.lmu.ifi.dbs.elki.algorithm.benchmark;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.IncompatibleDataException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

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
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @param <O> Vector type
 *
 * @assoc - - - RangeQuery
 */
public class RangeQueryBenchmarkAlgorithm<O extends NumberVector> extends AbstractDistanceBasedAlgorithm<O, Result> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(RangeQueryBenchmarkAlgorithm.class);

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
   * @param distanceFunction Distance function to use
   * @param queries Query data set (may be null!)
   * @param sampling Sampling rate
   * @param random Random factory
   */
  public RangeQueryBenchmarkAlgorithm(DistanceFunction<? super O> distanceFunction, DatabaseConnection queries, double sampling, RandomFactory random) {
    super(distanceFunction);
    this.queries = queries;
    this.sampling = sampling;
    this.random = random;
  }

  /**
   * Run the algorithm, with separate radius relation
   *
   * @param database Database
   * @param relation Relation
   * @param radrel Radius relation
   * @return Null result
   */
  public Result run(Database database, Relation<O> relation, Relation<NumberVector> radrel) {
    if(queries != null) {
      throw new AbortException("This 'run' method will not use the given query set!");
    }
    // Get a distance and kNN query instance.
    DistanceQuery<O> distQuery = database.getDistanceQuery(relation, getDistanceFunction());
    RangeQuery<O> rangeQuery = database.getRangeQuery(distQuery);

    final DBIDs sample = DBIDUtil.randomSample(relation.getDBIDs(), sampling, random);
    FiniteProgress prog = LOG.isVeryVerbose() ? new FiniteProgress("kNN queries", sample.size(), LOG) : null;
    int hash = 0;
    MeanVariance mv = new MeanVariance();
    for(DBIDIter iditer = sample.iter(); iditer.valid(); iditer.advance()) {
      double r = radrel.get(iditer).doubleValue(0);
      DoubleDBIDList rres = rangeQuery.getRangeForDBID(iditer, r);
      int ichecksum = 0;
      for(DBIDIter it = rres.iter(); it.valid(); it.advance()) {
        ichecksum += DBIDUtil.asInteger(it);
      }
      hash = Util.mixHashCodes(hash, ichecksum);
      mv.put(rres.size());
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics("Result hashcode: " + hash);
      LOG.statistics("Mean number of results: " + mv.getMean() + " +- " + mv.getNaiveStddev());
    }
    return null;
  }

  /**
   * Run the algorithm, with a separate query set.
   *
   * @param database Database
   * @param relation Relation
   * @return Null result
   */
  public Result run(Database database, Relation<O> relation) {
    if(queries == null) {
      throw new AbortException("A query set is required for this 'run' method.");
    }
    // Get a distance and kNN query instance.
    DistanceQuery<O> distQuery = database.getDistanceQuery(relation, getDistanceFunction());
    RangeQuery<O> rangeQuery = database.getRangeQuery(distQuery);
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
      StringBuilder buf = new StringBuilder();
      buf.append("No compatible data type in query input was found. Expected: ");
      buf.append(res.toString());
      buf.append(" have: ");
      for(int i = 0; i < bundle.metaLength(); i++) {
        if(i > 0) {
          buf.append(' ');
        }
        buf.append(bundle.meta(i).toString());
      }
      throw new IncompatibleDataException(buf.toString());
    }
    // Random sampling is a bit of hack, sorry.
    // But currently, we don't (yet) have an "integer random sample" function.
    DBIDRange sids = DBIDUtil.generateStaticDBIDRange(bundle.dataLength());

    final DBIDs sample = DBIDUtil.randomSample(sids, sampling, random);
    FiniteProgress prog = LOG.isVeryVerbose() ? new FiniteProgress("kNN queries", sample.size(), LOG) : null;
    int hash = 0;
    MeanVariance mv = new MeanVariance();
    double[] buf = new double[dim];
    for(DBIDIter iditer = sample.iter(); iditer.valid(); iditer.advance()) {
      int off = sids.binarySearch(iditer);
      assert (off >= 0);
      NumberVector o = (NumberVector) bundle.data(off, col);
      for(int i = 0; i < dim; i++) {
        buf[i] = o.doubleValue(i);
      }
      O v = ofactory.newNumberVector(buf);
      double r = o.doubleValue(dim);
      DoubleDBIDList rres = rangeQuery.getRangeForObject(v, r);
      int ichecksum = 0;
      for(DBIDIter it = rres.iter(); it.valid(); it.advance()) {
        ichecksum += DBIDUtil.asInteger(it);
      }
      hash = Util.mixHashCodes(hash, ichecksum);
      mv.put(rres.size());
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics("Result hashcode: " + hash);
      LOG.statistics("Mean number of results: " + mv.getMean() + " +- " + mv.getNaiveStddev());
    }
    return null;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    if(queries == null) {
      return TypeUtil.array(getDistanceFunction().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD_1D);
    }
    else {
      return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
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
  public static class Parameterizer<O extends NumberVector> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter for the query dataset.
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
     * Random generator factory
     */
    protected RandomFactory random;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DatabaseConnection> queryP = new ObjectParameter<>(QUERY_ID, DatabaseConnection.class);
      queryP.setOptional(true);
      if(config.grab(queryP)) {
        queries = queryP.instantiateClass(config);
      }
      DoubleParameter samplingP = new DoubleParameter(SAMPLING_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .setOptional(true);
      if(config.grab(samplingP)) {
        sampling = samplingP.doubleValue();
      }
      RandomParameter randomP = new RandomParameter(RANDOM_ID, RandomFactory.DEFAULT);
      if(config.grab(randomP)) {
        random = randomP.getValue();
      }
    }

    @Override
    protected RangeQueryBenchmarkAlgorithm<O> makeInstance() {
      return new RangeQueryBenchmarkAlgorithm<>(distanceFunction, queries, sampling, random);
    }
  }
}
