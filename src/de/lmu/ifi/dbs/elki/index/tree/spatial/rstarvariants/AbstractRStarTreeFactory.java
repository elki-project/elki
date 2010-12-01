package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndexFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Abstract factory for R*-Tree based trees.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has AbstractRstarTree oneway - - produces
 *
 * @param <O> Object type
 * @param <I> Index type
 */
public abstract class AbstractRStarTreeFactory<O extends NumberVector<O, ?>, I extends AbstractRStarTree<O, ?, ?>> extends SpatialIndexFactory<O, I> {
  /**
   * Option ID for the fast-insertion parameter
   */
  public static OptionID INSERTION_CANDIDATES_ID = OptionID.getOrCreateOptionID("rtree.insertion-candidates", "defines how many children are tested for finding the child generating the least overlap when inserting an object. Default 0 means all children.");

  /**
   * Fast-insertion parameter. Optional.
   */
  private IntParameter INSERTION_CANDIDATES_PARAM = new IntParameter(INSERTION_CANDIDATES_ID, true);

  /**
   * Defines how many children are tested for finding the child generating the
   * least overlap when inserting an object. Default 0 means all children
   */
  protected int insertionCandidates = 0;

  /**
   * Constructor
   * 
   * @param config Configuration
   */
  public AbstractRStarTreeFactory(Parameterization config) {
    super(config);
    config = config.descend(this);
    if(config.grab(INSERTION_CANDIDATES_PARAM)) {
      insertionCandidates = INSERTION_CANDIDATES_PARAM.getValue();
    }
  }
}
