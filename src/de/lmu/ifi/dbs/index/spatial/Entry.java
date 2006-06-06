package de.lmu.ifi.dbs.index.spatial;

import de.lmu.ifi.dbs.index.Identifier;

import java.io.Externalizable;

/**
 * Defines the requirements for an entry in a node of a Spatial Index.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Entry extends Externalizable, Identifier, SpatialObject{
  /**
   * Returns the MBR of the underlying spatial object of this entry.
   *
   * @return the MBR of the underlying spatial object of this entry
   */
  public MBR getMBR();

  /**
   * Returns true if this entry is a leaf entry, false otherwise.
   *
   * @return true if this entry is a leaf entry, false otherwise
   */
  public boolean isLeafEntry();
}
