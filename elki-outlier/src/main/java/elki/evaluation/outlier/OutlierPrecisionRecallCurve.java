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
package elki.evaluation.outlier;

import java.util.List;
import java.util.regex.Pattern;

import elki.database.Database;
import elki.database.DatabaseUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.SetDBIDs;
import elki.database.relation.DoubleRelation;
import elki.evaluation.Evaluator;
import elki.logging.Logging;
import elki.math.geometry.XYCurve;
import elki.result.*;
import elki.result.outlier.OutlierResult;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Compute a curve containing the precision values for an outlier detection
 * method.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @has - - - PRCurve
 */
public class OutlierPrecisionRecallCurve implements Evaluator {
  /**
   * AUC value for PR curve
   */
  public static final String PRAUC_LABEL = "PR AUC";

  /**
   * The logger.
   */
  private static final Logging LOG = Logging.getLogger(OutlierPrecisionRecallCurve.class);

  /**
   * Stores the "positive" class.
   */
  private Pattern positiveClassName;

  /**
   * Constructor.
   *
   * @param positiveClassName Pattern to recognize outliers
   */
  public OutlierPrecisionRecallCurve(Pattern positiveClassName) {
    super();
    this.positiveClassName = positiveClassName;
  }

  @Override
  public void processNewResult(Object result) {
    Database db = ResultUtil.findDatabase(result);
    // Prepare
    SetDBIDs positiveids = DBIDUtil.ensureSet(DatabaseUtil.getObjectsByLabelMatch(db, positiveClassName));

    if(positiveids.size() == 0) {
      LOG.warning("Computing a P/R curve failed - no objects matched.");
      return;
    }

    List<OutlierResult> oresults = OutlierResult.getOutlierResults(result);
    List<OrderingResult> orderings = ResultUtil.getOrderingResults(result);
    // Outlier results are the main use case.
    for(OutlierResult o : oresults) {
      DBIDs sorted = o.getOrdering().order(o.getOrdering().getDBIDs());
      PRCurve curve = computePrecisionResult(positiveids, sorted.iter(), o.getScores());
      Metadata.hierarchyOf(o).addChild(curve);
      EvaluationResult ev = EvaluationResult.findOrCreate(o, EvaluationResult.RANKING);
      ev.findOrCreateGroup("Evaluation measures").addMeasure(PRAUC_LABEL, curve.getAUC(), 0., 1., false);
      // Process them only once.
      orderings.remove(o.getOrdering());
    }

    // FIXME: find appropriate place to add the derived result
    // otherwise apply an ordering to the database IDs.
    for(OrderingResult or : orderings) {
      DBIDs sorted = or.order(or.getDBIDs());
      PRCurve curve = computePrecisionResult(positiveids, sorted.iter(), null);
      Metadata.hierarchyOf(or).addChild(curve);
      EvaluationResult ev = EvaluationResult.findOrCreate(or, EvaluationResult.RANKING);
      ev.findOrCreateGroup("Evaluation measures").addMeasure(PRAUC_LABEL, curve.getAUC(), 0., 1., false);
    }
  }

  private PRCurve computePrecisionResult(SetDBIDs ids, DBIDIter iter, DoubleRelation scores) {
    final int postot = ids.size();
    int poscnt = 0, total = 0;
    PRCurve curve = new PRCurve(postot + 2, postot);

    double prevscore = Double.NaN;
    for(; iter.valid(); iter.advance()) {
      // Previous precision rate - y axis
      final double curprec = ((double) poscnt) / total;
      // Previous recall rate - x axis
      final double curreca = ((double) poscnt) / postot;

      // Analyze next point
      // positive or negative match?
      if(ids.contains(iter)) {
        poscnt += 1;
      }
      total += 1;
      // First iteration ends here
      if(total == 1) {
        continue;
      }
      // defer calculation for ties
      if(scores != null) {
        double curscore = scores.doubleValue(iter);
        if(Double.compare(prevscore, curscore) == 0) {
          continue;
        }
        prevscore = curscore;
      }
      // Add a new point (for the previous entry - because of tie handling!)
      curve.addAndSimplify(curreca, curprec);
    }
    // End curve - always at all positives found.
    curve.addAndSimplify(1.0, postot / total);
    return curve;
  }

  /**
   * P/R Curve
   *
   * @author Erich Schubert
   */
  public static class PRCurve extends XYCurve {
    /**
     * Area under curve
     */
    double auc = Double.NaN;

    /**
     * Number of positive observations
     */
    int positive;

    /**
     * Constructor.
     *
     * @param size Size estimation
     * @param positive Number of positive elements (for AUC correction)
     */
    public PRCurve(int size, int positive) {
      super("Recall", "Precision", size);
      this.positive = positive;
      Metadata.of(this).setLongName("Precision-Recall-Curve");
    }

    /**
     * Get AUC value
     *
     * @return AUC value
     */
    public double getAUC() {
      if(Double.isNaN(auc)) {
        double max = 1 - 1. / positive;
        auc = areaUnderCurve(this) / max;
      }
      return auc;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * The pattern to identify positive classes.
     */
    public static final OptionID POSITIVE_CLASS_NAME_ID = new OptionID("precision.positive", "Class label for the 'positive' class.");

    protected Pattern positiveClassName = null;

    @Override
    public void configure(Parameterization config) {
      new PatternParameter(POSITIVE_CLASS_NAME_ID) //
          .grab(config, x -> positiveClassName = x);
    }

    @Override
    public OutlierPrecisionRecallCurve make() {
      return new OutlierPrecisionRecallCurve(positiveClassName);
    }
  }
}
