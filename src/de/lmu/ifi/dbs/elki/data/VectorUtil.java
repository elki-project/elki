package de.lmu.ifi.dbs.elki.data;

import de.lmu.ifi.dbs.elki.math.DoubleMinMax;

/**
 * Utility functions for use with vectors.
 * 
 * @author Erich Schubert
 */
public final class VectorUtil {
  /**
   * Return the range across all dimensions. Sensible for time series.
   * 
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