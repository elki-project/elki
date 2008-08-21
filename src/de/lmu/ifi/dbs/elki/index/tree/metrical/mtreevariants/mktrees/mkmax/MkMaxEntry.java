package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Defines the requirements for an entry in an MkMax-Tree node.
 * Additionally to an entry in an M-Tree an MkMaxEntry holds the knn distance of the underlying
 * data object or MkMax-Tree node.
 *
 * @author Elke Achtert 
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
