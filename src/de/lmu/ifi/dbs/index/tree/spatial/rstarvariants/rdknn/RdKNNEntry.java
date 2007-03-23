package de.lmu.ifi.dbs.index.tree.spatial.rstarvariants.rdknn;

import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.tree.spatial.SpatialEntry;

/**
 * Defines the requirements for an entry in an RdKNN-Tree node. Additionally to an
 * entry in an R*-Tree an RDkNNEntry holds the knn distance of the underlying
 * data object or RdKNN-Tree node.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

interface RdKNNEntry<D extends NumberDistance> extends SpatialEntry {
  /**
   * Returns the knn distance of this entry.
   *
   * @return the knn distance of this entry
   */
  public D getKnnDistance();

  /**
   * Sets the knn distance of this entry.
   *
   * @param knnDistance the knn distance to be set
   */
  public void setKnnDistance(D knnDistance);
}
