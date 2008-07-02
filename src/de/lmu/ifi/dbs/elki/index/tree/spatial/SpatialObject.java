package de.lmu.ifi.dbs.elki.index.tree.spatial;

/**
 * Defines the requirements for objects that can be indexed by a Spatial Index,
 * which are spatial nodes or data objects.
 *
 * @author Elke Achtert 
 */
public interface SpatialObject extends SpatialComparable {
  /**
   * Returns the id of this spatial object. The ids over all spatial nodes and the ids over all
   * data objects have to be unique.
   *
   * @return the id of this spatial object
   */
  Integer getID();
}
