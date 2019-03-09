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
import elki.evaluation.Evaluator;
import elki.logging.Logging;
import elki.math.geometry.XYCurve;
import elki.result.*;
import elki.result.outlier.OutlierResult;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Compute a curve containing the precision values for an outlier detection
 * method.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @has - - - PrecisionAtKCurve
 */
public class OutlierPrecisionAtKCurve implements Evaluator {
  /**
   * The logger.
   */
  private static final Logging LOG = Logging.getLogger(OutlierPrecisionAtKCurve.class);

  /**
   * Stores the "positive" class.
   */
  private Pattern positiveClassName;

  /**
   * Maximum value for k
   */
  private int maxk = Integer.MAX_VALUE;

  /**
   * Constructor.
   *
   * @param positiveClassName Pattern to recognize outliers
   * @param maxk Maximum value for k
   */
  public OutlierPrecisionAtKCurve(Pattern positiveClassName, int maxk) {
    super();
    this.positiveClassName = positiveClassName;
    this.maxk = maxk;
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
    List<OrderingResult> orderings = ResultUtil.getOrderingResults(result);
    // Outlier results are the main use case.
    for(OutlierResult o : oresults) {
      DBIDs sorted = o.getOrdering().order(o.getOrdering().getDBIDs());
      Metadata.hierarchyOf(o).addChild(computePrecisionResult(o.getScores().size(), positiveids, sorted));
      // Process them only once.
      orderings.remove(o.getOrdering());
    }

    // FIXME: find appropriate place to add the derived result
    // otherwise apply an ordering to the database IDs.
    for(OrderingResult or : orderings) {
      DBIDs sorted = or.order(or.getDBIDs());
      Metadata.hierarchyOf(or).addChild(computePrecisionResult(or.getDBIDs().size(), positiveids, sorted));
    }
  }

  private XYCurve computePrecisionResult(int size, SetDBIDs positiveids, DBIDs order) {
    if(order.size() != size) {
      throw new IllegalStateException("Iterable result doesn't match database size - incomplete ordering?");
    }
    int lastk = Math.min(size, maxk);
    XYCurve curve = new PrecisionAtKCurve(lastk);

    int pos = 0;
    DBIDIter i = order.iter();
    for(int k = 1; k <= lastk; k++, i.advance()) {
      if(positiveids.contains(i)) {
        pos++;
      }
      curve.addAndSimplify(k, pos / (double) k);
    }
    return curve;
  }

  /**
   * Precision at K curve.
   *
   * @author Erich Schubert
   */
  public static class PrecisionAtKCurve extends XYCurve {
    /**
     * Constructor.
     *
     * @param size Size estimation
     */
    public PrecisionAtKCurve(int size) {
      super("k", "Precision", size);
    }

    @Override
    public String getLongName() {
      return "Precision @ k Curve";
    }

    @Override
    public String getShortName() {
      return "precision-at-k";
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
    public static final OptionID POSITIVE_CLASS_NAME_ID = new OptionID("precision.positive", "Class label for the 'positive' class.");

    /**
     * Maximum value for k
     */
    public static final OptionID MAX_K_ID = new OptionID("precision.maxk", "Maximum value of 'k' to compute the curve up to.");

    /**
     * Stores the "positive" class.
     */
    private Pattern positiveClassName;

    /**
     * Maximum value for k
     */
    private int maxk = Integer.MAX_VALUE;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      PatternParameter positiveClassNameP = new PatternParameter(POSITIVE_CLASS_NAME_ID);
      if(config.grab(positiveClassNameP)) {
        positiveClassName = positiveClassNameP.getValue();
      }
      IntParameter maxkP = new IntParameter(MAX_K_ID) //
          .setOptional(true);
      if(config.grab(maxkP)) {
        maxk = maxkP.getValue();
      }
    }

    @Override
    protected OutlierPrecisionAtKCurve makeInstance() {
      return new OutlierPrecisionAtKCurve(positiveClassName, maxk);
    }
  }
}
