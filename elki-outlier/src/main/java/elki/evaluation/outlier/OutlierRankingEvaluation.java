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
import java.util.function.Supplier;
import java.util.regex.Pattern;

import elki.database.Database;
import elki.database.DatabaseUtil;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.SetDBIDs;
import elki.evaluation.Evaluator;
import elki.evaluation.scores.*;
import elki.evaluation.scores.adapter.OutlierScoreAdapter;
import elki.evaluation.scores.adapter.SimpleAdapter;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.result.EvaluationResult;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.result.OrderingResult;
import elki.result.ResultUtil;
import elki.result.outlier.OutlierResult;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Evaluate outlier scores by their ranking
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @opt nodefillcolor LemonChiffon
 *
 * @assoc - - - OutlierResult
 * @navhas - create - EvaluationResult
 */
public class OutlierRankingEvaluation implements Evaluator {
  /**
   * The logger.
   */
  private static final Logging LOG = Logging.getLogger(OutlierRankingEvaluation.class);

  /**
   * Stores the "positive" class.
   */
  private Pattern positiveClassName;

  /**
   * Key prefix for statistics logging.
   */
  private String key = OutlierRankingEvaluation.class.getName();

  /**
   * Constructor.
   *
   * @param positive_class_name Positive class name pattern
   */
  public OutlierRankingEvaluation(Pattern positive_class_name) {
    super();
    this.positiveClassName = positive_class_name;
  }

  /**
   * Produce various evaluation statistics
   *
   * @param res Result to output to
   * @param size Total size
   * @param pos Number of positive hits
   * @param adapter Adapter
   */
  private void evaluate(EvaluationResult res, int size, int pos, Supplier<ScoreEvaluation.Adapter> adapter) {
    final double rate = pos / (double) size;
    MeasurementGroup g = res.findOrCreateGroup("Evaluation measures");
    MeasurementGroup ag = res.findOrCreateGroup("Adjusted for chance");
    // Area under Receiver Operating Curve
    double auroc = ROCEvaluation.STATIC.evaluate(adapter.get());
    LOG.statistics(new DoubleStatistic(key + ".auroc", auroc));
    g.addMeasure("AUROC", auroc, 0., 1., .5, false);
    double adjauroc = 2 * auroc - 1;
    ag.addMeasure("Adjusted AUROC", adjauroc, 0., 1., 0., false);
    LOG.statistics(new DoubleStatistic(key + ".auroc.adjusted", adjauroc));
    // Area under Precision-Recall-Curve
    double auprc = AUPRCEvaluation.STATIC.evaluate(adapter.get());
    LOG.statistics(new DoubleStatistic(key + ".auprc", auprc));
    g.addMeasure("AUPRC", auprc, 0., 1., rate, false);
    double adjauprc = (auprc - rate) / (1 - rate);
    LOG.statistics(new DoubleStatistic(key + ".auprc.adjusted", adjauprc));
    ag.addMeasure("Adjusted AUPRC", adjauprc, 0., 1., 0., false);
    // Area under Precision-Recall-Gain-Curve
    double auprgc = PRGCEvaluation.STATIC.evaluate(adapter.get());
    LOG.statistics(new DoubleStatistic(key + ".auprgc", auprgc));
    g.addMeasure("AUPRGC", auprgc, 0., 1., .5, false);
    ag.addMeasure("Adjusted AUPRGC", (auprgc - 0.5) * 2, 0., 1., 0., false);
    // Average precision
    double avep = AveragePrecisionEvaluation.STATIC.evaluate(adapter.get());
    LOG.statistics(new DoubleStatistic(key + ".average-precision.", avep));
    g.addMeasure("Average Precision", avep, 0., 1., rate, false);
    double adjavep = (avep - rate) / (1 - rate);
    LOG.statistics(new DoubleStatistic(key + ".average-precision.adjusted", adjavep));
    ag.addMeasure("Adjusted AveP", adjavep, 0., 1., 0., false);
    // R-precision
    double rprec = PrecisionAtKEvaluation.RPRECISION.evaluate(adapter.get());
    LOG.statistics(new DoubleStatistic(key + ".r-precision", rprec));
    g.addMeasure("R-Precision", rprec, 0., 1., rate, false);
    double adjrprec = (rprec - rate) / (1 - rate);
    LOG.statistics(new DoubleStatistic(key + ".r-precision.adjusted", adjrprec));
    ag.addMeasure("Adjusted R-Prec", adjrprec, 0., 1., 0., false);
    // Maximum F1 measure
    double maxf1 = MaximumF1Evaluation.STATIC.evaluate(adapter.get());
    LOG.statistics(new DoubleStatistic(key + ".maximum-f1", maxf1));
    g.addMeasure("Maximum F1", maxf1, 0., 1., rate, false);
    double adjmaxf1 = (maxf1 - rate) / (1 - rate);
    LOG.statistics(new DoubleStatistic(key + ".maximum-f1.adjusted", adjmaxf1));
    ag.addMeasure("Adjusted Max F1", adjmaxf1, 0., 1., 0., false);
    // Maximum DCG, Normalized DCG
    double maxdcg = DCGEvaluation.maximum(pos);
    double dcg = DCGEvaluation.STATIC.evaluate(adapter.get());
    LOG.statistics(new DoubleStatistic(key + ".dcg", dcg));
    g.addMeasure("DCG", dcg, 0., maxdcg, DCGEvaluation.STATIC.expected(pos, size), false);
    double ndcg = NDCGEvaluation.STATIC.evaluate(adapter.get());
    LOG.statistics(new DoubleStatistic(key + ".dcg.normalized", ndcg));
    g.addMeasure("NDCG", ndcg, 0., 1., NDCGEvaluation.STATIC.expected(pos, size), false);
    double endcg = NDCGEvaluation.STATIC.expected(pos, size);
    double adjndcg = (ndcg - endcg) / (1. - endcg);
    LOG.statistics(new DoubleStatistic(key + ".dcg.adjusted", adjndcg));
    ag.addMeasure("Adjusted DCG", adjndcg, 0., 1., 0., false);
  }

