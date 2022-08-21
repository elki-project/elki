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
import elki.evaluation.scores.PRGCEvaluation;
import elki.evaluation.scores.PRGCEvaluation.PRGCurve;
import elki.evaluation.scores.adapter.OutlierScoreAdapter;
import elki.evaluation.scores.adapter.SimpleAdapter;
import elki.logging.Logging;
import elki.result.EvaluationResult;
import elki.result.Metadata;
import elki.result.OrderingResult;
import elki.result.ResultUtil;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.result.outlier.OutlierResult;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Compute a curve containing the precision gain and revall gain values for an
 * outlier detection method.
 * <p>
 * References:
 * <p>
 * P. Flach and M. Knull<br>
 * Precision-Recall-Gain Curves: PR Analysis Done Right<br>
 * Neural Information Processing Systems (NIPS 2015)
 * 
 * @author Robert Gehde
 * @since 0.8.0
 */
@Reference(authors = "P. Flach and M. Knull", //
    title = "Precision-Recall-Gain Curves: {PR} Analysis Done Right", //
    booktitle = "Neural Information Processing Systems (NIPS 2015)", //
    url = "http://papers.nips.cc/paper/5867-precision-recall-gain-curves-pr-analysis-done-right", //
    bibkey = "DBLP:conf/nips/FlachK15")
public class OutlierPrecisionRecallGainCurve implements Evaluator {
  /**
   * AUC Label for PRG Curve.
   */
  public static final String AUPRGC_LABEL = "AUPRGC";

  /**
   * The logger.
   */
  private static final Logging LOG = Logging.getLogger(OutlierPrecisionRecallGainCurve.class);

  /**
   * Pattern for the positive class.
   */
  private Pattern positiveClassName;

  /**
   * Constructor.
   *
   * @param positiveClassName Pattern to recognize outliers
   */
  public OutlierPrecisionRecallGainCurve(Pattern positiveClassName) {
    super();
    this.positiveClassName = positiveClassName;
  }

  @Override
  public void processNewResult(Object newResult) {
    Database db = ResultUtil.findDatabase(newResult);
    SetDBIDs positiveIDs = DBIDUtil.ensureSet(DatabaseUtil.getObjectsByLabelMatch(db, positiveClassName));
    if(positiveIDs.size() == 0) {
      LOG.warning("Computing a P/R-G curve failed - no objects matched.");
      return;
    }
    List<OutlierResult> outResults = OutlierResult.getOutlierResults(newResult);
    List<OrderingResult> ordResults = ResultUtil.getOrderingResults(newResult);

    for(OutlierResult o : outResults) {
      PRGCurve curve = PRGCEvaluation.materializePRGC(new OutlierScoreAdapter(positiveIDs, o));
      Metadata.hierarchyOf(o).addChild(curve);
      MeasurementGroup g = EvaluationResult.findOrCreate(o, EvaluationResult.RANKING) //
          .findOrCreateGroup("Evaluation measures");
      if(!g.hasMeasure(AUPRGC_LABEL)) {
        g.addMeasure(AUPRGC_LABEL, curve.getAUC(), 0., 1., false);
      }
      // Process them only once.
      ordResults.remove(o.getOrdering());
    }
    for(OrderingResult r : ordResults) {
      DBIDs sorted = r.order(r.getDBIDs());
      PRGCurve curve = PRGCEvaluation.materializePRGC(new SimpleAdapter(positiveIDs, sorted.iter(), sorted.size()));
      Metadata.hierarchyOf(r).addChild(curve);
      MeasurementGroup g = EvaluationResult.findOrCreate(r, EvaluationResult.RANKING) //
          .findOrCreateGroup("Evaluation measures");
      if(!g.hasMeasure(AUPRGC_LABEL)) {
        g.addMeasure(AUPRGC_LABEL, curve.getAUC(), 0., 1., false);
      }
    }
  }

  /**
   * Parameterization class.
   *
   * @author Robert Gehde
   */
  public static class Par implements Parameterizer {
    /**
     * The pattern to identify positive classes.
     */
    public static final OptionID POSITIVE_CLASS_NAME_ID = new OptionID("precisionrecallgain.positive", "Class label for the 'positive' class.");

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
    public OutlierPrecisionRecallGainCurve make() {
      return new OutlierPrecisionRecallGainCurve(positiveClassName);
    }
  }
}
