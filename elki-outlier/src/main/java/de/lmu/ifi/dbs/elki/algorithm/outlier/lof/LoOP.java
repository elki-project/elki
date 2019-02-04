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
package de.lmu.ifi.dbs.elki.algorithm.outlier.lof;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.CombinedTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import net.jafama.FastMath;

/**
 * LoOP: Local Outlier Probabilities
 * <p>
 * Distance/density based algorithm similar to LOF to detect outliers, but with
 * statistical methods to achieve better result stability.
 * <p>
 * Reference:
 * <p>
 * Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek:<br>
 * LoOP: Local Outlier Probabilities<br>
 * Proc. 18th Int. Conf. Information and Knowledge Management (CIKM 2009)
 * <p>
 * Implementation notes:
 * <ul>
 * <li>The lambda parameter was removed from the pdist term, because it cancels
 * out.</li>
 * <li>In ELKI 0.7.0, the {@code k} parameters have changed by 1 to make them
 * similar to other methods and more intuitive.</li>
 * </ul>
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @has - - - KNNQuery
 *
 * @param <O> type of objects handled by this algorithm
 */
@Title("LoOP: Local Outlier Probabilities")
@Description("Variant of the LOF algorithm normalized using statistical values.")
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", //
    title = "LoOP: Local Outlier Probabilities", //
    booktitle = "Proc. 18th Int. Conf. Information and Knowledge Management (CIKM 2009)", //
    url = "https://doi.org/10.1145/1645953.1646195", //
    bibkey = "DBLP:conf/cikm/KriegelKSZ09")
