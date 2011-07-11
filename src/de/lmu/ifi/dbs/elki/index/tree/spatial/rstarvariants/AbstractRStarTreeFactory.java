package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.BulkSplit;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
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
  public static OptionID INSERTION_CANDIDATES_ID = OptionID.getOrCreateOptionID("rtree.insertion-candidates", "defines how many children are tested for finding the child generating the least overlap when inserting an object. Default 0 means all children.");

  /**
   * Parameter for bulk strategy
   */
  public static final OptionID BULK_SPLIT_ID = OptionID.getOrCreateOptionID("spatial.bulkstrategy", "The class to perform the bulk split with.");

  /**
   * Defines how many children are tested for finding the child generating the
   * least overlap when inserting an object. Default 0 means all children
   */
  protected int insertionCandidates = 0;

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
   * @param insertionCandidates
   */
  public AbstractRStarTreeFactory(String fileName, int pageSize, long cacheSize, BulkSplit bulkSplitter, int insertionCandidates) {
    super(fileName, pageSize, cacheSize);
    this.insertionCandidates = insertionCandidates;
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

    protected int insertionCandidates = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configBulkLoad(config);
      IntParameter insertionCandidatesP = new IntParameter(INSERTION_CANDIDATES_ID, true);
      if(config.grab(insertionCandidatesP)) {
        insertionCandidates = insertionCandidatesP.getValue();
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