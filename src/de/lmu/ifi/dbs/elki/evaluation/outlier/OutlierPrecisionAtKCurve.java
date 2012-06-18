package de.lmu.ifi.dbs.elki.evaluation.outlier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntDoublePair;

/**
 * Compute a curve containing the precision values for an outlier detection
 * method.
 * 
 * @author Erich Schubert
 * 
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
      Iterator<DBID> iter = o.getOrdering().iter(o.getOrdering().getDBIDs());
      db.getHierarchy().add(o, computePrecisionResult(o.getScores().size(), positiveids, iter));
      // Process them only once.
      orderings.remove(o.getOrdering());
    }

    // FIXME: find appropriate place to add the derived result
    // otherwise apply an ordering to the database IDs.
    for(OrderingResult or : orderings) {
      Iterator<DBID> iter = or.iter(or.getDBIDs());
      db.getHierarchy().add(or, computePrecisionResult(or.getDBIDs().size(), positiveids, iter));
    }
  }

  private CollectionResult<IntDoublePair> computePrecisionResult(int size, SetDBIDs positiveids, Iterator<DBID> iter) {
    ArrayModifiableDBIDs order = DBIDUtil.newArray(size);
    while(iter.hasNext()) {
      Object o = iter.next();
      if(!(o instanceof DBID)) {
        throw new IllegalStateException("Iterable result contained non-DBID - result didn't satisfy requirements");
      }
      else {
        order.add((DBID) o);
      }
    }
    if(order.size() != size) {
      throw new IllegalStateException("Iterable result doesn't match database size - incomplete ordering?");
    }
    int lastk = Math.min(size, maxk);
    List<IntDoublePair> roccurve = new ArrayList<IntDoublePair>(lastk);

    int pos = 0;
    DBIDIter i = order.iter();
    for(int k = 1; k <= lastk; k++, i.advance()) {
      if(positiveids.contains(i.getDBID())) {
        pos++;
      }
      roccurve.add(new IntDoublePair(k, (pos * 1.0) / k));
    }
    String name = "Precision @ " + lastk + " " + ((pos * 1.0) / lastk);
    if(logger.isVerbose()) {
      logger.verbose(name);
    }

    List<String> header = new ArrayList<String>(1);
    header.add(name);
    final CollectionResult<IntDoublePair> rocresult = new CollectionResult<IntDoublePair>(name, "precisionatk", roccurve, header);

    return rocresult;
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