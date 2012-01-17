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
/**
 * Class to incrementally compute pearson correlation.
 * 
 * In fact, this actually computes Var(X), Var(Y) and Cov(X, Y), all of which
 * can be obtained from this class. If you need more than two variables, use
 * {@link de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix} which uses
 * slightly more memory (by using arrays) but essentially does the same.
 * 
 * @author Erich Schubert
 */
public class PearsonCorrelation {
  /**
   * Sum for XX
   */
  private double sumXX = 0;

  /**
   * Sum for YY
   */
  private double sumYY = 0;

  /**
   * Sum for XY
   */
  private double sumXY = 0;

  /**
   * Current mean for X
   */
  private double meanX = 0;

  /**
   * Current mean for Y
   */
  private double meanY = 0;

  /**
   * Weight sum
   */
  private double sumWe = 0;

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
    if(sumWe <= 0.0) {
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
    put(x, y, 1.0);
  }

  /**
   * Get the pearson correlation value.
   * 
   * @return Correlation value
   */
  public double getCorrelation() {
    final double popSdX = getNaiveStddevX();
    final double popSdY = getNaiveStddevY();
    final double covXY = getNaiveCovariance();
    if(popSdX == 0 || popSdY == 0) {
      return 0;
    }
    return covXY / (popSdX * popSdY);
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
    assert (sumWe > 1);
    return sumXY / (sumWe - 1);
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
    assert (sumWe > 1);
    return sumXX / (sumWe - 1);
  }

  /**
   * Return standard deviation using the non-sample variance
   * 
   * Note: usually, you should be using {@link #getSampleStddevX} instead!
   * 
   * @return stddev
   */
  public double getNaiveStddevX() {
    return Math.sqrt(getNaiveVarianceX());
  }

  /**
   * Return standard deviation
   * 
   * @return stddev
   */
  public double getSampleStddevX() {
    return Math.sqrt(getSampleVarianceX());
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
    assert (sumWe > 1);
    return sumYY / (sumWe - 1);
  }

  /**
   * Return standard deviation using the non-sample variance
   * 
   * Note: usually, you should be using {@link #getSampleStddevY} instead!
   * 
   * @return stddev
   */
  public double getNaiveStddevY() {
    return Math.sqrt(getNaiveVarianceY());
  }

  /**
   * Return standard deviation
   * 
   * @return stddev
   */
  public double getSampleStddevY() {
    return Math.sqrt(getSampleVarianceY());
  }
}