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

import elki.database.Database;
import elki.database.DatabaseUtil;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.SetDBIDs;
import elki.evaluation.Evaluator;
import elki.evaluation.scores.ROCEvaluation;
import elki.evaluation.scores.ROCEvaluation.ROCurve;
import elki.evaluation.scores.adapter.OutlierScoreAdapter;
import elki.evaluation.scores.adapter.SimpleAdapter;
import elki.logging.Logging;
import elki.result.EvaluationResult;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.result.Metadata;
import elki.result.OrderingResult;
import elki.result.ResultUtil;
import elki.result.outlier.OutlierResult;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Compute a ROC curve to evaluate a ranking algorithm and compute the
 * corresponding AUROC value.
 * <p>
 * The parameter {@code -AUROC.positive} specifies the class label of
 * "positive" hits.
 * <p>
 * The nested algorithm {@code -algorithm} will be run, the result will be
 * searched for an iterable or ordering result, which then is compared with the
 * clustering obtained via the given class label.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @opt nodefillcolor LemonChiffon
 *
 * @assoc - - - OutlierResult
 * @navhas - create - ROCResult
 */
public class OutlierROCCurve implements Evaluator {
  /**
   * The label we use for marking AUROC values.
   */
  public static final String AUROC_LABEL = "AUROC";

  /**
   * The logger.
   */
  private static final Logging LOG = Logging.getLogger(OutlierROCCurve.class);

  /**
   * Stores the "positive" class.
   */
  private Pattern positiveClassName;

  /**
   * Constructor.
   *
   * @param positive_class_name Positive class name pattern
   */
  public OutlierROCCurve(Pattern positive_class_name) {
    super();
    this.positiveClassName = positive_class_name;
  }

  @Override
  public void processNewResult(Object result) {
    Database db = ResultUtil.findDatabase(result);
    // Prepare
    SetDBIDs positiveids = DBIDUtil.ensureSet(DatabaseUtil.getObjectsByLabelMatch(db, positiveClassName));

    if(positiveids.size() == 0) {
      LOG.warning("Computing a ROC curve failed - no objects matched.");
      return;
    }

    boolean nonefound = true;
    List<OutlierResult> oresults = OutlierResult.getOutlierResults(result);
    List<OrderingResult> orderings = ResultUtil.getOrderingResults(result);
    // Outlier results are the main use case.
    for(OutlierResult o : oresults) {
      ROCurve roc = ROCEvaluation.materializeROC(new OutlierScoreAdapter(positiveids, o));
      Metadata.hierarchyOf(o).addChild(roc);
      MeasurementGroup g = EvaluationResult.findOrCreate(o, EvaluationResult.RANKING) //
          .findOrCreateGroup("Evaluation measures");
      if(!g.hasMeasure(AUROC_LABEL)) {
        g.addMeasure(AUROC_LABEL, roc.getAUC(), 0., 1., false);
      }
      // Process each ordering only once.
      orderings.remove(o.getOrdering());
      nonefound = false;
    }

    // FIXME: find appropriate place to add the derived result
    // otherwise apply an ordering to the database IDs.
    for(OrderingResult or : orderings) {
      DBIDs sorted = or.order(or.getDBIDs());
      if(sorted.size() != or.getDBIDs().size()) {
        throw new IllegalStateException("Iterable result doesn't match database size - incomplete ordering?");
      }
      ROCurve roc = ROCEvaluation.materializeROC(new SimpleAdapter(positiveids, sorted.iter(), sorted.size()));
      Metadata.hierarchyOf(or).addChild(roc);
      MeasurementGroup g = EvaluationResult.findOrCreate(or, EvaluationResult.RANKING) //
          .findOrCreateGroup("Evaluation measures");
      if(!g.hasMeasure(AUROC_LABEL)) {
        g.addMeasure(AUROC_LABEL, roc.getAUC(), 0., 1., false);
      }
      nonefound = false;
    }

    if(nonefound) {
      // LOG.warning("No results found to process with ROCurve analyzer.
      // Got "+iterables.size()+" iterables, "+orderings.size()+" orderings.");
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
    public static final OptionID POSITIVE_CLASS_NAME_ID = new OptionID("auroc.positive", "Class label for the 'positive' class.");

    /**
     * Pattern for positive class.
     */
    protected Pattern positiveClassName = null;

    @Override
    public void configure(Parameterization config) {
      new PatternParameter(POSITIVE_CLASS_NAME_ID) //
          .grab(config, x -> positiveClassName = x);
    }

    @Override
    public OutlierROCCurve make() {
      return new OutlierROCCurve(positiveClassName);
    }
  }
}
