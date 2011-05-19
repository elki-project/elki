package de.lmu.ifi.dbs.elki.math;

import java.math.BigInteger;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * A collection of math related utility functions.
 */
public final class MathUtil {
  /**
   * Two times Pi.
   */
  public static final double TWOPI = 2 * Math.PI;

  /**
   * Squre root of two times Pi.
   */
  public static final double SQRTTWOPI = Math.sqrt(TWOPI);

  /**
   * Square root of 2.
   */
  public static final double SQRT2 = Math.sqrt(2);

  /**
   * Fake constructor for static class.
   */
  private MathUtil() {
    // Static methods only - do not instantiate!
  }

  /**
   * Computes the square root of the sum of the squared arguments without under
   * or overflow.
   * 
   * @param a first cathetus
   * @param b second cathetus
   * @return {@code sqrt(a<sup>2</sup> + b<sup>2</sup>)}
   */
  public static double hypotenuse(double a, double b) {
    if(Math.abs(a) > Math.abs(b)) {
      final double r = b / a;
      return Math.abs(a) * Math.sqrt(1 + r * r);
    }
    else if(b != 0) {
      final double r = a / b;
      return Math.abs(b) * Math.sqrt(1 + r * r);
    }
    else {
      return 0.0;
    }
  }

  /**
   * Compute the Mahalanobis distance using the given weight matrix
   * 
   * @param weightMatrix Weight Matrix
   * @param o1_minus_o2 Delta vector
   * @return Mahalanobis distance
   */
  public static double mahalanobisDistance(Matrix weightMatrix, Vector o1_minus_o2) {
    double sqrDist = o1_minus_o2.transposeTimes(weightMatrix).times(o1_minus_o2).get(0);

    if(sqrDist < 0 && Math.abs(sqrDist) < 0.000000001) {
      sqrDist = Math.abs(sqrDist);
    }
    return Math.sqrt(sqrDist);
  }

