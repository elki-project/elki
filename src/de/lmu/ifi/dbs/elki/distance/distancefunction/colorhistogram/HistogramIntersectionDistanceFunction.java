package de.lmu.ifi.dbs.elki.distance.distancefunction.colorhistogram;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDoubleDistanceFunction;

/**
 * Intersection distance for color histograms.
 * 
 * According to:
 * MJ Swain, DH Ballard - International journal of computer vision, 1991 - Springer
 * 
 * @author Erich Schubert
 *
 * @param <V> Vector type
 */
public class HistogramIntersectionDistanceFunction<V extends NumberVector<V,?>> extends AbstractDoubleDistanceFunction<V> {
  /**
   * Constructor. No parameters.
   */
  public HistogramIntersectionDistanceFunction() {
    super();
  }

  @Override
  public DoubleDistance distance(V v1, V v2) {
    if(v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString() + "\n" + v1.getDimensionality() + "!=" + v2.getDimensionality());
    }
    double dist = 0;
    double norm1 = 0;
    double norm2 = 0;
    for(int i = 1; i <= v1.getDimensionality(); i++) {
      final double val1 = v1.doubleValue(i);
      final double val2 = v2.doubleValue(i);
      dist += Math.min(val1,val2);
      norm1 += val1;
      norm2 += val2;
    }
    dist = 1 - dist / Math.min(norm1, norm2);
    return new DoubleDistance(dist);
  }

  @Override
  public String shortDescription() {
    return "Color histogram intersection distance, as per MJ Swain, DH Ballard - International journal of computer vision.";
  }
}
