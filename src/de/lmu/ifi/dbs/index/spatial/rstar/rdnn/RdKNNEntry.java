package de.lmu.ifi.dbs.index.spatial.rstar.rdnn;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.spatial.SpatialEntry;

/**
 * Defines the requirements for an entry in an RdKNN-Tree node. Additionally to an
 * entry in an R*-Tree an RDkNNEntry holds the knn distance of the underlying
 * data object or RdKNN-Tree node.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

interface RdKNNEntry<D extends Distance> extends SpatialEntry {
  /**
   * Returns the knn distance of the underlying data object or RdKNN-Tree node.
   *
   * @return the knn distance of the underlying data object or RdKNN-Tree node
   */
  public D getKnnDistance();

  /**
   * Sets the knn distance of the underlying data object or RdKNN-Tree node.
   *
   * @param knnDistance the knn distance to be set
   */
  public void setKnnDistance(D knnDistance);
}
