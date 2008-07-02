package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktab;

import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

import java.util.List;

/**
 * Defines the requirements for an entry in an MkCop-Tree node.
 * Additionally to an entry in an M-Tree an MkTabEntry holds a list of knn distances
 * for for parameters k <= k_max of the underlying data object or MkTab-Tree node.
 *
 * @author Elke Achtert 
 */
interface MkTabEntry<D extends Distance<D>> extends MTreeEntry<D> {
  /**
   * Returns the list of knn distances of the entry.
   *
   * @return the list of  knn distances of the entry
   */
  public List<D> getKnnDistances();

  /**
   * Sets the knn distances of the entry.
   *
   * @param knnDistances the knn distances to be set
   */
  public void setKnnDistances(List<D> knnDistances);

  /**
   * Returns the knn distance of the entry for the specified parameter k.
   *
   * @param k the parameter k of the knn distance
   * @return the knn distance of the entry
   */
  public D getKnnDistance(int k);

  /**
   * Returns the parameter k_max.
   *
   * @return the parameter k_max
   */
  public int getK_max();

}
