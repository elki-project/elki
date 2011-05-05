package de.lmu.ifi.dbs.elki.data.spatial;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;

/**
 * Defines the required methods needed for comparison of spatial objects.
 *
 * @author Elke Achtert 
 */
public interface SpatialComparable {
  /**
   * Returns the dimensionality of the object.
   *
   * @return the dimensionality
   */
  int getDimensionality();

  /**
   * Returns the minimum coordinate at the specified dimension.
   *
   * @param dimension the dimension for which the coordinate should be returned,
   *                  where 1 &le; dimension &le; <code>getDimensionality()</code>
   * @return the minimum coordinate at the specified dimension
   */
  double getMin(int dimension);

  /**
   * Returns the maximum coordinate at the specified dimension.
   *
   * @param dimension the dimension for which the coordinate should be returned,
   *                  where 1 &le; dimension &le; <code>getDimensionality()</code>
   * @return the maximum coordinate at the specified dimension
   */
  double getMax(int dimension);
  
  /**
   * Get the objects complete MBR.
   * 
   * @return Object MBR
   * 
   * @deprecated This is an expensive operation. Avoid!
   */
  @Deprecated
  HyperBoundingBox getMBR();
}