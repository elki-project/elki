package de.lmu.ifi.dbs.elki.math;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.math.BigInteger;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * A collection of math related utility functions.
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 */
public final class MathUtil {
  /**
   * Two times Pi.
   */
  public static final double TWOPI = 2 * Math.PI;

  /**
   * Square root of two times Pi.
   */
  public static final double SQRTTWOPI = Math.sqrt(TWOPI);

  /**
   * Square root of 2.
   */
  public static final double SQRT2 = Math.sqrt(2);

  /**
   * Square root of 5
   */
  public static final double SQRT5 = Math.sqrt(5);

  /**
   * Square root of 0.5 == 1 / sqrt(2)
   */
  public static final double SQRTHALF = Math.sqrt(.5);

  /**
   * Precomputed value of 1 / sqrt(pi)
   */
  public static final double ONE_BY_SQRTPI = 1 / Math.sqrt(Math.PI);

  /**
   * Logarithm of 2 to the basis e, for logarithm conversion.
   */
  public static final double LOG2 = Math.log(2);

  /**
   * Natural logarithm of 10
   */
  public static final double LOG10 = Math.log(10);

  /**
   * Math.log(Math.PI)
   */
  public static final double LOGPI = Math.log(Math.PI);

  /**
   * Math.log(Math.PI) / 2
   */
  public static final double LOGPIHALF = LOGPI / 2.;

  /**
   * Math.log(Math.sqrt(2*Math.PI))
   */
  public static final double LOGSQRTTWOPI = Math.log(SQRTTWOPI);

  /**
   * Constant for degrees to radians
   */
  public static final double DEG2RAD = Math.PI / 180.0;

