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
package elki.outlier.lof;

import elki.data.type.CombinedTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.progress.StepProgress;
import elki.math.DoubleMinMax;
import elki.math.MathUtil;
import elki.math.statistics.distribution.NormalDistribution;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.outlier.ProbabilisticOutlierScore;
import elki.utilities.Priority;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
 * @has - - - KNNSearcher
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
@Priority(Priority.RECOMMENDED)
public class LoOP<O> implements OutlierAlgorithm {
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
  protected Distance<? super O> reachabilityDistance;

  /**
   * Distance function for comparison set.
   */
  protected Distance<? super O> comparisonDistance;

  /**
   * Constructor with parameters.
   *
   * @param kreach k for reachability
   * @param kcomp k for comparison
   * @param reachabilityDistance distance function for reachability
   * @param comparisonDistance distance function for comparison
   * @param lambda Lambda parameter
   */
  public LoOP(int kreach, int kcomp, Distance<? super O> reachabilityDistance, Distance<? super O> comparisonDistance, double lambda) {
    super();
    this.kreach = kreach;
    this.kcomp = kcomp;
    this.reachabilityDistance = reachabilityDistance;
    this.comparisonDistance = comparisonDistance;
    this.lambda = lambda;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(reachabilityDistance.equals(comparisonDistance) ? reachabilityDistance.getInputTypeRestriction() : //
        new CombinedTypeInformation(reachabilityDistance.getInputTypeRestriction(), comparisonDistance.getInputTypeRestriction()));
  }

  /**
   * Performs the LoOP algorithm on the given database.
   *
   * @param relation Relation to process
   * @return Outlier result
   */
  public OutlierResult run(Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress(5) : null;
    KNNSearcher<DBIDRef> knnComp, knnReach;
    if(comparisonDistance == reachabilityDistance || comparisonDistance.equals(reachabilityDistance)) {
      LOG.beginStep(stepprog, 1, "Materializing neighborhoods with respect to reference neighborhood distance function.");
      knnReach = knnComp = new QueryBuilder<>(relation, comparisonDistance).precomputed().kNNByDBID(MathUtil.max(kcomp, kreach) + 1);
    }
    else {
      LOG.beginStep(stepprog, 1, "Not materializing distance functions, since we request each DBID once only.");
      knnComp = new QueryBuilder<>(relation, comparisonDistance).kNNByDBID(kreach + 1);
      knnReach = new QueryBuilder<>(relation, reachabilityDistance).kNNByDBID(kcomp + 1);
    }

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
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Local Outlier Probabilities", relation.getDBIDs(), plofs);
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
  protected void computePDists(Relation<O> relation, KNNSearcher<DBIDRef> knn, WritableDoubleDataStore pdists) {
    // computing PRDs
    FiniteProgress prdsProgress = LOG.isVerbose() ? new FiniteProgress("pdists", relation.size(), LOG) : null;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final KNNList neighbors = knn.getKNN(iditer, kreach + 1); // +
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
      double pdist = ks > 0 ? Math.sqrt(ssum / ks) : 0.;
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
  protected double computePLOFs(Relation<O> relation, KNNSearcher<DBIDRef> knn, WritableDoubleDataStore pdists, WritableDoubleDataStore plofs) {
    FiniteProgress progressPLOFs = LOG.isVerbose() ? new FiniteProgress("PLOFs for objects", relation.size(), LOG) : null;
    double nplof = 0.;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final KNNList neighbors = knn.getKNN(iditer, kcomp + 1); // + query
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

    nplof = lambda * Math.sqrt(nplof / relation.size());
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("nplof normalization factor is " + nplof);
    }
    return nplof > 0. ? nplof : 1.;
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
  public static class Par<O> implements Parameterizer {
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
    protected Distance<O> reachabilityDistance = null;

    /**
     * Preprocessor Step 2.
     */
    protected Distance<O> comparisonDistance = null;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(KCOMP_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> kcomp = x);
      new ObjectParameter<Distance<O>>(COMPARISON_DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> comparisonDistance = x);
      kreach = kcomp;
      new IntParameter(KREACH_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .setOptional(true) //
          .grab(config, x -> kreach = x);
      new ObjectParameter<Distance<O>>(REACHABILITY_DISTANCE_FUNCTION_ID, Distance.class) //
          .setOptional(true) //
          .grab(config, x -> reachabilityDistance = x);
      // TODO: make default 1.0?
      new DoubleParameter(LAMBDA_ID, 2.0) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> lambda = x);
    }

    @Override
    public LoOP<O> make() {
      Distance<O> realreach = (reachabilityDistance != null) ? reachabilityDistance : comparisonDistance;
      return new LoOP<>(kreach, kcomp, realreach, comparisonDistance, lambda);
    }
  }
}
