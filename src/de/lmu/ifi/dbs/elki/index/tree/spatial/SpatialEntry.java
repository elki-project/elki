package de.lmu.ifi.dbs.elki.index.tree.spatial;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.index.tree.Entry;

/**
 * Defines the requirements for an entry in a node of a Spatial Index.
 *
 * @author Elke Achtert
 */
public interface SpatialEntry extends Entry, SpatialComparable {
  /**
   * Sets the MBR of this entry. This method is only supported, if this entry is
   * a directory entry.
   *
   * @param mbr the MBR to be set
   */
  // TODO: move into directory entry API.
  public void setMBR(HyperBoundingBox mbr);
}
