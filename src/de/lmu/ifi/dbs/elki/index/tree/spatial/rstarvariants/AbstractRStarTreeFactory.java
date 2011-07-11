package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.BulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.InsertionStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.LeastOverlapInsertionStrategy;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract factory for R*-Tree based trees.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses AbstractRStarTree oneway - - «create»
 * 
 * @param <O> Object type
 * @param <I> Index type
 */
public abstract class AbstractRStarTreeFactory<O extends NumberVector<O, ?>, I extends AbstractRStarTree<?, ?> & Index> extends TreeIndexFactory<O, I> {
  /**
   * Fast-insertion parameter. Optional.
   */
  public static OptionID INSERTION_STRATEGY_ID = OptionID.getOrCreateOptionID("rtree.insertionstrategy", "The strategy to use for object insertion.");

  /**
   * Parameter for bulk strategy
   */
  public static final OptionID BULK_SPLIT_ID = OptionID.getOrCreateOptionID("spatial.bulkstrategy", "The class to perform the bulk split with.");

  /**
   * Strategy to find the insertion node with.
   */
  protected InsertionStrategy insertionStrategy;

  /**
   * The strategy for bulk load.
   */
  protected BulkSplit bulkSplitter;

  /**
   * Constructor.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param bulkSplitter the strategy to use for bulk splitting
   * @param insertionStrategy the strategy to find the insertion child
   */
  public AbstractRStarTreeFactory(String fileName, int pageSize, long cacheSize, BulkSplit bulkSplitter, InsertionStrategy insertionStrategy) {
    super(fileName, pageSize, cacheSize);
    this.insertionStrategy = insertionStrategy;
    this.bulkSplitter = bulkSplitter;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer<O extends NumberVector<O, ?>> extends TreeIndexFactory.Parameterizer<O> {
    protected BulkSplit bulkSplitter = null;

    protected InsertionStrategy insertionStrategy = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configBulkLoad(config);
      ClassParameter<InsertionStrategy> insertionStrategyP = new ClassParameter<InsertionStrategy>(INSERTION_STRATEGY_ID, InsertionStrategy.class, LeastOverlapInsertionStrategy.class);
      if(config.grab(insertionStrategyP)) {
        insertionStrategy = insertionStrategyP.instantiateClass(config);
      }
    }

    /**
     * Configure the bulk load parameters.
     * 
     * @param config Parameterization
     */
    protected void configBulkLoad(Parameterization config) {
      ObjectParameter<BulkSplit> bulkSplitP = new ObjectParameter<BulkSplit>(BULK_SPLIT_ID, BulkSplit.class, true);
      if(config.grab(bulkSplitP)) {
        bulkSplitter = bulkSplitP.instantiateClass(config);
      }
    }

    @Override
    protected abstract AbstractRStarTreeFactory<O, ?> makeInstance();
  }
}