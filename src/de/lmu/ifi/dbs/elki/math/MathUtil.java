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
    double meanX = x.doubleValue(1);
    double meanY = y.doubleValue(1);
    for(int i = 2; i < x.getDimensionality(); i++) {
      double sweep = (i - 1.0) / i;
      double deltaX = x.doubleValue(i) - meanX;
      double deltaY = y.doubleValue(i) - meanY;
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

  /**
   * Compute the Factorial of n, often written as <code>c!</code> in
   * mathematics.
   * 
   * @param n Note: n &gt;= 0
   * @return n * (n-1) * (n-2) * ... * 1
   */
  public static double factorial(int n) {
    double nFac = 1.0;
    for(int i = n; i > 0; i--) {
      nFac *= i;
    }
    return nFac;
  }

  /**
   * <p>
   * Binomial coefficent, also known as "n choose k")
   * </p>
   * 
   * @param n Total number of samples. n &gt; 0
   * @param k Number of elements to choose. <code>n &gt;= k</code>,
   *        <code>k &gt;= 0</code>
   * @return n! / (k! * (n-k)!)
   */
  public static double binomialCoefficient(int n, int k) {
    long temp = 1;
    int m = Math.max(k, n - k);
    for(int i = n, j = 1; i > m; i--, j++) {
      temp = temp * i / j;
    }
    return temp;
  }

  /**
   * Probability density function of the normal distribution.
   * 
   * <pre>
   * 1/(SQRT(2*pi*sigma^2)) * e^(-(x-mu)^2/2sigma^2)
   * </pre>
   * 
   * @param x The value.
   * @param mu The mean.
   * @param sigma The standard deviation.
   * @return PDF of the given normal distribution at x.
   */
  public static double normalPDF(double x, double mu, double sigma) {
    return 1 / (Math.sqrt(2 * Math.PI * sigma * sigma)) * Math.exp(-1 * (x - mu) * (x - mu) / 2 / sigma / sigma);
  }

  /**
   * Cumulative probability density function (CDF) of a normal distribution.
   * 
   * @param x value to evaluate CDF at
   * @param mu Mean value
   * @param sigma Standard deviation.
   * @return The CDF of the normal given distribution at x.
   */
  public static double normalCDF(double x, double mu, double sigma) {
    return (1 + ErrorFunctions.erf(x / Math.sqrt(2))) / 2;
  }
}