  /**
   * Constant for radians to degrees
   */
  public static final double RAD2DEG = 180 / Math.PI;

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
   * Note: this code is <em>not</em> redundant to {@link Math#hypot}, since the
   * latter is significantly slower (but maybe has a higher precision).
   * 
   * @param a first cathetus
   * @param b second cathetus
   * @return {@code sqrt(a<sup>2</sup> + b<sup>2</sup>)}
   */
  public static double fastHypot(double a, double b) {
    if(a < 0) {
      a = -a;
    }
    if(b < 0) {
      b = -b;
    }
    if(a > b) {
      final double r = b / a;
      return a * Math.sqrt(1 + r * r);
    }
    else if(b != 0) {
      final double r = a / b;
      return b * Math.sqrt(1 + r * r);
    }
    else {
      return 0.0;
    }
  }

  /**
   * Computes the square root of the sum of the squared arguments without under
   * or overflow.
   * 
   * Note: this code is <em>not</em> redundant to {@link Math#hypot}, since the
   * latter is significantly slower (but has a higher precision).
   * 
   * @param a first cathetus
   * @param b second cathetus
   * @param c second cathetus
   * @return {@code sqrt(a<sup>2</sup> + b<sup>2</sup> + c<sup>2</sup>)}
   */
  public static double fastHypot3(double a, double b, double c) {
    if(a < 0) {
      a = -a;
    }
    if(b < 0) {
      b = -b;
    }
    if(c < 0) {
      c = -c;
    }
    double m = (a > b) ? ((a > c) ? a : c) : ((b > c) ? b : c);
    if(m <= 0) {
      return 0.0;
    }
    a = a / m;
    b = b / m;
    c = c / m;
    return m * Math.sqrt(a * a + b * b + c * c);
  }

  /**
   * Compute the Mahalanobis distance using the given weight matrix
   * 
   * @param weightMatrix Weight Matrix
   * @param o1_minus_o2 Delta vector
   * @return Mahalanobis distance
   */
  public static double mahalanobisDistance(Matrix weightMatrix, Vector o1_minus_o2) {
    double sqrDist = o1_minus_o2.transposeTimesTimes(weightMatrix, o1_minus_o2);

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
    PearsonCorrelation pc = new PearsonCorrelation();
    for(int i = 0; i < xdim; i++) {
      pc.put(x.doubleValue(i + 1), y.doubleValue(i + 1), 1.0);
    }
    return pc.getCorrelation();
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
  public static double weightedPearsonCorrelationCoefficient(NumberVector<?, ?> x, NumberVector<?, ?> y, double[] weights) {
    final int xdim = x.getDimensionality();
    final int ydim = y.getDimensionality();
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: feature vectors differ in dimensionality.");
    }
    if(xdim != weights.length) {
      throw new IllegalArgumentException("Dimensionality doesn't agree to weights.");
    }
    PearsonCorrelation pc = new PearsonCorrelation();
    for(int i = 0; i < xdim; i++) {
      pc.put(x.doubleValue(i + 1), y.doubleValue(i + 1), weights[i]);
    }
    return pc.getCorrelation();
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
  public static double weightedPearsonCorrelationCoefficient(NumberVector<?, ?> x, NumberVector<?, ?> y, NumberVector<?, ?> weights) {
    final int xdim = x.getDimensionality();
    final int ydim = y.getDimensionality();
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: feature vectors differ in dimensionality.");
    }
    if(xdim != weights.getDimensionality()) {
      throw new IllegalArgumentException("Dimensionality doesn't agree to weights.");
    }
    PearsonCorrelation pc = new PearsonCorrelation();
    for(int i = 0; i < xdim; i++) {
      pc.put(x.doubleValue(i + 1), y.doubleValue(i + 1), weights.doubleValue(i + 1));
    }
    return pc.getCorrelation();
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
  public static double pearsonCorrelationCoefficient(double[] x, double[] y) {
    final int xdim = x.length;
    final int ydim = y.length;
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: feature vectors differ in dimensionality.");
    }
    PearsonCorrelation pc = new PearsonCorrelation();
    for(int i = 0; i < xdim; i++) {
      pc.put(x[i], y[i], 1.0);
    }
    return pc.getCorrelation();
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
  public static double weightedPearsonCorrelationCoefficient(double[] x, double[] y, double[] weights) {
    final int xdim = x.length;
    final int ydim = y.length;
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: feature vectors differ in dimensionality.");
    }
    if(xdim != weights.length) {
      throw new IllegalArgumentException("Dimensionality doesn't agree to weights.");
    }
    PearsonCorrelation pc = new PearsonCorrelation();
    for(int i = 0; i < xdim; i++) {
      pc.put(x[i], y[i], weights[i]);
    }
    return pc.getCorrelation();
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
   * Compute the sum of the i first integers.
   * 
   * @param i maximum summand
   * @return Sum
   */
  public static long sumFirstIntegers(final long i) {
    return ((i - 1L) * i) / 2;
  }

  /**
   * Produce an array of random numbers in [0:1]
   * 
   * @param len Length
   * @return Array
   */
  public static double[] randomDoubleArray(int len) {
    return randomDoubleArray(len, new Random());
  }

  /**
   * Produce an array of random numbers in [0:1]
   * 
   * @param len Length
   * @param r Random generator
   * @return Array
   */
  public static double[] randomDoubleArray(int len, Random r) {
    final double[] ret = new double[len];
    for(int i = 0; i < len; i++) {
      ret[i] = r.nextDouble();
    }
    return ret;
  }

  /**
   * Convert Degree to Radians
   * 
   * @param deg Degree value
   * @return Radian value
   */
  public static double deg2rad(double deg) {
    return deg * DEG2RAD;
  }

  /**
   * Radians to Degree
   * 
   * @param rad Radians value
   * @return Degree value
   */
  public static double rad2deg(double rad) {
    return rad * RAD2DEG;
  }

  /**
   * Compute the approximate on-earth-surface distance of two points.
   * 
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance in km (approximately)
   */
  public static double latlngDistance(double lat1, double lon1, double lat2, double lon2) {
    final double EARTH_RADIUS = 6371; // km.
    // Work in radians
    lat1 = MathUtil.deg2rad(lat1);
    lat2 = MathUtil.deg2rad(lat2);
    lon1 = MathUtil.deg2rad(lon1);
    lon2 = MathUtil.deg2rad(lon2);
    // Delta
    final double dlat = lat1 - lat2;
    final double dlon = lon1 - lon2;

    // Spherical Law of Cosines
    // NOTE: there seems to be a signedness issue in this code!
    // double dist = Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) *
    // Math.cos(lat2) * Math.cos(dlon);
    // return EARTH_RADIUS * Math.atan(dist);

    // Alternative: Havestine formula, higher precision at < 1 meters:
    final double slat = Math.sin(dlat / 2);
    final double slon = Math.sin(dlon / 2);
    final double a = slat * slat + slon * slon * Math.cos(lat1) * Math.cos(lat2);
    final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return EARTH_RADIUS * c;
  }

  /**
   * Compute the angle between two vectors.
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @return Angle
   */
  public static double angle(Vector v1, Vector v2) {
    return angle(v1.getArrayRef(), v2.getArrayRef());
  }

  /**
   * Compute the angle between two vectors.
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @return Angle
   */
  public static double angle(double[] v1, double[] v2) {
    // Essentially, we want to compute this:
    // v1.transposeTimes(v2) / (v1.euclideanLength() * v2.euclideanLength());
    // We can just compute all three in parallel.
    double s = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < v1.length; k++) {
      final double r1 = v1[k];
      final double r2 = v2[k];
      s += r1 * r2;
      e1 += r1 * r1;
      e2 += r2 * r2;
    }
    return Math.sqrt((s / e1) * (s / e2));
  }

  /**
   * Compute the angle between two vectors.
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @param o Origin
   * @return Angle
   */
  public static double angle(Vector v1, Vector v2, Vector o) {
    return angle(v1.getArrayRef(), v2.getArrayRef(), o.getArrayRef());
  }

  /**
   * Compute the angle between two vectors.
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @param o Origin
   * @return Angle
   */
  public static double angle(double[] v1, double[] v2, double[] o) {
    // Essentially, we want to compute this:
    // v1' = v1 - o, v2' = v2 - o
    // v1'.transposeTimes(v2') / (v1'.euclideanLength()*v2'.euclideanLength());
    // We can just compute all three in parallel.
    double s = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < v1.length; k++) {
      final double r1 = v1[k] - o[k];
      final double r2 = v2[k] - o[k];
      s += r1 * r2;
      e1 += r1 * r1;
      e2 += r2 * r2;
    }
    return Math.sqrt((s / e1) * (s / e2));
  }

