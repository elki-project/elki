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

import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Algorithm to validate the quality of an approximative kNN index, by
 * performing a number of queries and comparing them to the results obtained by
 * exact indexing (e.g. linear scanning).
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <O> Object type
 * 
 * @assoc - - - KNNQuery
 */
public class ValidateApproximativeKNNIndex<O> extends AbstractDistanceBasedAlgorithm<O, Result> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ValidateApproximativeKNNIndex.class);

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
   * Force linear scanning.
   */
  protected boolean forcelinear = false;

  /**
   * Random generator factory
   */
  protected RandomFactory random;

  /**
   * Filter pattern
   */
  protected Pattern pattern;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function to use
   * @param k K parameter
   * @param queries Query data set (may be null!)
   * @param sampling Sampling rate
   * @param random Random factory
   * @param forcelinear Force the use of linear scanning.
   * @param pattern
   */
  public ValidateApproximativeKNNIndex(DistanceFunction<? super O> distanceFunction, int k, DatabaseConnection queries, double sampling, boolean forcelinear, RandomFactory random, Pattern pattern) {
    super(distanceFunction);
    this.k = k;
    this.queries = queries;
    this.sampling = sampling;
    this.forcelinear = forcelinear;
    this.random = random;
    this.pattern = pattern;
  }

  /**
   * Run the algorithm.
   * 
   * @param database Database
   * @param relation Relation
   * @return Null result
   */
  public Result run(Database database, Relation<O> relation) {
    // Get a distance and kNN query instance.
    DistanceQuery<O> distQuery = database.getDistanceQuery(relation, getDistanceFunction());
    // Approximate query:
    KNNQuery<O> knnQuery = database.getKNNQuery(distQuery, k, DatabaseQuery.HINT_OPTIMIZED_ONLY);
    if(knnQuery == null || knnQuery instanceof LinearScanQuery) {
      throw new AbortException("Expected an accelerated query, but got a linear scan -- index is not used.");
    }
    // Exact query:
    KNNQuery<O> truekNNQuery;
    if(forcelinear) {
      truekNNQuery = QueryUtil.getLinearScanKNNQuery(distQuery);
    }
    else {
      truekNNQuery = database.getKNNQuery(distQuery, k, DatabaseQuery.HINT_EXACT);
    }
    if(knnQuery.getClass().equals(truekNNQuery.getClass())) {
      LOG.warning("Query classes are the same. This experiment may be invalid!");
    }

    // No query set - use original database.
    if(queries == null || pattern != null) {
      // Relation to filter on
      Relation<String> lrel = (pattern != null) ? DatabaseUtil.guessLabelRepresentation(database) : null;

      final DBIDs sample = DBIDUtil.randomSample(relation.getDBIDs(), sampling, random);
      FiniteProgress prog = LOG.isVeryVerbose() ? new FiniteProgress("kNN queries", sample.size(), LOG) : null;
      MeanVariance mv = new MeanVariance(), mvrec = new MeanVariance();
      MeanVariance mvdist = new MeanVariance(), mvdaerr = new MeanVariance(),
          mvdrerr = new MeanVariance();
      int misses = 0;
      for(DBIDIter iditer = sample.iter(); iditer.valid(); iditer.advance()) {
        if(pattern == null || pattern.matcher(lrel.get(iditer)).find()) {
          // Query index:
          KNNList knns = knnQuery.getKNNForDBID(iditer, k);
          // Query reference:
          KNNList trueknns = truekNNQuery.getKNNForDBID(iditer, k);

          // Put adjusted knn size:
          mv.put(knns.size() * k / (double) trueknns.size());

          // Put recall:
          mvrec.put(DBIDUtil.intersectionSize(knns, trueknns) / (double) trueknns.size());

          if(knns.size() >= k) {
            double kdist = knns.getKNNDistance();
            final double tdist = trueknns.getKNNDistance();
            if(tdist > 0.0) {
              mvdist.put(kdist);
              mvdaerr.put(kdist - tdist);
              mvdrerr.put(kdist / tdist);
            }
          }
          else {
            // Less than k objects.
            misses++;
          }
        }
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
      if(LOG.isStatistics()) {
        LOG.statistics("Mean number of results: " + mv.getMean() + " +- " + mv.getNaiveStddev());
        LOG.statistics("Recall of true results: " + mvrec.getMean() + " +- " + mvrec.getNaiveStddev());
        if(mvdist.getCount() > 0) {
          LOG.statistics("Mean k-distance: " + mvdist.getMean() + " +- " + mvdist.getNaiveStddev());
          LOG.statistics("Mean absolute k-error: " + mvdaerr.getMean() + " +- " + mvdaerr.getNaiveStddev());
          LOG.statistics("Mean relative k-error: " + mvdrerr.getMean() + " +- " + mvdrerr.getNaiveStddev());
        }
        if(misses > 0) {
          LOG.statistics(String.format("Number of queries that returned less than k=%d objects: %d (%.2f%%)", k, misses, misses * 100. / mv.getCount()));
        }
      }
    }
    else {
      // Separate query set.
      TypeInformation res = getDistanceFunction().getInputTypeRestriction();
      MultipleObjectsBundle bundle = queries.loadData();
      int col = -1;
      for(int i = 0; i < bundle.metaLength(); i++) {
        if(res.isAssignableFromType(bundle.meta(i))) {
          col = i;
          break;
        }
      }
      if(col < 0) {
        throw new AbortException("No compatible data type in query input was found. Expected: " + res.toString());
      }
      // Random sampling is a bit of hack, sorry.
      // But currently, we don't (yet) have an "integer random sample" function.
      DBIDRange sids = DBIDUtil.generateStaticDBIDRange(bundle.dataLength());
      final DBIDs sample = DBIDUtil.randomSample(sids, sampling, random);
      FiniteProgress prog = LOG.isVeryVerbose() ? new FiniteProgress("kNN queries", sample.size(), LOG) : null;
      MeanVariance mv = new MeanVariance(), mvrec = new MeanVariance();
      MeanVariance mvdist = new MeanVariance(), mvdaerr = new MeanVariance(),
          mvdrerr = new MeanVariance();
      int misses = 0;
      for(DBIDIter iditer = sample.iter(); iditer.valid(); iditer.advance()) {
        int off = sids.binarySearch(iditer);
        assert (off >= 0);
        @SuppressWarnings("unchecked")
        O o = (O) bundle.data(off, col);

        // Query index:
        KNNList knns = knnQuery.getKNNForObject(o, k);
        // Query reference:
        KNNList trueknns = truekNNQuery.getKNNForObject(o, k);

        // Put adjusted knn size:
        mv.put(knns.size() * k / (double) trueknns.size());

        // Put recall:
        mvrec.put(DBIDUtil.intersectionSize(knns, trueknns) / (double) trueknns.size());

        if(knns.size() >= k) {
          double kdist = knns.getKNNDistance();
          final double tdist = trueknns.getKNNDistance();
          if(tdist > 0.0) {
            mvdist.put(kdist);
            mvdaerr.put(kdist - tdist);
            mvdrerr.put(kdist / tdist);
          }
        }
        else {
          // Less than k objects.
          misses++;
        }
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
      if(LOG.isStatistics()) {
        LOG.statistics("Mean number of results: " + mv.getMean() + " +- " + mv.getNaiveStddev());
        LOG.statistics("Recall of true results: " + mvrec.getMean() + " +- " + mvrec.getNaiveStddev());
        if(mvdist.getCount() > 0) {
          LOG.statistics("Mean absolute k-error: " + mvdaerr.getMean() + " +- " + mvdaerr.getNaiveStddev());
          LOG.statistics("Mean relative k-error: " + mvdrerr.getMean() + " +- " + mvdrerr.getNaiveStddev());
        }
        if(misses > 0) {
          LOG.statistics(String.format("Number of queries that returned less than k=%d objects: %d (%.2f%%)", k, misses, misses * 100. / mv.getCount()));
        }
      }
    }
    return null;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
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
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter for the number of neighbors.
     */
    public static final OptionID K_ID = new OptionID("validateknn.k", "Number of neighbors to retreive for kNN benchmarking.");

    /**
     * Parameter for the query dataset.
     */
    public static final OptionID QUERY_ID = new OptionID("validateknn.query", "Data source for the queries. If not set, the queries are taken from the database.");

    /**
     * Parameter for the sampling size.
     */
    public static final OptionID SAMPLING_ID = new OptionID("validateknn.sampling", "Sampling size parameter. If the value is less or equal 1, it is assumed to be the relative share. Larger values will be interpreted as integer sizes. By default, all data will be used.");

    /**
     * Force linear scanning.
     */
    public static final OptionID FORCE_ID = new OptionID("validateknn.force-linear", "Force the use of linear scanning as reference.");

    /**
     * Parameter for the random generator.
     */
    public static final OptionID RANDOM_ID = new OptionID("validateknn.random", "Random generator for sampling.");

    /**
     * Parameter to select query points.
     */
    public static final OptionID PATTERN_ID = new OptionID("validateknn.pattern", "Pattern to select query points.");

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
     * Force linear scanning.
     */
    protected boolean forcelinear = false;

    /**
     * Random generator factory
     */
    protected RandomFactory random;

    /**
     * Filter pattern for query points.
     */
    protected Pattern pattern;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }
      PatternParameter patternP = new PatternParameter(PATTERN_ID) //
          .setOptional(true);
      if(config.grab(patternP)) {
        pattern = patternP.getValue();
      }
      else {
        ObjectParameter<DatabaseConnection> queryP = new ObjectParameter<>(QUERY_ID, DatabaseConnection.class);
        queryP.setOptional(true);
        if(config.grab(queryP)) {
          queries = queryP.instantiateClass(config);
        }
      }
      DoubleParameter samplingP = new DoubleParameter(SAMPLING_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .setOptional(true);
      if(config.grab(samplingP)) {
        sampling = samplingP.doubleValue();
      }
      Flag forceP = new Flag(FORCE_ID);
      if(config.grab(forceP)) {
        forcelinear = forceP.isTrue();
      }
      RandomParameter randomP = new RandomParameter(RANDOM_ID, RandomFactory.DEFAULT);
      if(config.grab(randomP)) {
        random = randomP.getValue();
      }
    }

    @Override
    protected ValidateApproximativeKNNIndex<O> makeInstance() {
      return new ValidateApproximativeKNNIndex<>(distanceFunction, k, queries, sampling, forcelinear, random, pattern);
    }
  }
}
