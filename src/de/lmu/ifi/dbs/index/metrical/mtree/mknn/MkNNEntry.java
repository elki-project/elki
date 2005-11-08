package de.lmu.ifi.dbs.index.metrical.mtree.mknn;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.metrical.mtree.Entry;

/**
 * Defines the requirements for an entry in a MkNN-Tree node. Additionally to an entry in a M-Tree
 * getter and setter methods for the knn distance are provided.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

interface MkNNEntry<D extends Distance> extends Entry<D> {
  /**
   * Returns the knn distance of the object.
   *
   * @return the knn distance of the object
   */
  public D getKnnDistance();

  /**
   * Sets the knn distance of the object.
   *
   * @param knnDistance he knn distance to be set
   */
  public void setKnnDistance(D knnDistance);
}
