package de.lmu.ifi.dbs.index.spatial;

import de.lmu.ifi.dbs.index.Entry;

import java.io.Externalizable;

/**
 * Defines the requirements for an entry in a node of a Spatial Index.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface SpatialEntry extends Externalizable, Entry {
  /**
   * Returns the MBR of the underlying spatial object of this entry.
   *
   * @return the MBR of the underlying spatial object of this entry
   */
  public MBR getMBR();
}
