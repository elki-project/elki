package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Defines the requirements for an entry in an
 * {@link de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax.MkMaxTreeNode}.
 * Additionally to an entry in an M-Tree an MkMaxEntry holds the k-nearest neighbor distance of the underlying
 * data object or MkMax-Tree node.
 *
 * @author Elke Achtert
 * @param <D> the type of Distance used in the MkMaxTree
 */
interface MkMaxEntry<D extends Distance<D>> extends MTreeEntry<D> {
  /**
   * Returns the knn distance of the entry.
   *
   * @return the knn distance of the entry
   */
  public D getKnnDistance();

  /**
   * Sets the knn distance of the entry.
   *
   * @param knnDistance the knn distance to be set
   */
  public void setKnnDistance(D knnDistance);
}
