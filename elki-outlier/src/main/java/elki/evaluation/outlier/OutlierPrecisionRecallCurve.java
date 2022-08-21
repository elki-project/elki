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
package elki.evaluation.outlier;

import java.util.List;
import java.util.regex.Pattern;

import elki.database.DatabaseUtil;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.SetDBIDs;
import elki.evaluation.Evaluator;
import elki.evaluation.scores.AUPRCEvaluation;
import elki.evaluation.scores.AUPRCEvaluation.PRCurve;
import elki.evaluation.scores.adapter.OutlierScoreAdapter;
import elki.evaluation.scores.adapter.SimpleAdapter;
import elki.logging.Logging;
import elki.result.EvaluationResult;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.result.Metadata;
import elki.result.OrderingResult;
import elki.result.ResultUtil;
import elki.result.outlier.OutlierResult;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Compute a curve containing the precision values for an outlier detection
 * method. Unfortunately, there are quite different variations of this curve
 * in use, which is why tools may yield different results:
 * <ul>
 * <li>tie handling: on identical scores, many implementations evaluate a random
 * order, or perform linear interpolation; neither of which is proper
 * <li>at a recall of 0, the value is not defined; one could either begin
 * computing the area beginning at a recall of 1 object, or assume that the
 * recall at 0 is the recall at the first object (which appears to be more
 * common)
 * </ul>
 * References:
 * <p>
 * J. Davis and M. Goadrich<br>
 * The relationship between Precision-Recall and ROC curves<br>
 * Proc. 23rd Int. Conf. Machine Learning (ICML)
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @has - - - PRCurve
 */
@Reference(authors = "J. Davis and M. Goadrich", //
    title = "The relationship between Precision-Recall and ROC curves", //
    booktitle = "Proc. 23rd Int. Conf. Machine Learning (ICML)", //
    url = "https://doi.org/10.1145/1143844.1143874", //
    bibkey = "DBLP:conf/icml/DavisG06")
public class OutlierPrecisionRecallCurve implements Evaluator {
  /**
   * AUC value for PR curve
   */
  public static final String PRAUC_LABEL = "AUPRC";

  /**
   * The logger.
   */
  private static final Logging LOG = Logging.getLogger(OutlierPrecisionRecallCurve.class);

  /**
   * Matcher for the "positive" class.
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
    SetDBIDs positiveids = DBIDUtil.ensureSet(DatabaseUtil.getObjectsByLabelMatch(ResultUtil.findDatabase(result), positiveClassName));
    if(positiveids.size() == 0) {
      LOG.warning("Computing a P/R curve failed - no objects matched.");
      return;
    }

    List<OutlierResult> oresults = OutlierResult.getOutlierResults(result);
    List<OrderingResult> orderings = ResultUtil.getOrderingResults(result);
    // Outlier results are the main use case.
    for(OutlierResult o : oresults) {
      PRCurve curve = AUPRCEvaluation.materializePRC(new OutlierScoreAdapter(positiveids, o));
      Metadata.hierarchyOf(o).addChild(curve);
      MeasurementGroup g = EvaluationResult.findOrCreate(o, EvaluationResult.RANKING) //
          .findOrCreateGroup("Evaluation measures");
      if(!g.hasMeasure(PRAUC_LABEL)) {
        g.addMeasure(PRAUC_LABEL, curve.getAUC(), 0., 1., false);
      }
      // Process them only once.
      orderings.remove(o.getOrdering());
    }

    // FIXME: find appropriate place to add the derived result
    // otherwise apply an ordering to the database IDs.
    for(OrderingResult or : orderings) {
      DBIDs sorted = or.order(or.getDBIDs());
      PRCurve curve = AUPRCEvaluation.materializePRC(new SimpleAdapter(positiveids, sorted.iter(), sorted.size()));
      Metadata.hierarchyOf(or).addChild(curve);
      MeasurementGroup g = EvaluationResult.findOrCreate(or, EvaluationResult.RANKING) //
          .findOrCreateGroup("Evaluation measures");
      if(!g.hasMeasure(PRAUC_LABEL)) {
        g.addMeasure(PRAUC_LABEL, curve.getAUC(), 0., 1., false);
      }
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

    /**
     * Matcher for the "positive" class.
     */
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
