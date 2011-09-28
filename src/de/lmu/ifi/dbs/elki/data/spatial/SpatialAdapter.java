package de.lmu.ifi.dbs.elki.data.spatial;

/**
 * Class to handle virtual bounding rectangles. This class abstracts the actual
 * data objects (points, rectangles, ...) and representations from the needs of
 * an R-tree (in which essentially everything is a rectangle).
 * 
 * NOTE: the SpatialAdapter interface starts indexing at 0.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public interface SpatialAdapter<O> {
  /**
   * Get the dimensionality.
   * 
   * @param obj Object
   * @return dimensionality
   */
  int getDimensionality(O obj);

  /**
   * Get the minimum value in a single dimension.
   * 
   * @param obj Object
   * @param dim Dimension, starting at 0
   * @return Minimum value
   */
  double getMin(O obj, int dim);

  /**
   * Get the maximum value in a single dimension.
   * 
   * @param obj Object
   * @param dim Dimension, starting at 0
   * @return Maximum value
   */
  double getMax(O obj, int dim);

  /**
   * Get the length (extend, <code>max-min</code>) in a single dimension.
   * 
   * @param obj Object
   * @param dim Dimension, starting at 0
   * @return Length == max-min
   */
  // While this is redundant, it saves a computation for point objects, which are common.
  double getLen(O obj, int dim);
  
  /**
   * Compute the area (volume) of the object.
   * 
   * @param obj Object
   * @return Volume/area
   */
  // While this is redundant, it saves computations for point objects, which are common.
  double getArea(O obj);
}