@Alias("de.lmu.ifi.dbs.elki.algorithm.outlier.LoOP")
@Priority(Priority.RECOMMENDED)
public class LoOP<O> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(LoOP.class);

  /**
   * Reachability neighborhood size.
   */
  int kreach;

  /**
   * Comparison neighborhood size.
   */
  int kcomp;

  /**
   * Lambda parameter.
   */
  double lambda;

  /**
   * Distance function for reachability.
   */
  protected DistanceFunction<? super O> reachabilityDistanceFunction;

  /**
   * Distance function for comparison set.
   */
  protected DistanceFunction<? super O> comparisonDistanceFunction;

  /**
   * Constructor with parameters.
   *
   * @param kreach k for reachability
   * @param kcomp k for comparison
   * @param reachabilityDistanceFunction distance function for reachability
   * @param comparisonDistanceFunction distance function for comparison
   * @param lambda Lambda parameter
   */
  public LoOP(int kreach, int kcomp, DistanceFunction<? super O> reachabilityDistanceFunction, DistanceFunction<? super O> comparisonDistanceFunction, double lambda) {
    super();
    this.kreach = kreach;
    this.kcomp = kcomp;
    this.reachabilityDistanceFunction = reachabilityDistanceFunction;
    this.comparisonDistanceFunction = comparisonDistanceFunction;
    this.lambda = lambda;
  }

  /**
   * Get the kNN queries for the algorithm.
   *
   * @param database Database to analyze
   * @param relation Relation to analyze
   * @param stepprog Progress logger, may be {@code null}
   * @return result
   */
  protected Pair<KNNQuery<O>, KNNQuery<O>> getKNNQueries(Database database, Relation<O> relation, StepProgress stepprog) {
    KNNQuery<O> knnComp, knnReach;
    if(comparisonDistanceFunction == reachabilityDistanceFunction || comparisonDistanceFunction.equals(reachabilityDistanceFunction)) {
      LOG.beginStep(stepprog, 1, "Materializing neighborhoods with respect to reference neighborhood distance function.");
      knnComp = DatabaseUtil.precomputedKNNQuery(database, relation, comparisonDistanceFunction, MathUtil.max(kcomp, kreach) + 1);
      knnReach = knnComp;
    }
    else {
      LOG.beginStep(stepprog, 1, "Not materializing distance functions, since we request each DBID once only.");
      knnComp = QueryUtil.getKNNQuery(relation, comparisonDistanceFunction, kreach + 1);
      knnReach = QueryUtil.getKNNQuery(relation, reachabilityDistanceFunction, kcomp + 1);
    }
    return new Pair<>(knnComp, knnReach);
  }

  /**
   * Performs the LoOP algorithm on the given database.
   *
   * @param database Database to process
   * @param relation Relation to process
   * @return Outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress(5) : null;

    Pair<KNNQuery<O>, KNNQuery<O>> pair = getKNNQueries(database, relation, stepprog);
    KNNQuery<O> knnComp = pair.getFirst();
    KNNQuery<O> knnReach = pair.getSecond();

    // Assert we got something
    if(knnComp == null) {
      throw new AbortException("No kNN queries supported by database for comparison distance function.");
    }
    if(knnReach == null) {
      throw new AbortException("No kNN queries supported by database for density estimation distance function.");
    }

    // FIXME: tie handling!

    // Probabilistic distances
    WritableDoubleDataStore pdists = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB);
    LOG.beginStep(stepprog, 3, "Computing pdists");
    computePDists(relation, knnReach, pdists);
    // Compute PLOF values.
    WritableDoubleDataStore plofs = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    LOG.beginStep(stepprog, 4, "Computing PLOF");
    double nplof = computePLOFs(relation, knnComp, pdists, plofs);

    // Normalize the outlier scores.
    DoubleMinMax mm = new DoubleMinMax();
    {// compute LOOP_SCORE of each db object
      LOG.beginStep(stepprog, 5, "Computing LoOP scores");

      FiniteProgress progressLOOPs = LOG.isVerbose() ? new FiniteProgress("LoOP for objects", relation.size(), LOG) : null;
      final double norm = 1. / (nplof * MathUtil.SQRT2);
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        double loop = NormalDistribution.erf((plofs.doubleValue(iditer) - 1.) * norm);
        plofs.putDouble(iditer, loop);
        mm.put(loop);
        LOG.incrementProcessed(progressLOOPs);
      }
      LOG.ensureCompleted(progressLOOPs);
    }

    LOG.setCompleted(stepprog);

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Local Outlier Probabilities", "loop-outlier", plofs, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new ProbabilisticOutlierScore(mm.getMin(), mm.getMax(), 0.);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Compute the probabilistic distances used by LoOP.
   *
   * @param relation Data relation
   * @param knn kNN query
   * @param pdists Storage for distances
   */
  protected void computePDists(Relation<O> relation, KNNQuery<O> knn, WritableDoubleDataStore pdists) {
    // computing PRDs
    FiniteProgress prdsProgress = LOG.isVerbose() ? new FiniteProgress("pdists", relation.size(), LOG) : null;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final KNNList neighbors = knn.getKNNForDBID(iditer, kreach + 1); // +
                                                                       // query
                                                                       // point
      // use first kref neighbors as reference set
      int ks = 0;
      double ssum = 0.;
      for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid() && ks < kreach; neighbor.advance()) {
        if(DBIDUtil.equal(neighbor, iditer)) {
          continue;
        }
        final double d = neighbor.doubleValue();
        ssum += d * d;
        ks++;
      }
      double pdist = ks > 0 ? FastMath.sqrt(ssum / ks) : 0.;
      pdists.putDouble(iditer, pdist);
      LOG.incrementProcessed(prdsProgress);
    }
    LOG.ensureCompleted(prdsProgress);
  }

  /**
   * Compute the LOF values, using the pdist distances.
   *
   * @param relation Data relation
   * @param knn kNN query
   * @param pdists Precomputed distances
   * @param plofs Storage for PLOFs.
   * @return Normalization factor.
   */
  protected double computePLOFs(Relation<O> relation, KNNQuery<O> knn, WritableDoubleDataStore pdists, WritableDoubleDataStore plofs) {
    FiniteProgress progressPLOFs = LOG.isVerbose() ? new FiniteProgress("PLOFs for objects", relation.size(), LOG) : null;
    double nplof = 0.;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final KNNList neighbors = knn.getKNNForDBID(iditer, kcomp + 1); // + query
                                                                      // point
      // use first kref neighbors as comparison set.
      int ks = 0;
      double sum = 0.;
      for(DBIDIter neighbor = neighbors.iter(); neighbor.valid() && ks < kcomp; neighbor.advance()) {
        if(DBIDUtil.equal(neighbor, iditer)) {
          continue;
        }
        sum += pdists.doubleValue(neighbor);
        ks++;
      }
      double plof = MathUtil.max(pdists.doubleValue(iditer) * ks / sum, 1.0);
      if(Double.isNaN(plof) || Double.isInfinite(plof)) {
        plof = 1.0;
      }
      plofs.putDouble(iditer, plof);
      nplof += (plof - 1.0) * (plof - 1.0);

      LOG.incrementProcessed(progressPLOFs);
    }
    LOG.ensureCompleted(progressPLOFs);

    nplof = lambda * FastMath.sqrt(nplof / relation.size());
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("nplof normalization factor is " + nplof);
    }
    return nplof > 0. ? nplof : 1.;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    final TypeInformation type;
    if(reachabilityDistanceFunction.equals(comparisonDistanceFunction)) {
      type = reachabilityDistanceFunction.getInputTypeRestriction();
    }
    else {
      type = new CombinedTypeInformation(reachabilityDistanceFunction.getInputTypeRestriction(), comparisonDistanceFunction.getInputTypeRestriction());
    }
    return TypeUtil.array(type);
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
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * The distance function to determine the reachability distance between
     * database objects.
     */
    public static final OptionID REACHABILITY_DISTANCE_FUNCTION_ID = new OptionID("loop.referencedistfunction", "Distance function to determine the density of an object.");

    /**
     * The distance function to determine the reachability distance between
     * database objects.
     */
    public static final OptionID COMPARISON_DISTANCE_FUNCTION_ID = new OptionID("loop.comparedistfunction", "Distance function to determine the reference set of an object.");

    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its LOOP_SCORE, must be an integer greater than
     * 1.
     */
    public static final OptionID KREACH_ID = new OptionID("loop.kref", "The number of nearest neighbors of an object to be used for the PRD value.");

    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its LOOP_SCORE, must be an integer greater than
     * 1.
     */
    public static final OptionID KCOMP_ID = new OptionID("loop.kcomp", "The number of nearest neighbors of an object to be considered for computing its LOOP_SCORE.");

    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its LOOP_SCORE, must be an integer greater than
     * 1.
     */
    public static final OptionID LAMBDA_ID = new OptionID("loop.lambda", "The number of standard deviations to consider for density computation.");

    /**
     * Holds the value of {@link #KREACH_ID}.
     */
    int kreach = 0;

    /**
     * Holds the value of {@link #KCOMP_ID}.
     */
    int kcomp = 0;

    /**
     * Hold the value of {@link #LAMBDA_ID}.
     */
    double lambda = 2.0;

    /**
     * Preprocessor Step 1.
     */
    protected DistanceFunction<O> reachabilityDistanceFunction = null;

    /**
     * Preprocessor Step 2.
     */
    protected DistanceFunction<O> comparisonDistanceFunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kcompP = new IntParameter(KCOMP_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kcompP)) {
        kcomp = kcompP.intValue();
      }

      final ObjectParameter<DistanceFunction<O>> compDistP = new ObjectParameter<>(COMPARISON_DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(compDistP)) {
        comparisonDistanceFunction = compDistP.instantiateClass(config);
      }

      final IntParameter kreachP = new IntParameter(KREACH_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .setOptional(true);
      if(config.grab(kreachP)) {
        kreach = kreachP.intValue();
      }
      else {
        kreach = kcomp;
      }

      final ObjectParameter<DistanceFunction<O>> reachDistP = new ObjectParameter<>(REACHABILITY_DISTANCE_FUNCTION_ID, DistanceFunction.class, true);
      if(config.grab(reachDistP)) {
        reachabilityDistanceFunction = reachDistP.instantiateClass(config);
      }

      // TODO: make default 1.0?
      final DoubleParameter lambdaP = new DoubleParameter(LAMBDA_ID, 2.0) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(lambdaP)) {
        lambda = lambdaP.doubleValue();
      }
    }

    @Override
    protected LoOP<O> makeInstance() {
      DistanceFunction<O> realreach = (reachabilityDistanceFunction != null) ? reachabilityDistanceFunction : comparisonDistanceFunction;
      return new LoOP<>(kreach, kcomp, realreach, comparisonDistanceFunction, lambda);
    }
  }
}
