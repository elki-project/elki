package de.lmu.ifi.dbs.index.metrical.mtree.mkmax;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.metrical.mtree.Entry;

import java.util.List;

/**
 * Defines the requirements for an entry in a MkMax-Tree node. Additionally to an entry in a M-Tree
 * getter and setter methods for the knn distances are provided.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

interface MkMaxEntry<D extends Distance> extends Entry<D> {
  /**
   * Returns the knn distances of the object.
   *
   * @return the knn distances of the object
   */
  public List<D> getKnnDistances();

  /**
   * Sets the knn distances of the object.
   *
   * @param knnDistances he knn distance to be set
   */
  public void setKnnDistances(List<D> knnDistances);

  /**
   * Returns the knn distance of the object.
   *
   * @param k the parameter k of the knn distance
   * @return the knn distance of the object
   */
  public D getKnnDistance(int k);

  /**
   * Returns the parameter k.
   * @return the parameter k
   */
  public int getK();

}
