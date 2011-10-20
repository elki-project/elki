package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.reinsert;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayAdapter;

/**
 * Reinsertion strategy to resolve overflows in the RStarTree.
 * 
 * @author Erich Schubert
 */
public interface ReinsertStrategy {
  /**
   * Perform reinsertions.
   * 
   * @param entries Entries in overflowing node
   * @param getter Adapter for the entries array
   * @param page Spatial extend of the page 
   * @return true when a reinsert was performed.
   */
  public <E extends SpatialComparable, A> int[] computeReinserts(A entries, ArrayAdapter<E, A> getter, SpatialComparable page);
}
