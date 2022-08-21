/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.math;

import elki.data.NumberVector;

/**
 * Class to compute the Pearson correlation coefficient (PCC) also known as
 * Pearson product-moment correlation coefficient (PPMCC).
 * <p>
 * This computes Var(X), Var(Y) and Cov(X, Y), all of which can be obtained from
 * this class. If you need more than two variables, use
 * {@link elki.math.linearalgebra.CovarianceMatrix} which uses
 * slightly more memory (by using arrays) but essentially does the same.
 * <p>
 * This method used a numerically more stable approach than the popular
 * \( E[XY]-E[X]E[Y] \) based version.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class PearsonCorrelation {
  /**
   * Aggregation for squared residuals - we are not using sum-of-squares!
   */
  private double sumXX, sumYY, sumXY;

  /**
   * Current mean for X and Y.
   */
  private double sumX, sumY;

  /**
   * Weight sum.
   */
  private double sumWe;

  /**
   * Constructor.
   */
  public PearsonCorrelation() {
    sumXX = sumYY = sumXY = sumX = sumY = sumWe = 0.;
  }

  /**
   * Put a single value into the correlation statistic.
   * 
   * @param x Value in X
   * @param y Value in Y
   * @param w Weight
   */
  public void put(double x, double y, double w) {
    if(w == 0.) {
      return;
    }
    if(sumWe <= 0.) {
      sumX = x * w;
      sumY = y * w;
      sumWe = w;
      return;
    }
    // Delta to previous mean
    final double deltaX = x * sumWe - sumX, deltaY = y * sumWe - sumY;
    final double oldWe = sumWe;
    // Incremental update
    sumWe += w;
    final double f = w / (sumWe * oldWe);
    // Update
    sumXX += f * deltaX * deltaX;
    sumYY += f * deltaY * deltaY;
    // should equal weight * deltaY * neltaX!
    sumXY += f * deltaX * deltaY;
    // Update means
    sumX += x * w;
    sumY += y * w;
  }

  /**
   * Put a single value into the correlation statistic.
   * 
   * @param x Value in X
   * @param y Value in Y
   */
  public void put(double x, double y) {
    if(sumWe <= 0.) {
      sumX = x;
      sumY = y;
      sumWe = 1;
      return;
    }
    // Delta to previous mean
    final double deltaX = x * sumWe - sumX, deltaY = y * sumWe - sumY;
    final double oldWe = sumWe;
    // Incremental update
    sumWe += 1;
    final double f = 1. / (sumWe * oldWe);
    // Update
    sumXX += f * deltaX * deltaX;
    sumYY += f * deltaY * deltaY;
    // should equal weight * deltaY * neltaX!
    sumXY += f * deltaX * deltaY;
    // Update means
    sumX += x;
    sumY += y;
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
    return sumXY / Math.sqrt(sumXX * sumYY);
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
    return sumX / sumWe;
  }

  /**
   * Return mean of Y
   * 
   * @return mean
   */
  public double getMeanY() {
    return sumY / sumWe;
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
   * <p>
   * Note: often you should be using {@link #getSampleVarianceX} instead!
   * 
   * @return variance
   */
  public double getPopulationVarianceX() {
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
   * <p>
   * Note: often you should be using {@link #getSampleStddevX} instead!
   * 
   * @return standard deviation
   */
  public double getPopulationStddevX() {
    return Math.sqrt(getPopulationVarianceX());
  }

  /**
   * Return standard deviation
   * 
   * @return standard deviation
   */
  public double getSampleStddevX() {
    return Math.sqrt(getSampleVarianceX());
  }

  /**
   * Return the naive variance (not taking sampling into account)
   * <p>
   * Note: often you should be using {@link #getSampleVarianceY} instead!
   * 
   * @return variance
   */
  public double getPopulationVarianceY() {
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
   * <p>
   * Note: often you should be using {@link #getSampleStddevY} instead!
   * 
   * @return stddev
   */
  public double getPopulationStddevY() {
    return Math.sqrt(getPopulationVarianceY());
  }

  /**
   * Return standard deviation
   * 
   * @return stddev
   */
  public double getSampleStddevY() {
    return Math.sqrt(getSampleVarianceY());
  }

  /**
   * Reset the value.
   */
  public void reset() {
    sumXX = sumXY = sumYY = sumX = sumY = sumWe = 0.;
  }

  /**
   * Compute the Pearson product-moment correlation coefficient.
   *
   * @param x first data array
   * @param y second data array
   * @return the Pearson product-moment correlation coefficient for x and y
   */
  public static double coefficient(double[] x, double[] y) {
    final int xdim = x.length, ydim = y.length;
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: arrays differ in length.");
    }
    if(xdim == 0) {
      throw new IllegalArgumentException("Empty vector.");
    }
    // Inlined computation of Pearson correlation, to avoid allocating objects!
    // This is a numerically stabilized version, avoiding sum-of-squares.
    double sumXX = 0., sumYY = 0., sumXY = 0.;
    double sumX = x[0], sumY = y[0];
    int i = 1;
    while(i < xdim) {
      final double xv = x[i], yv = y[i];
      // Delta to previous mean
      final double deltaX = xv * i - sumX, deltaY = yv * i - sumY;
      // Increment count first
      final double oldi = i; // Convert to double!
      ++i;
      final double f = 1. / (i * oldi);
      // Update
      sumXX += f * deltaX * deltaX;
      sumYY += f * deltaY * deltaY;
      // should equal deltaY * neltaX!
      sumXY += f * deltaX * deltaY;
      // Update sums
      sumX += xv;
      sumY += yv;
    }
    // One or both series were constant:
    return !(sumXX > 0. && sumYY > 0.) ? sumXX == sumYY ? 1. : 0. : //
        sumXY / Math.sqrt(sumXX * sumYY);
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
    final int xdim = x.getDimensionality(), ydim = y.getDimensionality();
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: number vectors differ in dimensionality.");
    }
    if(xdim == 0) {
      throw new IllegalArgumentException("Empty vector.");
    }
    // Inlined computation of Pearson correlation, to avoid allocating objects!
    // This is a numerically stabilized version, avoiding sum-of-squares.
    double sumXX = 0., sumYY = 0., sumXY = 0.;
    double sumX = x.doubleValue(0), sumY = y.doubleValue(0);
    int i = 1;
    while(i < xdim) {
      final double xv = x.doubleValue(i), yv = y.doubleValue(i);
      // Delta to previous mean
      final double deltaX = xv * i - sumX, deltaY = yv * i - sumY;
      // Increment count first
      final double oldi = i; // Convert to double!
      ++i;
      final double f = 1. / (i * oldi);
      // Update
      sumXX += f * deltaX * deltaX;
      sumYY += f * deltaY * deltaY;
      // should equal deltaY * neltaX!
      sumXY += f * deltaX * deltaY;
      // Update sums
      sumX += xv;
      sumY += yv;
    }
    // One or both series were constant:
    return !(sumXX > 0. && sumYY > 0.) ? sumXX == sumYY ? 1. : 0. : //
        sumXY / Math.sqrt(sumXX * sumYY);
  }

  /**
   * Compute the Pearson product-moment correlation coefficient.
   *
   * @param x first data array
   * @param y second data array
   * @param weights Weights
   * @return the Pearson product-moment correlation coefficient for x and y
   */
  public static double weightedCoefficient(double[] x, double[] y, double[] weights) {
    final int xdim = x.length, ydim = y.length;
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: arrays differ in length.");
    }
    if(xdim != weights.length) {
      throw new IllegalArgumentException("Dimensionality doesn't agree to weights.");
    }
    if(xdim == 0) {
      throw new IllegalArgumentException("Empty vector.");
    }
    // Inlined computation of Pearson correlation, to avoid allocating objects!
    // This is a numerically stabilized version, avoiding sum-of-squares.
    double sumXX = 0., sumYY = 0., sumXY = 0., sumWe = weights[0];
    double sumX = x[0] * sumWe, sumY = y[0] * sumWe;
    for(int i = 1; i < xdim; ++i) {
      final double xv = x[i], yv = y[i], w = weights[i];
      // Delta to previous mean
      final double deltaX = xv * sumWe - sumX, deltaY = yv * sumWe - sumY;
      // Increment count first
      final double oldWe = sumWe; // Convert to double!
      sumWe += w;
      final double f = w / (sumWe * oldWe);
      // Update
      sumXX += f * deltaX * deltaX;
      sumYY += f * deltaY * deltaY;
      // should equal deltaY * neltaX!
      sumXY += f * deltaX * deltaY;
      // Update sums
      sumX += xv * w;
      sumY += yv * w;
    }
    // One or both series were constant:
    return !(sumXX > 0. && sumYY > 0.) ? sumXX == sumYY ? 1. : 0. : //
        sumXY / Math.sqrt(sumXX * sumYY);
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
    final int xdim = x.getDimensionality(), ydim = y.getDimensionality();
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: number vectors differ in dimensionality.");
    }
    if(xdim != weights.length) {
      throw new IllegalArgumentException("Dimensionality doesn't agree to weights.");
    }
    if(xdim == 0) {
      throw new IllegalArgumentException("Empty vector.");
    }
    // Inlined computation of Pearson correlation, to avoid allocating objects!
    // This is a numerically stabilized version, avoiding sum-of-squares.
    double sumXX = 0., sumYY = 0., sumXY = 0., sumWe = weights[0];
    double sumX = x.doubleValue(0) * sumWe, sumY = y.doubleValue(0) * sumWe;
    for(int i = 1; i < xdim; ++i) {
      final double xv = x.doubleValue(i), yv = y.doubleValue(i), w = weights[i];
      // Delta to previous mean
      final double deltaX = xv * sumWe - sumX, deltaY = yv * sumWe - sumY;
      // Increment count first
      final double oldWe = sumWe; // Convert to double!
      sumWe += w;
      final double f = w / (sumWe * oldWe);
      // Update
      sumXX += f * deltaX * deltaX;
      sumYY += f * deltaY * deltaY;
      // should equal deltaY * neltaX!
      sumXY += f * deltaX * deltaY;
      // Update sums
      sumX += xv * w;
      sumY += yv * w;
    }
    // One or both series were constant:
    return !(sumXX > 0. && sumYY > 0.) ? sumXX == sumYY ? 1. : 0. : //
        sumXY / Math.sqrt(sumXX * sumYY);
  }

  /**
   * Compute the Pearson product-moment correlation coefficient,
   *
   * @param x first NumberVector
   * @param y second NumerVector
   * @param weights Weights
   * @return the Pearson product-moment correlation coefficient for x and y
   */
  public static double weightedCoefficient(NumberVector x, NumberVector y, NumberVector weights) {
    final int xdim = x.getDimensionality(), ydim = y.getDimensionality();
    if(xdim != ydim) {
      throw new IllegalArgumentException("Invalid arguments: feature vectors differ in dimensionality.");
    }
    if(xdim != weights.getDimensionality()) {
      throw new IllegalArgumentException("Dimensionality doesn't agree to weights.");
    }
    if(xdim == 0) {
      throw new IllegalArgumentException("Empty vector.");
    }
    // Inlined computation of Pearson correlation, to avoid allocating objects!
    // This is a numerically stabilized version, avoiding sum-of-squares.
    double sumXX = 0., sumYY = 0., sumXY = 0., sumWe = weights.doubleValue(0);
    double sumX = x.doubleValue(0) * sumWe, sumY = y.doubleValue(0) * sumWe;
    for(int i = 1; i < xdim; ++i) {
      final double xv = x.doubleValue(i), yv = y.doubleValue(i);
      final double w = weights.doubleValue(i);
      // Delta to previous mean
      final double deltaX = xv * sumWe - sumX, deltaY = yv * sumWe - sumY;
      // Increment count first
      final double oldWe = sumWe; // Convert to double!
      sumWe += w;
      final double f = w / (sumWe * oldWe);
      // Update
      sumXX += f * deltaX * deltaX;
      sumYY += f * deltaY * deltaY;
      // should equal deltaY * neltaX!
      sumXY += f * deltaX * deltaY;
      // Update sums
      sumX += xv * w;
      sumY += yv * w;
    }
    // One or both series were constant:
    return !(sumXX > 0. && sumYY > 0.) ? sumXX == sumYY ? 1. : 0. : //
        sumXY / Math.sqrt(sumXX * sumYY);
  }
}
