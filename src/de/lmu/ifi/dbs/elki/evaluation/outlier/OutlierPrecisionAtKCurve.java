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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Compute a curve containing the precision values for an outlier detection
 * method.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has PrecisionAtKCurve
 */
public class OutlierPrecisionAtKCurve implements Evaluator {
  /**
   * The logger.
   */
  private static final Logging logger = Logging.getLogger(OutlierPrecisionAtKCurve.class);

  /**
   * The pattern to identify positive classes.
   * 
   * <p>
   * Key: {@code -precision.positive}
   * </p>
   */
  public static final OptionID POSITIVE_CLASS_NAME_ID = OptionID.getOrCreateOptionID("precision.positive", "Class label for the 'positive' class.");

  /**
   * Maximum value for k
   * 
   * <p>
   * Key: {@code -precision.k}
   * </p>
   */
  public static final OptionID MAX_K_ID = OptionID.getOrCreateOptionID("precision.maxk", "Maximum value of 'k' to compute the curve up to.");

  /**
   * Stores the "positive" class.
   */
  private Pattern positiveClassName;

  /**
   * Maximum value for k
   */
  int maxk = Integer.MAX_VALUE;

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
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    Database db = ResultUtil.findDatabase(baseResult);
    // Prepare
    SetDBIDs positiveids = DBIDUtil.ensureSet(DatabaseUtil.getObjectsByLabelMatch(db, positiveClassName));

    if(positiveids.size() == 0) {
      logger.warning("Computing a ROC curve failed - no objects matched.");
      return;
    }

    List<OutlierResult> oresults = ResultUtil.getOutlierResults(result);
    List<OrderingResult> orderings = ResultUtil.getOrderingResults(result);
    // Outlier results are the main use case.
    for(OutlierResult o : oresults) {
      DBIDs sorted = o.getOrdering().iter(o.getOrdering().getDBIDs());
      db.getHierarchy().add(o, computePrecisionResult(o.getScores().size(), positiveids, sorted));
      // Process them only once.
      orderings.remove(o.getOrdering());
    }

    // FIXME: find appropriate place to add the derived result
    // otherwise apply an ordering to the database IDs.
    for(OrderingResult or : orderings) {
      DBIDs sorted = or.iter(or.getDBIDs());
      db.getHierarchy().add(or, computePrecisionResult(or.getDBIDs().size(), positiveids, sorted));
    }
  }

  private XYCurve computePrecisionResult(int size, SetDBIDs positiveids, DBIDs order) {
    if(order.size() != size) {
      throw new IllegalStateException("Iterable result doesn't match database size - incomplete ordering?");
    }
    int lastk = Math.min(size, maxk);
    XYCurve curve = new PrecisionAtKCurve("k", "Precision", lastk);

    int pos = 0;
    DBIDIter i = order.iter();
    for(int k = 1; k <= lastk; k++, i.advance()) {
      if(positiveids.contains(i)) {
        pos++;
      }
      curve.addAndSimplify(k, pos / (double) k);
    }
    if(logger.isVerbose()) {
      logger.verbose("Precision @ " + lastk + " " + ((pos * 1.0) / lastk));
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
    public PrecisionAtKCurve(String labelx, String labely, int size) {
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

    @Override
    public void writeToText(TextWriterStream out, String label) {
      final int last = size() - 1;
      out.commentPrintLn("Precision @ " + ((int) getX(last)) + ": " + getY(last));
      out.commentPrintSeparator();
      out.flush();
      out.commentPrint(labelx);
      out.commentPrint(" ");
      out.commentPrint(labely);
      out.flush();
      for(int pos = 0; pos < data.size(); pos+=2) {
        out.inlinePrint((int)data.get(pos));
        out.inlinePrint(data.get(pos + 1));
        out.flush();
      }
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

    protected int maxk = Integer.MAX_VALUE;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      PatternParameter positiveClassNameP = new PatternParameter(POSITIVE_CLASS_NAME_ID);
      if(config.grab(positiveClassNameP)) {
        positiveClassName = positiveClassNameP.getValue();
      }
      IntParameter maxkP = new IntParameter(MAX_K_ID, true);
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