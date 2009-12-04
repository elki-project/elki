package de.lmu.ifi.dbs.elki.math;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * A collection of math related utility functions.
 */
public class MathUtil {
  /**
   * Computes the square root of the sum of the squared arguments without under
   * or overflow.
   * 
   * @param a first cathetus
   * @param b second cathetus
   * @return {@code sqrt(a<sup>2</sup> + b<sup>2</sup>)}
   */
  public static double hypotenuse(double a, double b) {
    double r;
    if(Math.abs(a) > Math.abs(b)) {
      r = b / a;
      r = Math.abs(a) * Math.sqrt(1 + r * r);
    }
    else if(b != 0) {
      r = a / b;
      r = Math.abs(b) * Math.sqrt(1 + r * r);
    }
    else {
      r = 0.0;
    }
    return r;
  }

  /**
   * Compute the Mahalanobis distance using the given weight matrix
   * 
   * @param weightMatrix Weight Matrix
   * @param o1_minus_o2 Delta vector
   * @return Mahalanobis distance
   */
  public static double mahalanobisDistance(Matrix weightMatrix, Vector o1_minus_o2) {
    double sqrDist = o1_minus_o2.transposeTimes(weightMatrix).times(o1_minus_o2).get(0, 0);

    if(sqrDist < 0 && Math.abs(sqrDist) < 0.000000001) {
      sqrDist = Math.abs(sqrDist);
    }
    double dist = Math.sqrt(sqrDist);
    return dist;
  }

  /**
   * <p>
   * Provides the Pearson product-moment correlation coefficient for two
   * FeatureVectors.
   * </p>
   * 
   * @param <V> type of the FeatureVectors
   * @param <N> type of the numerical attributes of the FeatureVectors of type V
   * @param x first FeatureVector
   * @param y second FeatureVector
   * @return the Pearson product-moment correlation coefficient for x and y
   */
  public static <V extends NumberVector<V, N>, N extends Number> double pearsonCorrelationCoefficient(NumberVector<V, ?> x, NumberVector<V, ?> y) {
    if(x.getDimensionality() != y.getDimensionality()) {
      throw new IllegalArgumentException("Invalid arguments: feature vectors differ in dimensionality.");
    }
    if(x.getDimensionality() <= 0) {
      throw new IllegalArgumentException("Invalid arguments: dimensionality not positive.");
    }
    double sumSqX = 0;
    double sumSqY = 0;
    double sumCoproduct = 0;
    double meanX = x.getValue(1).doubleValue();
    double meanY = y.getValue(1).doubleValue();
    for(int i = 2; i < x.getDimensionality(); i++) {
      double sweep = (i - 1.0) / i;
      double deltaX = x.getValue(i).doubleValue() - meanX;
      double deltaY = y.getValue(i).doubleValue() - meanY;
      sumSqX += deltaX * deltaX * sweep;
      sumSqY += deltaY * deltaY * sweep;
      sumCoproduct += deltaX * deltaY * sweep;
      meanX += deltaX / i;
      meanY += deltaY / i;
    }
    double popSdX = Math.sqrt(sumSqX / x.getDimensionality());
    double popSdY = Math.sqrt(sumSqY / y.getDimensionality());
    double covXY = sumCoproduct / x.getDimensionality();
    double correlation = covXY / (popSdX * popSdY);
    return correlation;
  }
}