  @Override
  public void processNewResult(Object result) {
    Database db = ResultUtil.findDatabase(result);
    SetDBIDs positiveids = DBIDUtil.ensureSet(DatabaseUtil.getObjectsByLabelMatch(db, positiveClassName));

    if(positiveids.size() == 0) {
      LOG.warning("Cannot evaluate outlier results - no objects matched the given pattern.");
      return;
    }

    boolean nonefound = true;
    List<OutlierResult> oresults = OutlierResult.getOutlierResults(result);
    List<OrderingResult> orderings = ResultUtil.getOrderingResults(result);
    // Outlier results are the main use case.
    for(OutlierResult o : oresults) {
      evaluate(EvaluationResult.findOrCreate(o, EvaluationResult.RANKING), //
          o.getScores().size(), positiveids.size(), () -> new OutlierScoreAdapter(positiveids, o));
      // Process them only once.
      orderings.remove(o.getOrdering());
      nonefound = false;
    }

    // FIXME: find appropriate place to add the derived result
    // otherwise apply an ordering to the database IDs.
    for(OrderingResult or : orderings) {
      DBIDs sorted = or.order(or.getDBIDs());
      int size = or.getDBIDs().size();
      if(sorted.size() != size) {
        throw new IllegalStateException("Iterable result doesn't match database size - incomplete ordering?");
      }
      evaluate(EvaluationResult.findOrCreate(or, EvaluationResult.RANKING), //
          size, positiveids.size(), () -> new SimpleAdapter(positiveids, sorted.iter(), sorted.size()));
      nonefound = false;
    }

    if(nonefound) {
      // LOG.warning("No results found to process with ROC curve analyzer. Got
      // "+iterables.size()+" iterables, "+orderings.size()+" orderings.");
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
    public static final OptionID POSITIVE_CLASS_NAME_ID = new OptionID("outliereval.positive", "Class label for the 'positive' class.");

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
    public OutlierRankingEvaluation make() {
      return new OutlierRankingEvaluation(positiveClassName);
    }
  }
}
