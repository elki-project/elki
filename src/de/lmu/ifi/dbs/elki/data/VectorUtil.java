package de.lmu.ifi.dbs.elki.data;

import de.lmu.ifi.dbs.elki.math.DoubleMinMax;

/**
 * Utility functions for use with vectors.
 * 
 * Note: obviously, many functions are class methods or database related.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.data.NumberVector
 */
public final class VectorUtil {
  /**
   * Return the range across all dimensions. Useful in particular for time series.
   * 
   * @param vec Vector to process.
   * @param <V> Vector type 
   * @return [min, max]
   */
  public static <V extends NumberVector<?, ?>> DoubleMinMax getRangeDouble(V vec) {
    DoubleMinMax minmax = new DoubleMinMax();

    for(int i = 0; i < vec.getDimensionality(); i++) {
      minmax.put(vec.doubleValue(i + 1));
    }

    return minmax;
  }
}