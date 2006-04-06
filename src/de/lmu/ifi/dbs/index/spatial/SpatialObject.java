package de.lmu.ifi.dbs.index.spatial;

/**
 * Defines the requirements for an object that can be indexed by a SpatialIndex.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface SpatialObject {
  /**
   * Returns the id of this spatial object.
   *
   * @return the id of this spatial object
   */
  Integer getID();

  /**
   * Returns the dimensionality of this spatial object.
   *
   * @return the dimensionality of this spatial object
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
}
