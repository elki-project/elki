package de.lmu.ifi.dbs.index.spatial;

/**
 * Defines the requirements for an object that can be used in a SpatialIndex.
 * A spatial object can be a spatial node or a spatial data object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface SpatialObject extends Comparable {
  /**
   * Returns the parent id of this spatial object.
   *
   * @return the parent id of this spatial object
   */
  int getParentID();

  /**
   * Returns the dimensionality of this spatial object.
   *
   * @return the dimensionality of this spatial object
   */
  int getDimensionality();

  /**
   * Computes and returns the MBR of this spatial object.
   *
   * @return the MBR of this spatial object
   */
  MBR mbr();

//  boolean isNode();



//  double getMinValue(int index);

//  double getMaxValue(int index);


}
