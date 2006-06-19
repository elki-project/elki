package de.lmu.ifi.dbs.index.spatial;

import de.lmu.ifi.dbs.index.Entry;

/**
 * Defines the requirements for an entry in a node of a Spatial Index.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface SpatialEntry extends Entry, SpatialComparable {
  /**
   * Returns the MBR of the underlying spatial object of this entry.
   *
   * @return the MBR of the underlying spatial object of this entry
   */
  public MBR getMBR();

  /**
   * Sets the MBR of this entry. This method is only supported, if this entry is
   * a directory entry.
   *
   * @param mbr the MBR to be set
   */
  public void setMBR(MBR mbr);
}
