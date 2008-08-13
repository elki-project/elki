package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

public interface WeightFunction<V> {
  /**
   * WeightFunction interface that allows the use of various distance-based
   * weight functions. In addition to the distance parameter, the maximum
   * distance and standard deviation are also given, to allow distance functions
   * to be normalized according to the maximum or standard deviation.
   * 
   * @param distance distance of the query point
   * @param max maximum distance of all included points
   * @param stddev standard deviation (i.e. quadratic mean / RMS) of the
   *        included points
   * @return weight for the query point
   */
  double getWeight(double distance, double max, double stddev);
}
