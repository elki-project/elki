package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.overflow;

import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;

/**
 * Reinsertion strategy to resolve overflows in the RStarTree.
 * 
 * @author Erich Schubert
 */
public interface OverflowTreatment {
  /**
   * Reinitialize the reinsertion treatment (for a new primary insertion).
   */
  public void reinitialize();
  
  /**
   * Handle overflow in the given node.
   * 
   * @param <N> Node
   * @param <E> Entry
   * @param tree Tree
   * @param node Node
   * @param path Path
   * @return true when already handled (e.g. by reinserting)
   */
  <N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> boolean handleOverflow(AbstractRStarTree<N, E> tree, N node, IndexTreePath<E> path);
}