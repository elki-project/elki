package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;

/**
 * Use two different insertion strategies for directory and leaf nodes.
 * 
 * Using two different strategies was likely first suggested in:
 * <p>
 * N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger:<br />
 * The R*-tree: an efficient and robust access method for points and rectangles<br />
 * in: Proceedings of the 1990 ACM SIGMOD International Conference on Management
 * of Data, Atlantic City, NJ, May 23-25, 1990
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger", title = "The R*-tree: an efficient and robust access method for points and rectangles", booktitle = "Proceedings of the 1990 ACM SIGMOD International Conference on Management of Data, Atlantic City, NJ, May 23-25, 1990", url = "http://dx.doi.org/10.1145/93597.98741")
public class CombinedInsertionStrategy implements InsertionStrategy {
  /**
   * Strategy when inserting into directory nodes
   */
  InsertionStrategy dirStrategy;

  /**
   * Strategy when inserting into leaf nodes.
   */
  InsertionStrategy leafStrategy;

  /**
   * Constructor.
   * 
   * @param dirStrategy Strategy for directory nodes
   * @param leafStrategy Strategy for leaf nodes
   */
  public CombinedInsertionStrategy(InsertionStrategy dirStrategy, InsertionStrategy leafStrategy) {
    super();
    this.dirStrategy = dirStrategy;
    this.leafStrategy = leafStrategy;
  }

  @Override
  public <A> int choose(A options, ArrayAdapter<? extends SpatialComparable, A> getter, SpatialComparable obj, int height, int depth) {
    if(depth + 1 >= height) {
      return leafStrategy.choose(options, getter, obj, height, depth);
    }
    else {
      return dirStrategy.choose(options, getter, obj, height, depth);
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
    /**
     * Insertion strategy for directory nodes.
     */
    public static final OptionID DIR_STRATEGY_ID = OptionID.getOrCreateOptionID("rtree.insert-directory", "Insertion strategy for directory nodes.");

    /**
     * Insertion strategy for leaf nodes.
     */
    public static final OptionID LEAF_STRATEGY_ID = OptionID.getOrCreateOptionID("rtree.insert-leaf", "Insertion strategy for leaf nodes.");

    /**
     * Strategy when inserting into directory nodes
     */
    InsertionStrategy dirStrategy;

    /**
     * Strategy when inserting into leaf nodes.
     */
    InsertionStrategy leafStrategy;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ClassParameter<InsertionStrategy> dirP = new ClassParameter<InsertionStrategy>(DIR_STRATEGY_ID, InsertionStrategy.class, LeastEnlargementWithAreaInsertionStrategy.class);
      if(config.grab(dirP)) {
        dirStrategy = dirP.instantiateClass(config);
      }

      ClassParameter<InsertionStrategy> leafP = new ClassParameter<InsertionStrategy>(LEAF_STRATEGY_ID, InsertionStrategy.class, LeastOverlapInsertionStrategy.class);
      if(config.grab(leafP)) {
        leafStrategy = leafP.instantiateClass(config);
      }
    }

    @Override
    protected CombinedInsertionStrategy makeInstance() {
      return new CombinedInsertionStrategy(dirStrategy, leafStrategy);
    }
  }
}