  /**
   * Find the next power of 2.
   * 
   * Classic bit operation, for signed 32-bit. Valid for positive integers only
   * (0 otherwise).
   * 
   * @param x original integer
   * @return Next power of 2
   */
  public static int nextPow2Int(int x) {
    --x;
    x |= x >>> 1;
    x |= x >>> 2;
    x |= x >>> 4;
    x |= x >>> 8;
    x |= x >>> 16;
    return ++x;
  }

  /**
   * Find the next power of 2.
   * 
   * Classic bit operation, for signed 64-bit. Valid for positive integers only
   * (0 otherwise).
   * 
   * @param x original long integer
   * @return Next power of 2
   */
  public static long nextPow2Long(long x) {
    --x;
    x |= x >>> 1;
    x |= x >>> 2;
    x |= x >>> 4;
    x |= x >>> 16;
    x |= x >>> 32;
    return ++x;
  }

  /**
   * Find the next larger number with all ones.
   * 
   * Classic bit operation, for signed 32-bit. Valid for positive integers only
   * (-1 otherwise).
   * 
   * @param x original integer
   * @return Next number with all bits set
   */
  public static int nextAllOnesInt(int x) {
    x |= x >>> 1;
    x |= x >>> 2;
    x |= x >>> 4;
    x |= x >>> 8;
    x |= x >>> 16;
    return x;
  }

  /**
   * Find the next larger number with all ones.
   * 
   * Classic bit operation, for signed 64-bit. Valid for positive integers only
   * (-1 otherwise).
   * 
   * @param x original long integer
   * @return Next number with all bits set
   */
  public static long nextAllOnesLong(long x) {
    x |= x >>> 1;
    x |= x >>> 2;
    x |= x >>> 4;
    x |= x >>> 16;
    x |= x >>> 32;
    return x;
  }

