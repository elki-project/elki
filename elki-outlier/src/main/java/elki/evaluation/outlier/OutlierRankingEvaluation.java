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
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.SetDBIDs;
import elki.evaluation.Evaluator;
import elki.evaluation.scores.*;
import elki.evaluation.scores.adapter.DBIDsTest;
import elki.evaluation.scores.adapter.OutlierScoreAdapter;
import elki.evaluation.scores.adapter.SimpleAdapter;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.result.*;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.result.outlier.OutlierResult;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
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

  private void evaluateOutlierResult(EvaluationResult res, int size, SetDBIDs positiveids, OutlierResult or) {
    DBIDsTest test = new DBIDsTest(positiveids);

    final int pos = positiveids.size();
    final double rate = pos / (double) size;
    MeasurementGroup g = res.findOrCreateGroup("Evaluation measures");
    double rocauc = ROCEvaluation.STATIC.evaluate(test, new OutlierScoreAdapter(or));
    if(!g.hasMeasure("ROC AUC")) {
      g.addMeasure("ROC AUC", rocauc, 0., 1., .5, false);
    }
    double avep = AveragePrecisionEvaluation.STATIC.evaluate(test, new OutlierScoreAdapter(or));
    g.addMeasure("Average Precision", avep, 0., 1., rate, false);
    double rprec = PrecisionAtKEvaluation.RPRECISION.evaluate(test, new OutlierScoreAdapter(or));
    g.addMeasure("R-Precision", rprec, 0., 1., rate, false);
    double maxf1 = MaximumF1Evaluation.STATIC.evaluate(test, new OutlierScoreAdapter(or));
    g.addMeasure("Maximum F1", maxf1, 0., 1., rate, false);
    double maxdcg = DCGEvaluation.maximum(pos);
    double dcg = DCGEvaluation.STATIC.evaluate(test, new OutlierScoreAdapter(or));
    g.addMeasure("DCG", dcg, 0., maxdcg, DCGEvaluation.STATIC.expected(pos, size), false);
    double ndcg = NDCGEvaluation.STATIC.evaluate(test, new OutlierScoreAdapter(or));
    g.addMeasure("NDCG", ndcg, 0., 1., NDCGEvaluation.STATIC.expected(pos, size), false);

    g = res.findOrCreateGroup("Adjusted for chance");
    double adjauc = 2 * rocauc - 1;
    g.addMeasure("Adjusted AUC", adjauc, 0., 1., 0., false);
    double adjavep = (avep - rate) / (1 - rate);
    g.addMeasure("Adjusted AveP", adjavep, 0., 1., 0., false);
    double adjrprec = (rprec - rate) / (1 - rate);
    g.addMeasure("Adjusted R-Prec", adjrprec, 0., 1., 0., false);
    double adjmaxf1 = (maxf1 - rate) / (1 - rate);
    g.addMeasure("Adjusted Max F1", adjmaxf1, 0., 1., 0., false);
    double endcg = NDCGEvaluation.STATIC.expected(pos, size);
    double adjndcg = (ndcg - endcg) / (1. - endcg);
    g.addMeasure("Adjusted DCG", adjndcg, 0., 1., 0., false);

    if(LOG.isStatistics()) {
      LOG.statistics(new DoubleStatistic(key + ".rocauc", rocauc));
      LOG.statistics(new DoubleStatistic(key + ".rocauc.adjusted", adjauc));
      LOG.statistics(new DoubleStatistic(key + ".precision.average", avep));
      LOG.statistics(new DoubleStatistic(key + ".precision.average.adjusted", adjavep));
      LOG.statistics(new DoubleStatistic(key + ".precision.r", rprec));
      LOG.statistics(new DoubleStatistic(key + ".precision.r.adjusted", adjrprec));
      LOG.statistics(new DoubleStatistic(key + ".f1.maximum", maxf1));
      LOG.statistics(new DoubleStatistic(key + ".f1.maximum.adjusted", adjmaxf1));
      LOG.statistics(new DoubleStatistic(key + ".dcg", dcg));
      LOG.statistics(new DoubleStatistic(key + ".dcg.normalized", ndcg));
      LOG.statistics(new DoubleStatistic(key + ".dcg.adjusted", adjndcg));
    }
  }

  private void evaluateOrderingResult(EvaluationResult res, int size, SetDBIDs positiveids, DBIDs order) {
    if(order.size() != size) {
      throw new IllegalStateException("Iterable result doesn't match database size - incomplete ordering?");
    }

    DBIDsTest test = new DBIDsTest(positiveids);

    double rate = positiveids.size() / (double) size;
    MeasurementGroup g = res.newGroup("Evaluation measures:");
    double rocauc = ROCEvaluation.STATIC.evaluate(test, new SimpleAdapter(order.iter()));
    g.addMeasure("ROC AUC", rocauc, 0., 1., .5, false);
    double avep = AveragePrecisionEvaluation.STATIC.evaluate(test, new SimpleAdapter(order.iter()));
    g.addMeasure("Average Precision", avep, 0., 1., rate, false);
    double rprec = PrecisionAtKEvaluation.RPRECISION.evaluate(test, new SimpleAdapter(order.iter()));
    g.addMeasure("R-Precision", rprec, 0., 1., rate, false);
    double maxf1 = MaximumF1Evaluation.STATIC.evaluate(test, new SimpleAdapter(order.iter()));
    g.addMeasure("Maximum F1", maxf1, 0., 1., rate, false);

    g = res.newGroup("Adjusted for chance:");
    double adjauc = 2 * rocauc - 1;
    g.addMeasure("Adjusted AUC", adjauc, 0., 1., 0., false);
    double adjavep = (avep - rate) / (1 - rate);
    g.addMeasure("Adjusted AveP", adjavep, 0., 1., 0., false);
    double adjrprec = (rprec - rate) / (1 - rate);
    g.addMeasure("Adjusted R-Prec", adjrprec, 0., 1., 0., false);
    double adjmaxf1 = (maxf1 - rate) / (1 - rate);
    g.addMeasure("Adjusted Max F1", adjmaxf1, 0., 1., 0., false);

    if(LOG.isStatistics()) {
      LOG.statistics(new DoubleStatistic(key + ".rocauc", rocauc));
      LOG.statistics(new DoubleStatistic(key + ".rocauc.adjusted", adjauc));
      LOG.statistics(new DoubleStatistic(key + ".precision.average", avep));
      LOG.statistics(new DoubleStatistic(key + ".precision.average.adjusted", adjavep));
      LOG.statistics(new DoubleStatistic(key + ".precision.r", rprec));
      LOG.statistics(new DoubleStatistic(key + ".precision.r.adjusted", adjrprec));
      LOG.statistics(new DoubleStatistic(key + ".f1.maximum", maxf1));
      LOG.statistics(new DoubleStatistic(key + ".f1.maximum.adjusted", adjmaxf1));
    }
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
      EvaluationResult res = EvaluationResult.findOrCreate(o, EvaluationResult.RANKING);
      evaluateOutlierResult(res, o.getScores().size(), positiveids, o);
      // Process them only once.
      orderings.remove(o.getOrdering());
      nonefound = false;
    }

    // FIXME: find appropriate place to add the derived result
    // otherwise apply an ordering to the database IDs.
    for(OrderingResult or : orderings) {
      DBIDs sorted = or.order(or.getDBIDs());
      EvaluationResult res = EvaluationResult.findOrCreate(or, EvaluationResult.RANKING);
      evaluateOrderingResult(res, or.getDBIDs().size(), positiveids, sorted);
      nonefound = false;
    }

    if(nonefound) {
      return;
      // LOG.warning("No results found to process with ROC curve analyzer. Got
      // "+iterables.size()+" iterables, "+orderings.size()+" orderings.");
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * The pattern to identify positive classes.
     */
    public static final OptionID POSITIVE_CLASS_NAME_ID = new OptionID("outliereval.positive", "Class label for the 'positive' class.");

    /**
     * Pattern for positive class.
     */
    protected Pattern positiveClassName = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      PatternParameter positiveClassNameP = new PatternParameter(POSITIVE_CLASS_NAME_ID);
      if(config.grab(positiveClassNameP)) {
        positiveClassName = positiveClassNameP.getValue();
      }
    }

    @Override
    protected OutlierRankingEvaluation makeInstance() {
      return new OutlierRankingEvaluation(positiveClassName);
    }
  }
}
