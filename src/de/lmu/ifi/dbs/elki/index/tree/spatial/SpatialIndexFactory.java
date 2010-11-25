package de.lmu.ifi.dbs.elki.index.tree.spatial;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.EqualStringConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Spatial index factory base class.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndex - - produces
 *
 * @param <O> Object type
 * @param <I> Index type
 */
public abstract class SpatialIndexFactory<O extends NumberVector<?, ?>, I extends SpatialIndex<O, ?, ?>> extends TreeIndexFactory<O, I> {
  /**
   * OptionID for {@link #BULK_LOAD_FLAG}
   */
  public static final OptionID BULK_LOAD_ID = OptionID.getOrCreateOptionID("spatial.bulk", "flag to specify bulk load (default is no bulk load)");

  /**
   * Parameter for bulk loading
   */
  private final Flag BULK_LOAD_FLAG = new Flag(BULK_LOAD_ID);

  /**
   * OptionID for {@link #BULK_LOAD_STRATEGY_PARAM}
   */
  public static final OptionID BULK_LOAD_STRATEGY_ID = OptionID.getOrCreateOptionID("spatial.bulkstrategy", "the strategy for bulk load, available strategies are: [" + BulkSplit.Strategy.MAX_EXTENSION + "| " + BulkSplit.Strategy.ZCURVE + "]" + "(default is " + BulkSplit.Strategy.ZCURVE + ")");

  /**
   * Parameter for bulk strategy
   */
  private final StringParameter BULK_LOAD_STRATEGY_PARAM = new StringParameter(BULK_LOAD_STRATEGY_ID, new EqualStringConstraint(new String[] { BulkSplit.Strategy.MAX_EXTENSION.toString(), BulkSplit.Strategy.ZCURVE.toString() }), BulkSplit.Strategy.ZCURVE.toString());

  /**
   * If true, a bulk load will be performed.
   */
  protected boolean bulk;

  /**
   * The strategy for bulk load.
   */
  protected BulkSplit.Strategy bulkLoadStrategy;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public SpatialIndexFactory(Parameterization config) {
    super(config);
    config = config.descend(this);
    if(config.grab(BULK_LOAD_FLAG)) {
      bulk = BULK_LOAD_FLAG.getValue();
    }
    config.grab(BULK_LOAD_STRATEGY_PARAM);
    if(bulk) {
      String strategy = BULK_LOAD_STRATEGY_PARAM.getValue();

      if(strategy.equals(BulkSplit.Strategy.MAX_EXTENSION.toString())) {
        bulkLoadStrategy = BulkSplit.Strategy.MAX_EXTENSION;
      }
      else if(strategy.equals(BulkSplit.Strategy.ZCURVE.toString())) {
        bulkLoadStrategy = BulkSplit.Strategy.ZCURVE;
      }
      else {
        config.reportError(new WrongParameterValueException(BULK_LOAD_STRATEGY_PARAM, strategy));
      }
    }
    // TODO: specify constraint?
  }
}
