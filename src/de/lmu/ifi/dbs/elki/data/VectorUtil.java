package de.lmu.ifi.dbs.elki.data;

import java.util.Random;

import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;

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

  /**
   * Produce a new vector based on random numbers in [0:1] of the same type and
   * dimensionality as the given vector.
   * 
   * @param template existing instance of wanted dimensionality.
   * @param r Random generator
   * @return new instance
   */
  public static <V extends NumberVector<V, ?>> V randomVector(V template, Random r) {
    return template.newInstance(MathUtil.randomDoubleArray(template.getDimensionality(), r));
  }

  /**
   * Produce a new vector based on random numbers in [0:1] of the same type and
   * dimensionality as the given vector.
   * 
   * @param template existing instance of wanted dimensionality.
   * @return new instance
   */
  public static <V extends NumberVector<V, ?>> V randomVector(V template) {
    return randomVector(template, new Random());
  }
}