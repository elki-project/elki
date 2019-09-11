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
import elki.database.ids.SetDBIDs;
import elki.database.relation.DoubleRelation;
import elki.evaluation.Evaluator;
import elki.logging.Logging;
import elki.math.geometry.XYCurve;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.result.outlier.OutlierResult;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Smooth ROC curves are a variation of classic ROC curves that takes the scores
 * into account.
 * <p>
 * Reference:
 * <p>
 * W. Klement, P. A. Flach, N. Japkowicz, S. Matwin<br>
 * Smooth Receiver Operating Characteristics (smROC) Curves.<br>
 * European Conf. Machine Learning and Principles and Practice of Knowledge
 * Discovery in Databases (ECML-PKDD'11)
 * <p>
 * However, this method has some deficiencies when the mean score is not 0.5, as
 * discussed in:
 * <p>
 * Erich Schubert, Remigius Wojdanowski, Arthur Zimek, Hans-Peter Kriegel<br>
 * On Evaluation of Outlier Rankings and Outlier Scores<br>
 * Proc. 12th SIAM Int. Conf. on Data Mining (SDM 2012)
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @assoc - - - OutlierResult
 * @navhas - create - SmROCResult
 */
@Reference(authors = "W. Klement, P. A. Flach, N. Japkowicz, S. Matwin", //
    title = "Smooth Receiver Operating Characteristics (smROC) Curves", //
    booktitle = "European Conf. Machine Learning and Principles and Practice of Knowledge Discovery in Databases (ECML-PKDD'11)", //
    url = "https://doi.org/10.1007/978-3-642-23783-6_13", //
    bibkey = "DBLP:conf/pkdd/KlementFJM11")
public class OutlierSmROCCurve implements Evaluator {
  /**
   * The label we use for marking ROCAUC values.
   */
  public static final String SMROCAUC_LABEL = "ROCAUC";

  /**
   * The logger.
   */
  private static final Logging LOG = Logging.getLogger(OutlierSmROCCurve.class);

  /**
   * Stores the "positive" class.
   */
  private Pattern positiveClassName;

  /**
   * Constructor.
   *
   * @param positive_class_name Positive class name pattern
   */
  public OutlierSmROCCurve(Pattern positive_class_name) {
    super();
    this.positiveClassName = positive_class_name;
  }

  private SmROCResult computeSmROCResult(SetDBIDs positiveids, OutlierResult or) {
    DoubleRelation scores = or.getScores();
    final int size = scores.size();

    // Compute mean, for inversion
    double mean = 0.0;
    for(DBIDIter iditer = scores.iterDBIDs(); iditer.valid(); iditer.advance()) {
      mean += scores.doubleValue(iditer) / size;
    }

    SmROCResult curve = new SmROCResult(positiveids.size() + 2);

    // start in bottom left
    curve.add(0.0, 0.0);

    int poscnt = 0, negcnt = 0;
    double prevscore = Double.NaN;
    double x = 0, y = 0;
    for(DBIDIter nei = or.getOrdering().order(or.getOrdering().getDBIDs()).iter(); nei.valid(); nei.advance()) {
      // Analyze next point
      final double curscore = scores.doubleValue(nei);
      // defer calculation for ties
      if(!Double.isNaN(prevscore) && (Double.compare(prevscore, curscore) == 0)) {
        // positive or negative match?
        if(positiveids.contains(nei)) {
          poscnt += 1;
        }
        else {
          negcnt += 1;
        }
        continue;
      }
      else {
        // Add point for *previous* result (since we are no longer tied with it)
        if(prevscore > mean) {
          y += poscnt * prevscore + negcnt * (1.0 - prevscore);
          x += poscnt * (1.0 - prevscore) + negcnt * prevscore;
        }
        else if(prevscore < mean) {
          y += poscnt * (1.0 - prevscore) + negcnt * prevscore;
          x += poscnt * prevscore + negcnt * (1.0 - prevscore);
        }
        curve.addAndSimplify(x, y);
        // positive or negative match?
        if(positiveids.contains(nei)) {
          poscnt = 1;
          negcnt = 0;
        }
        else {
          poscnt = 0;
          negcnt = 1;
        }
        prevscore = curscore;
      }
    }
    // Last point
    {
      if(prevscore > mean) {
        y += poscnt * prevscore + negcnt * (1.0 - prevscore);
        x += poscnt * (1.0 - prevscore) + negcnt * prevscore;
      }
      else if(prevscore < mean) {
        y += poscnt * (1.0 - prevscore) + negcnt * prevscore;
        x += poscnt * prevscore + negcnt * (1.0 - prevscore);
      }
      curve.addAndSimplify(x, y);
    }

    double rocauc = XYCurve.areaUnderCurve(curve) / (x * y);
    if(LOG.isVerbose()) {
      LOG.verbose(SMROCAUC_LABEL + ": " + rocauc);
    }
    curve.rocauc = rocauc;

    return curve;
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

    List<OutlierResult> oresults = OutlierResult.getOutlierResults(result);
    for(OutlierResult o : oresults) {
      Metadata.hierarchyOf(o).addChild(computeSmROCResult(positiveids, o));
    }
  }

  /**
   * Result object for Smooth ROC curves.
   *
   * @author Erich Schubert
   */
  public static class SmROCResult extends XYCurve {
    /**
     * ROC AUC score
     */
    double rocauc = Double.NaN;

    /**
     * Constructor.
     *
     * @param size Size estimate
     */
    public SmROCResult(int size) {
      super("SmROC Negative", "SmROC Positive", size);
      Metadata.of(this).setLongName("SmROC Curve");
    }

    /**
     * SmROC AUC value
     *
     * @return SmROC auc value
     */
    public double getAUC() {
      return rocauc;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Pattern for positive class.
     */
    protected Pattern positiveClassName = null;

    @Override
    public void configure(Parameterization config) {
      new PatternParameter(OutlierROCCurve.Par.POSITIVE_CLASS_NAME_ID) //
          .grab(config, x -> positiveClassName = x);
    }

    @Override
    public OutlierSmROCCurve make() {
      return new OutlierSmROCCurve(positiveClassName);
    }
  }
}
