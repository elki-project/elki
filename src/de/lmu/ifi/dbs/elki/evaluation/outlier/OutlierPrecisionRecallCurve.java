package de.lmu.ifi.dbs.elki.evaluation.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.geometry.XYCurve;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Compute a curve containing the precision values for an outlier detection
 * method.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has PRCurve
 */
public class OutlierPrecisionRecallCurve implements Evaluator {
  /**
   * The logger.
   */
  private static final Logging LOG = Logging.getLogger(OutlierPrecisionRecallCurve.class);

  /**
   * The pattern to identify positive classes.
   * 
   * <p>
   * Key: {@code -precision.positive}
   * </p>
   */
  public static final OptionID POSITIVE_CLASS_NAME_ID = OptionID.getOrCreateOptionID("precision.positive", "Class label for the 'positive' class.");

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
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    Database db = ResultUtil.findDatabase(baseResult);
    // Prepare
    SetDBIDs positiveids = DBIDUtil.ensureSet(DatabaseUtil.getObjectsByLabelMatch(db, positiveClassName));

    if (positiveids.size() == 0) {
      LOG.warning("Computing a ROC curve failed - no objects matched.");
      return;
    }

    List<OutlierResult> oresults = ResultUtil.getOutlierResults(result);
    List<OrderingResult> orderings = ResultUtil.getOrderingResults(result);
    // Outlier results are the main use case.
    for (OutlierResult o : oresults) {
      DBIDs sorted = o.getOrdering().iter(o.getOrdering().getDBIDs());
      XYCurve curve = computePrecisionResult(o.getScores().size(), positiveids, sorted.iter(), o.getScores());
      db.getHierarchy().add(o, curve);
      // Process them only once.
      orderings.remove(o.getOrdering());
    }

    // FIXME: find appropriate place to add the derived result
    // otherwise apply an ordering to the database IDs.
    for (OrderingResult or : orderings) {
      DBIDs sorted = or.iter(or.getDBIDs());
      XYCurve curve = computePrecisionResult(or.getDBIDs().size(), positiveids, sorted.iter(), null);
      db.getHierarchy().add(or, curve);
    }
  }

  private XYCurve computePrecisionResult(int size, SetDBIDs ids, DBIDIter iter, Relation<Double> scores) {
    final int postot = ids.size();
    int poscnt = 0, total = 0;
    XYCurve curve = new PRCurve(postot + 2, postot);

    double prevscore = Double.NaN;
    for (; iter.valid(); iter.advance()) {
      // Previous precision rate - y axis
      final double curprec = ((double) poscnt) / total;
      // Previous recall rate - x axis
      final double curreca = ((double) poscnt) / postot;

      // Analyze next point
      // positive or negative match?
      if (ids.contains(iter)) {
        poscnt += 1;
      }
      total += 1;
      // First iteration ends here
      if (total == 1) {
        continue;
      }
      // defer calculation for ties
      if (scores != null) {
        double curscore = scores.get(iter);
        if (Double.compare(prevscore, curscore) == 0) {
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
     * AUC value for PR curve
     */
    public static final String PRAUC_LABEL = "PR-AUC";

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
    }

    @Override
    public String getLongName() {
      return "Precision-Recall-Curve";
    }

    @Override
    public String getShortName() {
      return "pr-curve";
    }

    /**
     * Get AUC value
     * 
     * @return AUC value
     */
    public double getAUC() {
      if (Double.isNaN(auc)) {
        double max = 1 - 1. / positive; 
        auc = areaUnderCurve(this) / max;
      }
      return auc;
    }

    @Override
    public void writeToText(TextWriterStream out, String label) {
      out.commentPrintLn(PRAUC_LABEL + ": " + getAUC());
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
    protected Pattern positiveClassName = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      PatternParameter positiveClassNameP = new PatternParameter(POSITIVE_CLASS_NAME_ID);
      if (config.grab(positiveClassNameP)) {
        positiveClassName = positiveClassNameP.getValue();
      }
    }

    @Override
    protected OutlierPrecisionRecallCurve makeInstance() {
      return new OutlierPrecisionRecallCurve(positiveClassName);
    }
  }
}
