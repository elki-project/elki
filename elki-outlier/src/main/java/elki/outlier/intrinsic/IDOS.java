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
package elki.outlier.intrinsic;

import elki.AbstractDistanceBasedAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.DoubleDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.progress.StepProgress;
import elki.math.DoubleMinMax;
import elki.math.statistics.intrinsicdimensionality.ALIDEstimator;
import elki.math.statistics.intrinsicdimensionality.IntrinsicDimensionalityEstimator;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.outlier.QuotientOutlierScoreMeta;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Intrinsic Dimensional Outlier Detection in High-Dimensional Data.
 * <p>
 * Reference:
 * <p>
 * Jonathan von Brünken, Michael E. Houle, Arthur Zimek<br>
 * Intrinsic Dimensional Outlier Detection in High-Dimensional Data<br>
 * NII Technical Report (NII-2015-003E)
 *
 * @author Jonathan von Brünken
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <O> Object type
 */
@Title("IDOS: Intrinsic Dimensional Outlier Score")
@Reference(authors = "Jonathan von Brünken, Michael E. Houle, Arthur Zimek", //
    title = "Intrinsic Dimensional Outlier Detection in High-Dimensional Data", //
    booktitle = "NII Technical Report (NII-2015-003E)", //
    url = "http://www.nii.ac.jp/TechReports/15-003E.html", //
    bibkey = "tr/nii/BrunkenHZ15")
public class IDOS<O> extends AbstractDistanceBasedAlgorithm<Distance<? super O>, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(IDOS.class);

  /**
   * kNN for the context set (ID computation).
   */
  protected int k_c;

  /**
   * kNN for the reference set.
   */
  protected int k_r;

  /**
   * Estimator for intrinsic dimensionality.
   */
  protected IntrinsicDimensionalityEstimator estimator;

  /**
   * Constructor.
   *
   * @param distance the distance function to use
   * @param estimator Estimator for intrinsic dimensionality
   * @param kc the context set size for the ID computation
   * @param kr the neighborhood size to use in score computation
   */
  public IDOS(Distance<? super O> distance, IntrinsicDimensionalityEstimator estimator, int kc, int kr) {
    super(distance);
    this.estimator = estimator;
    this.k_c = kc;
    this.k_r = kr;
  }

  /**
   * Run the algorithm
   *
   * @param relation Data relation
   * @return Outlier result
   */
  public OutlierResult run(Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("IDOS", 3) : null;
    if(stepprog != null) {
      stepprog.beginStep(1, "Precomputing neighborhoods", LOG);
    }
    KNNQuery<O> knnQ = new QueryBuilder<>(relation, distance).precomputed().kNNQuery(Math.max(k_c, k_r) + 1);
    DBIDs ids = relation.getDBIDs();

    if(stepprog != null) {
      stepprog.beginStep(2, "Computing intrinsic dimensionalities", LOG);
    }
    DoubleDataStore intDims = computeIDs(ids, knnQ);
    if(stepprog != null) {
      stepprog.beginStep(3, "Computing IDOS scores", LOG);
    }
    DoubleMinMax idosminmax = new DoubleMinMax();
    DoubleDataStore ldms = computeIDOS(ids, knnQ, intDims, idosminmax);
    if(stepprog != null) {
      stepprog.setCompleted(LOG);
    }
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Intrinsic Dimensionality Outlier Score", ids, ldms);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(idosminmax.getMin(), idosminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Computes all IDs
   *
   * @param ids the DBIDs to process
   * @param knnQ the KNN query
   * @return The computed intrinsic dimensionalities.
   */
  protected DoubleDataStore computeIDs(DBIDs ids, KNNQuery<O> knnQ) {
    WritableDoubleDataStore intDims = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Intrinsic dimensionality", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double id = 0.;
      try {
        id = estimator.estimate(knnQ, iter, k_c + 1);
      }
      catch(ArithmeticException e) {
        id = 0; // Too many duplicates, etc.
      }
      intDims.putDouble(iter, id);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    return intDims;
  }

  /**
   * Computes all IDOS scores.
   *
   * @param ids the DBIDs to process
   * @param knnQ the KNN query
   * @param intDims Precomputed intrinsic dimensionalities
   * @param idosminmax Output of minimum and maximum, for metadata
   * @return ID scores
   */
  protected DoubleDataStore computeIDOS(DBIDs ids, KNNQuery<O> knnQ, DoubleDataStore intDims, DoubleMinMax idosminmax) {
    WritableDoubleDataStore ldms = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("ID Outlier Scores for objects", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final KNNList neighbors = knnQ.getKNNForDBID(iter, k_r);
      double sum = 0.;
      int cnt = 0;
      for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
        if(DBIDUtil.equal(iter, neighbor)) {
          continue;
        }
        final double id = intDims.doubleValue(neighbor);
        sum += id > 0 ? 1.0 / id : 0.;
        if(++cnt == k_r) { // Always stop after at most k_r elements.
          break;
        }
      }
      final double id_q = intDims.doubleValue(iter);
      final double idos = id_q > 0 ? id_q * sum / cnt : 0.;

      ldms.putDouble(iter, idos);
      idosminmax.put(idos);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    return ldms;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Jonathan von Brünken
   * @author Erich Schubert
   */
  public static class Par<O> extends AbstractDistanceBasedAlgorithm.Par<Distance<? super O>> {
    /**
     * The class used for estimating the intrinsic dimensionality.
     */
    public static final OptionID ESTIMATOR_ID = new OptionID("idos.estimator", "Estimator of intrinsic dimensionality.");

    /**
     * Parameter to specify the neighborhood size to use for the averaging.
     */
    public static final OptionID KR_ID = new OptionID("idos.kr", "Reference set size.");

    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * used for the GED computation.
     */
    public static final OptionID KC_ID = new OptionID("idos.kc", "Context set size (ID estimation).");

    /**
     * Estimator for intrinsic dimensionality.
     */
    protected IntrinsicDimensionalityEstimator estimator;

    /**
     * kNN for the context set (ID computation).
     */
    protected int k_c = 20;

    /**
     * kNN for the reference set.
     */
    protected int k_r = 20;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<IntrinsicDimensionalityEstimator>(ESTIMATOR_ID, IntrinsicDimensionalityEstimator.class, ALIDEstimator.class) //
          .grab(config, x -> estimator = x);
      new IntParameter(KC_ID) //
          .addConstraint(new GreaterEqualConstraint(5)) //
          .grab(config, x -> k_c = x);
      new IntParameter(KR_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> k_r = x);
    }

    @Override
    public IDOS<O> make() {
      return new IDOS<>(distance, estimator, k_c, k_r);
    }
  }
}
