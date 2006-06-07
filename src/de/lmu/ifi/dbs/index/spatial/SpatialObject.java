package de.lmu.ifi.dbs.index.spatial;

/**
 * Defines the requirements for an object that can be indexed by a SpatialIndex.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface SpatialObject extends SpatialComparable {
  /**
   * Returns the id of this spatial object.
   *
   * @return the id of this spatial object
   */
  Integer getID();
}
