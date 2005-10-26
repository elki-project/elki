package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.Identifier;

import java.io.Externalizable;

/**
 * Defines the requirements for an entry in a M-Tree node.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public interface Entry<O extends MetricalObject, D extends Distance> extends Externalizable, Identifier {
  /**
   * Returns the feature value (object) of this entry.
   *
   * @return the feature value of this entry
   */
  O getObject();

  /**
   * Sets the object of this entry.
   * @param object the object to be set
   */
  void setObject(O object);

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