  /**
   * Return the largest double that rounds down to this float.
   * 
   * Note: Probably not always correct - subnormal values are quite tricky. So
   * some of the bounds might not be tight.
   * 
   * @param f Float value
   * @return Double value
   */
  public static double floatToDoubleUpper(float f) {
    if(Float.isNaN(f)) {
      return Double.NaN;
    }
    if(Float.isInfinite(f)) {
      if(f > 0) {
        return Double.POSITIVE_INFINITY;
      }
      else {
        return Double.longBitsToDouble(0xc7efffffffffffffl);
      }
    }
    long bits = Double.doubleToRawLongBits((double) f);
    if((bits & 0x8000000000000000l) == 0) { // Positive
      if(bits == 0l) {
        return Double.longBitsToDouble(0x3690000000000000l);
      }
      if(f == Float.MIN_VALUE) {
        // bits += 0x7_ffff_ffff_ffffl;
        return Double.longBitsToDouble(0x36a7ffffffffffffl);
      }
      if(Float.MIN_NORMAL > f && f >= Double.MIN_NORMAL) {
        // The most tricky case:
        // a denormalized float, but a normalized double
        final long bits2 = Double.doubleToRawLongBits((double) Math.nextUp(f));
        bits = (bits >>> 1) + (bits2 >>> 1) - 1l;
      }
      else {
        bits += 0xfffffffl; // 28 extra bits
      }
      return Double.longBitsToDouble(bits);
    }
    else {
      if(bits == 0x8000000000000000l) {
        return -0.0d;
      }
      if(f == -Float.MIN_VALUE) {
        // bits -= 0xf_ffff_ffff_ffffl;
        return Double.longBitsToDouble(0xb690000000000001l);
      }
      if(-Float.MIN_NORMAL < f && f <= -Double.MIN_NORMAL) {
        // The most tricky case:
        // a denormalized float, but a normalized double
        final long bits2 = Double.doubleToRawLongBits((double) Math.nextUp(f));
        bits = (bits >>> 1) + (bits2 >>> 1) + 1l;
      }
      else {
        bits -= 0xfffffffl; // 28 extra bits
      }
      return Double.longBitsToDouble(bits);
    }
  }

  /**
   * Return the largest double that rounds up to this float.
   * 
   * Note: Probably not always correct - subnormal values are quite tricky. So
   * some of the bounds might not be tight.
   * 
   * @param f Float value
   * @return Double value
   */
  public static double floatToDoubleLower(float f) {
    if(Float.isNaN(f)) {
      return Double.NaN;
    }
    if(Float.isInfinite(f)) {
      if(f < 0) {
        return Double.NEGATIVE_INFINITY;
      }
      else {
        return Double.longBitsToDouble(0x47efffffffffffffl);
      }
    }
    long bits = Double.doubleToRawLongBits((double) f);
    if((bits & 0x8000000000000000l) == 0) { // Positive
      if(bits == 0l) {
        return +0.0d;
      }
      if(f == Float.MIN_VALUE) {
        // bits -= 0xf_ffff_ffff_ffffl;
        return Double.longBitsToDouble(0x3690000000000001l);
      }
      if(Float.MIN_NORMAL > f /* && f >= Double.MIN_NORMAL */) {
        // The most tricky case:
        // a denormalized float, but a normalized double
        final long bits2 = Double.doubleToRawLongBits((double) -Math.nextUp(-f));
        bits = (bits >>> 1) + (bits2 >>> 1) + 1l; // + (0xfff_ffffl << 18);
      }
      else {
        bits -= 0xfffffffl; // 28 extra bits
      }
      return Double.longBitsToDouble(bits);
    }
    else {
      if(bits == 0x8000000000000000l) {
        return Double.longBitsToDouble(0xb690000000000000l);
      }
      if(f == -Float.MIN_VALUE) {
        // bits += 0x7_ffff_ffff_ffffl;
        return Double.longBitsToDouble(0xb6a7ffffffffffffl);
      }
      if(-Float.MIN_NORMAL < f /* && f <= -Double.MIN_NORMAL */) {
        // The most tricky case:
        // a denormalized float, but a normalized double
        final long bits2 = Double.doubleToRawLongBits((double) -Math.nextUp(-f));
        bits = (bits >>> 1) + (bits2 >>> 1) - 1l;
      }
      else {
        bits += 0xfffffffl; // 28 extra bits
      }
      return Double.longBitsToDouble(bits);
    }
  }
}