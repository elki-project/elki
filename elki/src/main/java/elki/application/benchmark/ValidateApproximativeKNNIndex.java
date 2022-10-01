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

import java.util.regex.Pattern;

import elki.application.AbstractDistanceBasedApplication;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.DatabaseUtil;
import elki.database.ids.*;
import elki.database.query.LinearScanQuery;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.datasource.DatabaseConnection;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MeanVariance;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.*;
import elki.utilities.random.RandomFactory;
import elki.workflow.InputStep;

/**
 * Algorithm to validate the quality of an approximative kNN index, by
 * performing a number of queries and comparing them to the results obtained by
 * exact indexing (e.g., linear scanning).
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <O> Object type
 * 
 * @assoc - - - KNNSearcher
 */
public class ValidateApproximativeKNNIndex<O> extends AbstractDistanceBasedApplication<O> {
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
   * @param input Data input
   * @param distance Distance function to use
   * @param k K parameter
   * @param queries Query data set (may be null!)
   * @param sampling Sampling rate
   * @param random Random factory
   * @param forcelinear Force the use of linear scanning.
   * @param pattern
   */
  public ValidateApproximativeKNNIndex(InputStep input, Distance<? super O> distance, int k, DatabaseConnection queries, double sampling, boolean forcelinear, RandomFactory random, Pattern pattern) {
    super(input, distance);
    this.k = k;
    this.queries = queries;
    this.sampling = sampling;
    this.forcelinear = forcelinear;
    this.random = random;
    this.pattern = pattern;
  }

  @Override
  public void run() {
    if(!LOG.isStatistics()) {
      LOG.error("Logging level should be at least level STATISTICS (parameter -time) to see any output.");
    }
    Database database = inputstep.getDatabase();
    Relation<O> relation = database.getRelation(distance.getInputTypeRestriction());
    MeanVariance mv = new MeanVariance(), mvrec = new MeanVariance(),
        mvdist = new MeanVariance(), mvdaerr = new MeanVariance(),
        mvdrerr = new MeanVariance();
    int misses = 0;
    // No query set - use original database.
    if(queries == null || pattern != null) {
      // Approximate query:
      KNNSearcher<DBIDRef> knnQuery = new QueryBuilder<>(relation, distance).optimizedOnly().kNNByDBID(k);
      if(knnQuery == null || knnQuery instanceof LinearScanQuery) {
        throw new AbortException("Expected an accelerated query, but got a linear scan -- index is not used.");
      }
      // Exact query:
      KNNSearcher<DBIDRef> truekNNQuery = forcelinear ? new QueryBuilder<>(relation, distance).linearOnly().kNNByDBID(k) //
          : new QueryBuilder<>(relation, distance).exactOnly().kNNByDBID(k);
      if(knnQuery.getClass().equals(truekNNQuery.getClass())) {
        LOG.warning("Query classes are the same. This experiment may be invalid!");
      }

      // Relation to filter on
      Relation<String> lrel = (pattern != null) ? DatabaseUtil.guessLabelRepresentation(database) : null;

      final DBIDs sample = DBIDUtil.randomSample(relation.getDBIDs(), sampling, random);
      FiniteProgress prog = LOG.isVeryVerbose() ? new FiniteProgress("kNN queries", sample.size(), LOG) : null;
      for(DBIDIter iditer = sample.iter(); iditer.valid(); iditer.advance()) {
        if(pattern == null || pattern.matcher(lrel.get(iditer)).find()) {
          // Query index:
          KNNList knns = knnQuery.getKNN(iditer, k);
          // Query reference:
          KNNList trueknns = truekNNQuery.getKNN(iditer, k);

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
    }
    else {
      // Approximate query:
      KNNSearcher<O> knnQuery = new QueryBuilder<>(relation, distance).optimizedOnly().kNNByObject(k);
      if(knnQuery == null || knnQuery instanceof LinearScanQuery) {
        throw new AbortException("Expected an accelerated query, but got a linear scan -- index is not used.");
      }
      // Exact query:
      KNNSearcher<O> truekNNQuery = forcelinear ? new QueryBuilder<>(relation, distance).linearOnly().kNNByObject(k) //
          : new QueryBuilder<>(relation, distance).exactOnly().kNNByObject(k);
      if(knnQuery.getClass().equals(truekNNQuery.getClass())) {
        LOG.warning("Query classes are the same. This experiment may be invalid!");
      }

      // Separate query set.
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
        throw new AbortException("No compatible data type in query input was found. Expected: " + res.toString());
      }
      // Random sampling is a bit of hack, sorry.
      // But currently, we don't (yet) have an "integer random sample" function.
      DBIDRange sids = DBIDUtil.generateStaticDBIDRange(bundle.dataLength());
      final DBIDs sample = DBIDUtil.randomSample(sids, sampling, random);
      FiniteProgress prog = LOG.isVeryVerbose() ? new FiniteProgress("kNN queries", sample.size(), LOG) : null;
      for(DBIDIter iditer = sample.iter(); iditer.valid(); iditer.advance()) {
        int off = sids.binarySearch(iditer);
        assert (off >= 0);
        @SuppressWarnings("unchecked")
        O o = (O) bundle.data(off, col);

        // Query index:
        KNNList knns = knnQuery.getKNN(o, k);
        // Query reference:
        KNNList trueknns = truekNNQuery.getKNN(o, k);

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
    }
    if(LOG.isStatistics()) {
      LOG.statistics("Mean number of results: " + mv.getMean() + " +- " + mv.getPopulationStddev());
      LOG.statistics("Recall of true results: " + mvrec.getMean() + " +- " + mvrec.getPopulationStddev());
      if(mvdist.getCount() > 0) {
        LOG.statistics("Mean absolute k-error: " + mvdaerr.getMean() + " +- " + mvdaerr.getPopulationStddev());
        LOG.statistics("Mean relative k-error: " + mvdrerr.getMean() + " +- " + mvdrerr.getPopulationStddev());
      }
      if(misses > 0) {
        LOG.statistics(String.format("Number of queries that returned less than k=%d objects: %d (%.2f%%)", k, misses, misses * 100. / mv.getCount()));
      }
    }
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
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      if(!new PatternParameter(PATTERN_ID) //
          .setOptional(true) //
          .grab(config, x -> pattern = x)) //
      {
        new ObjectParameter<DatabaseConnection>(QUERY_ID, DatabaseConnection.class) //
            .setOptional(true) //
            .grab(config, x -> queries = x);
      }
      new DoubleParameter(SAMPLING_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .setOptional(true) //
          .grab(config, x -> sampling = x);
      new Flag(FORCE_ID).grab(config, x -> forcelinear = x);
      new RandomParameter(RANDOM_ID, RandomFactory.DEFAULT).grab(config, x -> random = x);
    }

    @Override
    public ValidateApproximativeKNNIndex<O> make() {
      return new ValidateApproximativeKNNIndex<>(inputstep, distance, k, queries, sampling, forcelinear, random, pattern);
    }
  }
}
