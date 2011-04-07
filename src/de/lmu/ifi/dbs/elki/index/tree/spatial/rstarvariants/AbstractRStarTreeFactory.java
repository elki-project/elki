package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndexFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

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
public abstract class AbstractRStarTreeFactory<O extends NumberVector<O, ?>, I extends AbstractRStarTree<O, ?, ?>> extends SpatialIndexFactory<O, I> {
  /**
   * Fast-insertion parameter. Optional.
   */
  public static OptionID INSERTION_CANDIDATES_ID = OptionID.getOrCreateOptionID("rtree.insertion-candidates", "defines how many children are tested for finding the child generating the least overlap when inserting an object. Default 0 means all children.");

  /**
   * Defines how many children are tested for finding the child generating the
   * least overlap when inserting an object. Default 0 means all children
   */
  protected int insertionCandidates = 0;

  /**
   * Constructor.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param bulk
   * @param bulkLoadStrategy
   * @param insertionCandidates
   */
  public AbstractRStarTreeFactory(String fileName, int pageSize, long cacheSize, boolean bulk, Strategy bulkLoadStrategy, int insertionCandidates) {
    super(fileName, pageSize, cacheSize, bulk, bulkLoadStrategy);
    this.insertionCandidates = insertionCandidates;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer<O extends NumberVector<O, ?>> extends SpatialIndexFactory.Parameterizer<O> {
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

    @Override
    protected abstract AbstractRStarTreeFactory<O, ?> makeInstance();
  }
}