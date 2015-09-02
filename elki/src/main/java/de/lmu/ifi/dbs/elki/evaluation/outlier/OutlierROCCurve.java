package de.lmu.ifi.dbs.elki.evaluation.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.evaluation.scores.ROCEvaluation;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.DBIDsTest;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.OutlierScoreAdapter;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.SimpleAdapter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.geometry.XYCurve;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Compute a ROC curve to evaluate a ranking algorithm and compute the
 * corresponding ROCAUC value.
 *
 * The parameter {@code -rocauc.positive} specifies the class label of
 * "positive" hits.
 *
 * The nested algorithm {@code -algorithm} will be run, the result will be
 * searched for an iterable or ordering result, which then is compared with the
 * clustering obtained via the given class label.
 *
 * @author Erich Schubert
 *
 * @apiviz.landmark
 *
 * @apiviz.uses OutlierResult
 * @apiviz.has ROCResult oneway - - «create»
 */
@Alias({ "ComputeROCCurve", "de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve" })
public class OutlierROCCurve implements Evaluator {
  /**
   * The label we use for marking ROCAUC values.
   */
  public static final String ROCAUC_LABEL = "ROCAUC";

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

  private ROCResult computeROCResult(int size, SetDBIDs positiveids, DBIDs order) {
    if(order.size() != size) {
      throw new IllegalStateException("Iterable result doesn't match database size - incomplete ordering?");
    }
    XYCurve roccurve = ROCEvaluation.materializeROC(new DBIDsTest(positiveids), new SimpleAdapter(order.iter()));
    double rocauc = XYCurve.areaUnderCurve(roccurve);
    return new ROCResult(roccurve, rocauc);
  }

  private ROCResult computeROCResult(int size, SetDBIDs positiveids, OutlierResult or) {
    XYCurve roccurve = ROCEvaluation.materializeROC(new DBIDsTest(positiveids), new OutlierScoreAdapter(or));
    double rocauc = XYCurve.areaUnderCurve(roccurve);
    return new ROCResult(roccurve, rocauc);
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result result) {
    Database db = ResultUtil.findDatabase(hier);
    // Prepare
    SetDBIDs positiveids = DBIDUtil.ensureSet(DatabaseUtil.getObjectsByLabelMatch(db, positiveClassName));

    if(positiveids.size() == 0) {
      LOG.warning("Computing a ROC curve failed - no objects matched.");
      return;
    }

    boolean nonefound = true;
    List<OutlierResult> oresults = ResultUtil.getOutlierResults(result);
    List<OrderingResult> orderings = ResultUtil.getOrderingResults(result);
    // Outlier results are the main use case.
    for(OutlierResult o : oresults) {
      db.getHierarchy().add(o, computeROCResult(o.getScores().size(), positiveids, o));
      // Process them only once.
      orderings.remove(o.getOrdering());
      nonefound = false;
    }

    // FIXME: find appropriate place to add the derived result
    // otherwise apply an ordering to the database IDs.
    for(OrderingResult or : orderings) {
      DBIDs sorted = or.order(or.getDBIDs());
      db.getHierarchy().add(or, computeROCResult(or.getDBIDs().size(), positiveids, sorted));
      nonefound = false;
    }

    if(nonefound) {
      return;
      // logger.warning("No results found to process with ROC curve analyzer. Got "+iterables.size()+" iterables, "+orderings.size()+" orderings.");
    }
  }

  /**
   * Result object for ROC curves.
   *
   * @author Erich Schubert
   */
  public static class ROCResult extends XYCurve {
    /**
     * AUC value
     */
    private double auc;

    /**
     * Constructor.
     *
     * @param col roc curve
     * @param rocauc ROC AUC value
     */
    public ROCResult(XYCurve col, double rocauc) {
      super(col);
      this.auc = rocauc;
    }

    /**
     * @return the area under curve
     */
    public double getAUC() {
      return auc;
    }

    @Override
    public String getLongName() {
      return "ROC Curve";
    }

    @Override
    public String getShortName() {
      return "roc-curve";
    }

    @Override
    public void writeToText(TextWriterStream out, String label) {
      out.commentPrintLn(ROCAUC_LABEL + ": " + auc);
      out.flush();
      super.writeToText(out, label);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * The pattern to identify positive classes.
     *
     * <p>
     * Key: {@code -rocauc.positive}
     * </p>
     */
    public static final OptionID POSITIVE_CLASS_NAME_ID = new OptionID("rocauc.positive", "Class label for the 'positive' class.");
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
    protected OutlierROCCurve makeInstance() {
      return new OutlierROCCurve(positiveClassName);
    }
  }
}
