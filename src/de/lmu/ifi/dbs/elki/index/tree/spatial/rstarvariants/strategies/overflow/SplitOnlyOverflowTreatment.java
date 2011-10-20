package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.overflow;

import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Always split, as in the original R-Tree
 * 
 * @author Erich Schubert
 */
public class SplitOnlyOverflowTreatment implements OverflowTreatment {
  /**
   * Static instance
   */
  public static final SplitOnlyOverflowTreatment STATIC = new SplitOnlyOverflowTreatment();

  /**
   * Constructor
   */
  public SplitOnlyOverflowTreatment() {
    super();
  }

  @Override
  public <N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> boolean handleOverflow(AbstractRStarTree<N, E> tree, N node, IndexTreePath<E> path) {
    return false;
  }

  @Override
  public void reinitialize() {
    // Nothing to do
  }


  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected SplitOnlyOverflowTreatment makeInstance() {
      return SplitOnlyOverflowTreatment.STATIC;
    }
  }
}