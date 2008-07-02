package de.lmu.ifi.dbs.elki.index.tree.spatial;

import de.lmu.ifi.dbs.elki.index.tree.Entry;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;

/**
 * Defines the requirements for an entry in a node of a Spatial Index.
 *
 * @author Elke Achtert 
 */
public interface SpatialEntry extends Entry, SpatialComparable {
  /**
   * Returns the MBR of the underlying spatial object of this entry.
   *
   * @return the MBR of the underlying spatial object of this entry
   */
  public HyperBoundingBox getMBR();

  /**
   * Sets the MBR of this entry. This method is only supported, if this entry is
   * a directory entry.
   *
   * @param mbr the MBR to be set
   */
  public void setMBR(HyperBoundingBox mbr);
}
