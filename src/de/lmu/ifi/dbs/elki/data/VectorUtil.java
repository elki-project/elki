package de.lmu.ifi.dbs.elki.data;

import de.lmu.ifi.dbs.elki.math.DoubleMinMax;

public final class VectorUtil {

  /**
   * Return the range across all dimensions. Sensible for time series.
   * 
   * @return [min, max]
   */
  public static <V extends FeatureVector<?, ?>> DoubleMinMax getRangeDouble(V vec) {
    DoubleMinMax minmax = new DoubleMinMax();

    for(int i = 0; i < vec.getDimensionality(); i++) {
      minmax.put(vec.getValue(i+1).doubleValue());
    }

    return minmax;
  }
}
