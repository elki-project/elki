package de.lmu.ifi.dbs.elki.math;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import net.jafama.FastMath;

/**
 * Class to incrementally compute pearson correlation.
 * 
 * In fact, this actually computes Var(X), Var(Y) and Cov(X, Y), all of which
 * can be obtained from this class. If you need more than two variables, use
 * {@link de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix} which uses
 * slightly more memory (by using arrays) but essentially does the same.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class PearsonCorrelation {
  /**
   * Aggregation for squared residuals - we are not using sum-of-squares!
   */
  private double sumXX = 0., sumYY = 0., sumXY = 0.;

  /**
   * Current mean for X and Y.
   */
  private double meanX = 0., meanY = 0.;

  /**
   * Weight sum.
   */
  private double sumWe = 0.;

  /**
   * Constructor.
   */
  public PearsonCorrelation() {
    super();
  }

  /**
   * Put a single value into the correlation statistic.
   * 
   * @param x Value in X
   * @param y Value in Y
   * @param w Weight
   */
  public void put(double x, double y, double w) {
    if(sumWe <= 0.) {
      meanX = x;
      meanY = y;
      sumWe = w;
      return;
    }
    // Incremental update
    sumWe += w;
    // Delta to previous mean
    final double deltaX = x - meanX;
    final double deltaY = y - meanY;
    // Update means
    meanX += deltaX * w / sumWe;
    meanY += deltaY * w / sumWe;
    // Delta to new mean
    final double neltaX = x - meanX;
    final double neltaY = y - meanY;
    // Update
    sumXX += w * deltaX * neltaX;
    sumYY += w * deltaY * neltaY;
    // should equal weight * deltaY * neltaX!
    sumXY += w * deltaX * neltaY;
  }

  /**
   * Put a single value into the correlation statistic.
   * 
   * @param x Value in X
   * @param y Value in Y
   */
  public void put(double x, double y) {
    put(x, y, 1.);
  }

  /**
   * Get the Pearson correlation value.
   * 
   * @return Correlation value
   */
  public double getCorrelation() {
    if(!(sumXX > 0. && sumYY > 0.)) {
      return (sumXX == sumYY) ? 1. : 0.;
    }
    return sumXY / FastMath.sqrt(sumXX * sumYY);
  }

  /**
   * Get the number of points the average is based on.
   * 
   * @return number of data points
   */
  public double getCount() {
    return sumWe;
  }

  /**
   * Return mean of X
   * 
   * @return mean
   */
  public double getMeanX() {
    return meanX;
  }

  /**
   * Return mean of Y
   * 
   * @return mean
   */
  public double getMeanY() {
    return meanY;
  }

  /**
   * Get the covariance of X and Y (not taking sampling into account)
   * 
   * @return Covariance
   */
  public double getNaiveCovariance() {
    return sumXY / sumWe;
  }

  /**
   * Get the covariance of X and Y (with sampling correction)
   * 
   * @return Covariance
   */
  public double getSampleCovariance() {
    assert (sumWe > 1.);
    return sumXY / (sumWe - 1.);
  }

  /**
   * Return the naive variance (not taking sampling into account)
   * 
   * Note: usually, you should be using {@link #getSampleVarianceX} instead!
   * 
   * @return variance
   */
  public double getNaiveVarianceX() {
    return sumXX / sumWe;
  }

  /**
   * Return sample variance.
   * 
   * @return sample variance
   */
  public double getSampleVarianceX() {
    assert (sumWe > 1.);
    return sumXX / (sumWe - 1.);
  }

  /**
   * Return standard deviation using the non-sample variance
   * 
   * Note: usually, you should be using {@link #getSampleStddevX} instead!
   * 
   * @return standard deviation
   */
  public double getNaiveStddevX() {
    return FastMath.sqrt(getNaiveVarianceX());
  }

  /**
   * Return standard deviation
   * 
   * @return standard deviation
   */
  public double getSampleStddevX() {
    return FastMath.sqrt(getSampleVarianceX());
  }

  /**
   * Return the naive variance (not taking sampling into account)
   * 
   * Note: usually, you should be using {@link #getSampleVarianceY} instead!
   * 
   * @return variance
   */
  public double getNaiveVarianceY() {
    return sumYY / sumWe;
  }

  /**
   * Return sample variance.
   * 
   * @return sample variance
   */
  public double getSampleVarianceY() {
    assert (sumWe > 1.);
    return sumYY / (sumWe - 1.);
  }

  /**
   * Return standard deviation using the non-sample variance
   * 
   * Note: usually, you should be using {@link #getSampleStddevY} instead!
   * 
   * @return stddev
   */
  public double getNaiveStddevY() {
    return FastMath.sqrt(getNaiveVarianceY());
  }

  /**
   * Return standard deviation
   * 
   * @return stddev
   */
  public double getSampleStddevY() {
    return FastMath.sqrt(getSampleVarianceY());
  }

  /**
   * Reset the value.
   */
  public void reset() {
    sumXX = 0.;
    sumXY = 0.;
    sumYY = 0.;
    meanX = 0.;
    meanY = 0.;
    sumWe = 0.;
  }

  /**
   * Compute the Pearson product-moment correlation coefficient for two
   * FeatureVectors.
   *
   * @param x first FeatureVector
   * @param y second FeatureVector
   * @return the Pearson product-moment correlation coefficient for x and y
   */
  public static double coefficient(double[] x, double[] y) {
    final int xdim = x.length;
    final int ydim = y.length;
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: arrays differ in length.");
    }
    // Inlined computation of Pearson correlation, to avoid allocating objects!
    // This is a numerically stabilized version, avoiding sum-of-squares.
    double sumXX = 0., sumYY = 0., sumXY = 0.;
    double meanX = x[0], meanY = y[0];
    int i = 1;
    while(i < xdim) {
      final double xv = x[i], yv = y[i];
      // Delta to previous mean
      final double deltaX = xv - meanX;
      final double deltaY = yv - meanY;
      // Increment count first
      ++i;
      // Update means
      meanX += deltaX / i;
      meanY += deltaY / i;
      // Delta to new mean
      final double neltaX = xv - meanX;
      final double neltaY = yv - meanY;
      // Update
      sumXX += deltaX * neltaX;
      sumYY += deltaY * neltaY;
      // should equal deltaY * neltaX!
      sumXY += deltaX * neltaY;
    }
    // One or both series were constant:
    if(!(sumXX > 0. && sumYY > 0.)) {
      return (sumXX == sumYY) ? 1. : 0.;
    }
    return sumXY / FastMath.sqrt(sumXX * sumYY);
  }

  /**
   * Compute the Pearson product-moment correlation coefficient for two
   * NumberVectors.
   *
   * @param x first NumberVector
   * @param y second NumberVector
   * @return the Pearson product-moment correlation coefficient for x and y
   */
  public static double coefficient(NumberVector x, NumberVector y) {
    final int xdim = x.getDimensionality();
    final int ydim = y.getDimensionality();
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: number vectors differ in dimensionality.");
    }
    // Inlined computation of Pearson correlation, to avoid allocating objects!
    // This is a numerically stabilized version, avoiding sum-of-squares.
    double sumXX = 0., sumYY = 0., sumXY = 0.;
    double meanX = x.doubleValue(0), meanY = y.doubleValue(0);
    int i = 1;
    while(i < xdim) {
      final double xv = x.doubleValue(i), yv = y.doubleValue(i);
      // Delta to previous mean
      final double deltaX = xv - meanX;
      final double deltaY = yv - meanY;
      // Increment count first
      ++i;
      // Update means
      meanX += deltaX / i;
      meanY += deltaY / i;
      // Delta to new mean
      final double neltaX = xv - meanX;
      final double neltaY = yv - meanY;
      // Update
      sumXX += deltaX * neltaX;
      sumYY += deltaY * neltaY;
      // should equal deltaY * neltaX!
      sumXY += deltaX * neltaY;
    }
    // One or both series were constant:
    if(!(sumXX > 0. && sumYY > 0.)) {
      return (sumXX == sumYY) ? 1. : 0.;
    }
    return sumXY / FastMath.sqrt(sumXX * sumYY);
  }

  /**
   * Compute the Pearson product-moment correlation coefficient for two
   * FeatureVectors.
   *
   * @param x first FeatureVector
   * @param y second FeatureVector
   * @param weights Weights
   * @return the Pearson product-moment correlation coefficient for x and y
   */
  public static double weightedCoefficient(double[] x, double[] y, double[] weights) {
    final int xdim = x.length;
    final int ydim = y.length;
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: arrays differ in length.");
    }
    if(xdim != weights.length) {
      throw new IllegalArgumentException("Dimensionality doesn't agree to weights.");
    }
    // Inlined computation of Pearson correlation, to avoid allocating objects!
    // This is a numerically stabilized version, avoiding sum-of-squares.
    double sumXX = 0., sumYY = 0., sumXY = 0., sumWe = weights[0];
    double meanX = x[0], meanY = y[0];
    for(int i = 1; i < xdim; ++i) {
      final double xv = x[i], yv = y[i], w = weights[i];
      // Delta to previous mean
      final double deltaX = xv - meanX;
      final double deltaY = yv - meanY;
      // Increment weight first
      sumWe += w;
      // Update means
      meanX += deltaX * w / sumWe;
      meanY += deltaY * w / sumWe;
      // Delta to new mean
      final double neltaX = xv - meanX;
      final double neltaY = yv - meanY;
      // Update
      sumXX += w * deltaX * neltaX;
      sumYY += w * deltaY * neltaY;
      // should equal weight * deltaY * neltaX!
      sumXY += w * deltaX * neltaY;
    }
    // One or both series were constant:
    if(!(sumXX > 0. && sumYY > 0.)) {
      return (sumXX == sumYY) ? 1. : 0.;
    }
    return sumXY / FastMath.sqrt(sumXX * sumYY);
  }

  /**
   * Compute the Pearson product-moment correlation coefficient for two
   * NumberVectors.
   *
   * @param x first NumberVector
   * @param y second NumberVector
   * @param weights Weights
   * @return the Pearson product-moment correlation coefficient for x and y
   */
  public static double weightedCoefficient(NumberVector x, NumberVector y, double[] weights) {
    final int xdim = x.getDimensionality();
    final int ydim = y.getDimensionality();
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: number vectors differ in dimensionality.");
    }
    if(xdim != weights.length) {
      throw new IllegalArgumentException("Dimensionality doesn't agree to weights.");
    }
    // Inlined computation of Pearson correlation, to avoid allocating objects!
    // This is a numerically stabilized version, avoiding sum-of-squares.
    double sumXX = 0., sumYY = 0., sumXY = 0., sumWe = weights[0];
    double meanX = x.doubleValue(0), meanY = y.doubleValue(0);
    for(int i = 1; i < xdim; ++i) {
      final double xv = x.doubleValue(i), yv = y.doubleValue(i), w = weights[i];
      // Delta to previous mean
      final double deltaX = xv - meanX;
      final double deltaY = yv - meanY;
      // Increment weight first
      sumWe += w;
      // Update means
      meanX += deltaX * w / sumWe;
      meanY += deltaY * w / sumWe;
      // Delta to new mean
      final double neltaX = xv - meanX;
      final double neltaY = yv - meanY;
      // Update
      sumXX += w * deltaX * neltaX;
      sumYY += w * deltaY * neltaY;
      // should equal weight * deltaY * neltaX!
      sumXY += w * deltaX * neltaY;
    }
    // One or both series were constant:
    if(!(sumXX > 0. && sumYY > 0.)) {
      return (sumXX == sumYY) ? 1. : 0.;
    }
    return sumXY / FastMath.sqrt(sumXX * sumYY);
  }

  /**
   * Compute the Pearson product-moment correlation coefficient for two
   * FeatureVectors.
   *
   * @param x first FeatureVector
   * @param y second FeatureVector
   * @param weights Weights
   * @return the Pearson product-moment correlation coefficient for x and y
   */
  public static double weightedPearsonCorrelationCoefficient(NumberVector x, NumberVector y, NumberVector weights) {
    final int xdim = x.getDimensionality();
    final int ydim = y.getDimensionality();
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: feature vectors differ in dimensionality.");
    }
    if(xdim != weights.getDimensionality()) {
      throw new IllegalArgumentException("Dimensionality doesn't agree to weights.");
    }
    // Inlined computation of Pearson correlation, to avoid allocating objects!
    // This is a numerically stabilized version, avoiding sum-of-squares.
    double sumXX = 0., sumYY = 0., sumXY = 0., sumWe = weights.doubleValue(0);
    double meanX = x.doubleValue(0), meanY = y.doubleValue(0);
    for(int i = 1; i < xdim; ++i) {
      final double xv = x.doubleValue(i), yv = y.doubleValue(i),
          w = weights.doubleValue(i);
      // Delta to previous mean
      final double deltaX = xv - meanX;
      final double deltaY = yv - meanY;
      // Increment weight first
      sumWe += w;
      // Update means
      meanX += deltaX * w / sumWe;
      meanY += deltaY * w / sumWe;
      // Delta to new mean
      final double neltaX = xv - meanX;
      final double neltaY = yv - meanY;
      // Update
      sumXX += w * deltaX * neltaX;
      sumYY += w * deltaY * neltaY;
      // should equal weight * deltaY * neltaX!
      sumXY += w * deltaX * neltaY;
    }
    // One or both series were constant:
    if(!(sumXX > 0. && sumYY > 0.)) {
      return (sumXX == sumYY) ? 1. : 0.;
    }
    return sumXY / FastMath.sqrt(sumXX * sumYY);
  }
}