package de.lmu.ifi.dbs.elki.index.tree.spatial;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;
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
 * @apiviz.stereotype factory,interface
 * @apiviz.uses SpatialIndexTree oneway - - «create»
 * 
 * @param <O> Object type
 * @param <I> Index type
 */
public abstract class SpatialIndexFactory<O extends NumberVector<?, ?>, I extends SpatialIndexTree<?, ?> & Index> extends TreeIndexFactory<O, I> {
  /**
   * Parameter for bulk loading
   */
  public static final OptionID BULK_LOAD_ID = OptionID.getOrCreateOptionID("spatial.bulk", "flag to specify bulk load (default is no bulk load)");

  /**
   * Parameter for bulk strategy
   */
  public static final OptionID BULK_LOAD_STRATEGY_ID = OptionID.getOrCreateOptionID("spatial.bulkstrategy", "the strategy for bulk load, available strategies are: [" + BulkSplit.Strategy.MAX_EXTENSION + "| " + BulkSplit.Strategy.ZCURVE + "]" + "(default is " + BulkSplit.Strategy.ZCURVE + ")");

  /**
   * If true, a bulk load will be performed.
   */
  protected boolean bulk;

  /**
   * The strategy for bulk load.
   */
  protected BulkSplit.Strategy bulkLoadStrategy;

  /**
   * Constructor.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param bulk
   * @param bulkLoadStrategy
   */
  protected SpatialIndexFactory(String fileName, int pageSize, long cacheSize, boolean bulk, Strategy bulkLoadStrategy) {
    super(fileName, pageSize, cacheSize);
    this.bulk = bulk;
    this.bulkLoadStrategy = bulkLoadStrategy;
  }

  /**
   * Constructor with bulk load disabled.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   */
  protected SpatialIndexFactory(String fileName, int pageSize, long cacheSize) {
    super(fileName, pageSize, cacheSize);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer<O extends NumberVector<?, ?>> extends TreeIndexFactory.Parameterizer<O> {
    protected boolean bulk = false;

    protected BulkSplit.Strategy bulkLoadStrategy = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // configBulkLoad(config);
    }

    /**
     * Configure the bulk load parameters.
     * 
     * @param config Parameterization
     */
    protected void configBulkLoad(Parameterization config) {
      Flag BULK_LOAD_FLAG = new Flag(BULK_LOAD_ID);
      if(config.grab(BULK_LOAD_FLAG)) {
        bulk = BULK_LOAD_FLAG.getValue();
      }

      StringParameter BULK_LOAD_STRATEGY_PARAM = new StringParameter(BULK_LOAD_STRATEGY_ID, new EqualStringConstraint(new String[] { BulkSplit.Strategy.MAX_EXTENSION.toString(), BulkSplit.Strategy.ZCURVE.toString() }), BulkSplit.Strategy.ZCURVE.toString());
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
    }

    @Override
    protected abstract SpatialIndexFactory<O, ?> makeInstance();
  }
}