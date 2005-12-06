package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.Identifier;

import java.io.Externalizable;

/**
 * Defines the requirements for an entry in an M-Tree node.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public interface MTreeEntry<D extends Distance> extends Externalizable, Identifier {
  /**
   * Returns the id of the underlying metrical object of this entry, if this entry is a
   * leaf entry, the id of the routing object, otherwise.
   *
   * @return the id of the underlying metrical object of this entry, if this entry is a
   *         leaf entry, the id of the routing object, otherwise
   */
  Integer getObjectID();

  /**
   * Sets the id of the underlying metrical object of this entry, if this entry is a leaf entry,
   * the id of the routing object, otherwise.
   *
   * @param objectID the id to be set
   */
  void setObjectID(Integer objectID);

  /**
   * Returns the distance from the object to its parent object.
   *
   * @return the distance from the object to its parent object
   */
  D getParentDistance();

  /**
   * Sets the distance from the object to its parent object.
   *
   * @param parentDistance the distance to be set
   */
  public void setParentDistance(D parentDistance);

  /**
   * Returns true if this entry is a leaf entry, false otherwise.
   *
   * @return true if this entry is a leaf entry, false otherwise
   */
  public boolean isLeafEntry();


}