  /**
   * <p>
   * Provides the Pearson product-moment correlation coefficient for two
   * FeatureVectors.
   * </p>
   * 
   * @param x first FeatureVector
   * @param y second FeatureVector
   * @return the Pearson product-moment correlation coefficient for x and y
   */
  public static double pearsonCorrelationCoefficient(NumberVector<?, ?> x, NumberVector<?, ?> y) {
    final int xdim = x.getDimensionality();
    final int ydim = y.getDimensionality();
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: feature vectors differ in dimensionality.");
    }
    if(xdim <= 0) {
      throw new IllegalArgumentException("Invalid arguments: dimensionality not positive.");
    }
    double sumSqX = 0;
    double sumSqY = 0;
    double sumCoproduct = 0;
    {
      // Incremental computation
      double meanX = x.doubleValue(1);
      double meanY = y.doubleValue(1);
      for(int i = 2; i < xdim; i++) {
        final double sweep = (i - 1.0) / i;
        final double deltaX = x.doubleValue(i) - meanX;
        final double deltaY = y.doubleValue(i) - meanY;
        sumSqX += deltaX * deltaX * sweep;
        sumSqY += deltaY * deltaY * sweep;
        sumCoproduct += deltaX * deltaY * sweep;
        meanX += deltaX / i;
        meanY += deltaY / i;
      }
    }
    final double popSdX = Math.sqrt(sumSqX / xdim);
    final double popSdY = Math.sqrt(sumSqY / ydim);
    final double covXY = sumCoproduct / xdim;
    if (popSdX == 0 || popSdY == 0) {
      return 0;
    }
    return covXY / (popSdX * popSdY);
  }

  /**
   * <p>
   * Provides the Pearson product-moment correlation coefficient for two
   * FeatureVectors.
   * </p>
   * 
   * @param x first FeatureVector
   * @param y second FeatureVector
   * @return the Pearson product-moment correlation coefficient for x and y
   */
  // TODO: mathematically correct?
  public static double weightedPearsonCorrelationCoefficient(NumberVector<?, ?> x, NumberVector<?, ?> y, double[] weights) {
    final int xdim = x.getDimensionality();
    final int ydim = y.getDimensionality();
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: feature vectors differ in dimensionality.");
    }
    if(xdim != weights.length) {
      throw new IllegalArgumentException("Dimensionality doesn't agree to weights.");
    }
    // Compute means
    double sumW = 0.0;
    double sumX = 0.0;
    double sumY = 0.0;
    double sumSqW = 0.0;
    {
      for(int i = 1; i < xdim; i++) {
        final double weight = weights[i - 1];
        final double valX = x.doubleValue(i);
        final double valY = y.doubleValue(i);

        sumW += weight;
        sumSqW += weight * weight;
        sumX += valX * weight;
        sumY += valY * weight;
      }
    }
    final double meanX = sumX / sumW;
    final double meanY = sumY / sumW;
    // Compute stddevs
    // TODO: can we do this single-pass but mathematically sound? Steiner Translation?
    double sumDX = 0.0;
    double sumDY = 0.0;
    double sumCo = 0.0;
    {
      for(int i = 1; i < xdim; i++) {
        final double weight = weights[i - 1];
        final double deltaX = Math.abs(x.doubleValue(i) - meanX);
        final double deltaY = Math.abs(y.doubleValue(i) - meanY);

        sumDX += deltaX * weight;
        sumDY += deltaY * weight;
        sumCo += deltaX * deltaY * weight;
      }
    }
    final double varX = (sumDX / sumW) / (1 - sumSqW); 
    final double varY = (sumDY / sumW) / (1 - sumSqW); 
    final double covXY = (sumCo / sumW);
    if (varX <= 0 || varY <= 0) {
      return 0;
    }
    return covXY / (Math.sqrt(varX) * Math.sqrt(varY));
  }

  /**
   * Compute the Factorial of n, often written as <code>c!</code> in
   * mathematics.</p>
   * <p>
   * Use this method if for large values of <code>n</code>.
   * </p>
   * 
   * @param n Note: n &gt;= 0. This {@link BigInteger} <code>n</code> will be 0
   *        after this method finishes.
   * @return n * (n-1) * (n-2) * ... * 1
   */
  public static BigInteger factorial(BigInteger n) {
    BigInteger nFac = BigInteger.valueOf(1);
    while(n.compareTo(BigInteger.valueOf(1)) > 0) {
      nFac = nFac.multiply(n);
      n = n.subtract(BigInteger.valueOf(1));
    }
    return nFac;
  }

  /**
   * Compute the Factorial of n, often written as <code>c!</code> in
   * mathematics.
   * 
   * @param n Note: n &gt;= 0
   * @return n * (n-1) * (n-2) * ... * 1
   */
  public static long factorial(int n) {
    long nFac = 1;
    for(long i = n; i > 0; i--) {
      nFac *= i;
    }
    return nFac;
  }

  /**
   * <p>
   * Binomial coefficient, also known as "n choose k".
   * </p>
   * 
   * @param n Total number of samples. n &gt; 0
   * @param k Number of elements to choose. <code>n &gt;= k</code>,
   *        <code>k &gt;= 0</code>
   * @return n! / (k! * (n-k)!)
   */
  public static long binomialCoefficient(long n, long k) {
    final long m = Math.max(k, n - k);
    double temp = 1;
    for(long i = n, j = 1; i > m; i--, j++) {
      temp = temp * i / j;
    }
    return (long) temp;
  }

  /**
   * Compute the Factorial of n, often written as <code>c!</code> in
   * mathematics.
   * 
   * @param n Note: n &gt;= 0
   * @return n * (n-1) * (n-2) * ... * 1
   */
  public static double approximateFactorial(int n) {
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
  public static double approximateBinomialCoefficient(int n, int k) {
    final int m = Math.max(k, n - k);
    long temp = 1;
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
    final double x_mu = x - mu;
    final double sigmasq = sigma * sigma;
    return 1 / (Math.sqrt(TWOPI * sigmasq)) * Math.exp(-1 * x_mu * x_mu / 2 / sigmasq);
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

  /**
   * Compute the sum of the i first integers.
   * 
   * @param i maximum summand
   * @return Sum
   */
  public static long sumFirstIntegers(final long i) {
    return ((i - 1L) * i) / 2;
  }
}