package de.lmu.ifi.dbs.elki.math;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Do some simple statistics (mean, variance) using a numerically stable online
 * algorithm.
 * 
 * This class can repeatedly be fed with data using the add() methods, the
 * resulting values for mean and average can be queried at any time using
 * getMean() and getSampleVariance().
 * 
 * Make sure you have understood variance correctly when using
 * getNaiveVariance() - since this class is fed with samples and estimates the
 * mean from the samples, getSampleVariance() is the proper formula.
 * 
 * Trivial code, but replicated a lot. The class is final so it should come at
 * low cost.
 * 
 * Related Literature:
 * 
 * <p>
 * B. P. Welford<br />
 * Note on a method for calculating corrected sums of squares and products<br />
 * in: Technometrics 4(3)
 * </p>
 * 
 * <p>
 * D.H.D. West<br />
 * Updating Mean and Variance Estimates: An Improved Method<br />
 * In: Communications of the ACM, Volume 22 Issue 9
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.2
 */
@Reference(authors = "B. P. Welford", //
title = "Note on a method for calculating corrected sums of squares and products", //
booktitle = "Technometrics 4(3)")
public class MeanVariance extends Mean {
  /**
   * nVariance
   */
  protected double m2 = 0.0;

  /**
   * Empty constructor
   */
  public MeanVariance() {
    // nothing to do here, initialization done above.
  }

  /**
   * Constructor from other instance
   * 
   * @param other other instance to copy data from.
   */
  public MeanVariance(MeanVariance other) {
    this.m1 = other.m1;
    this.m2 = other.m2;
    this.n = other.n;
  }

  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  @Override
  public void put(double val) {
    n += 1.0;
    final double delta = val - m1;
    m1 += delta / n;
    // The next line needs the *new* mean!
    m2 += delta * (val - m1);
  }

  /**
   * Add data with a given weight.
   * 
   * See also: D.H.D. West<br />
   * Updating Mean and Variance Estimates: An Improved Method
   * 
   * @param val data
   * @param weight weight
   */
  @Override
  @Reference(authors = "D.H.D. West", //
  title = "Updating Mean and Variance Estimates: An Improved Method", //
  booktitle = "Communications of the ACM, Volume 22 Issue 9")
  public void put(double val, double weight) {
    final double nwsum = weight + n;
    final double delta = val - m1;
    final double rval = delta * weight / nwsum;
    m1 += rval;
    // Use old and new weight sum here:
    m2 += n * delta * rval;
    n = nwsum;
  }

  /**
   * Join the data of another MeanVariance instance.
   * 
   * @param other Data to join with
   */
  @Override
  public void put(Mean other) {
    if(other instanceof MeanVariance) {
      final double nwsum = other.n + this.n;
      final double delta = other.m1 - this.m1;
      final double rval = delta * other.n / nwsum;

      // this.mean += rval;
      // This supposedly is more numerically stable:
      this.m1 = (this.n * this.m1 + other.n * other.m1) / nwsum;
      this.m2 += ((MeanVariance) other).m2 + delta * this.n * rval;
      this.n = nwsum;
    }
    else {
      throw new AbortException("I cannot combine Mean and MeanVariance to a MeanVariance.");
    }
  }

  /**
   * Add values with weight 1.0
   * 
   * @param vals Values
   * @return this
   */
  @Override
  public MeanVariance put(double[] vals) {
    if(vals.length <= 2) {
      final int l = vals.length;
      int i = 0;
      while(i < l) {
        put(vals[l]);
      }
      return this;
    }
    // First pass:
    double sum = 0.;
    final int l = vals.length;
    int i = 0;
    while(i < l) {
      sum += vals[l];
    }
    double om1 = sum / vals.length;
    // Second pass:
    double om2 = 0.;
    i = 0;
    while(i < l) {
      final double v = vals[l] - om1;
      om2 += v * v;
    }
    final double nwsum = vals.length + this.n;
    final double delta = om1 - this.m1;
    final double rval = delta * vals.length / nwsum;

    // this.mean += rval;
    // This supposedly is more numerically stable:
    this.m1 = (this.n * this.m1 + sum) / nwsum;
    this.m2 += om2 + delta * this.n * rval;
    this.n = nwsum;
    return this;
  }

  @Override
  public MeanVariance put(double[] vals, double[] weights) {
    assert (vals.length == weights.length);
    for(int i = 0, end = vals.length; i < end; i++) {
      // TODO: use a two-pass update as in the other put
      put(vals[i], weights[i]);
    }
    return this;
  }

  /**
   * Get the number of points the average is based on.
   * 
   * @return number of data points
   */
  @Override
  public double getCount() {
    return n;
  }

  /**
   * Return mean
   * 
   * @return mean
   */
  @Override
  public double getMean() {
    return m1;
  }

  /**
   * Return the naive variance (not taking sampling into account)
   * 
   * Note: usually, you should be using {@link #getSampleVariance} instead!
   * 
   * @return variance
   */
  public double getNaiveVariance() {
    return m2 / n;
  }

  /**
   * Return sample variance.
   * 
   * @return sample variance
   */
  public double getSampleVariance() {
    if(!(n > 1.)) {
      throw new ArithmeticException("Cannot compute a reasonable sample variance with weight <= 1.0!");
    }
    return m2 / (n - 1);
  }

  /**
   * Return standard deviation using the non-sample variance
   * 
   * Note: usually, you should be using {@link #getSampleStddev} instead!
   * 
   * @return stddev
   */
  public double getNaiveStddev() {
    return Math.sqrt(getNaiveVariance());
  }

  /**
   * Return standard deviation
   * 
   * @return stddev
   */
  public double getSampleStddev() {
    return Math.sqrt(getSampleVariance());
  }

  /**
   * Return the normalized value (centered at the mean, distance normalized by
   * standard deviation)
   * 
   * @param val original value
   * @return normalized value
   */
  public double normalizeValue(double val) {
    return (val - getMean()) / getSampleStddev();
  }

  /**
   * Return the unnormalized value (centered at the mean, distance normalized by
   * standard deviation)
   * 
   * @param val normalized value
   * @return de-normalized value
   */
  public double denormalizeValue(double val) {
    return (val * getSampleStddev()) + getMean();
  }

  /**
   * Create and initialize a new array of MeanVariance
   * 
   * @param dimensionality Dimensionality
   * @return New and initialized Array
   */
  public static MeanVariance[] newArray(int dimensionality) {
    MeanVariance[] arr = new MeanVariance[dimensionality];
    for(int i = 0; i < dimensionality; i++) {
      arr[i] = new MeanVariance();
    }
    return arr;
  }

  @Override
  public String toString() {
    return "MeanVariance(mean=" + getMean() + ",var=" + ((n > 1.) ? getSampleVariance() : "n/a") + ")";
  }

  @Override
  public void reset() {
    super.reset();
    m2 = 0;
  }
}
