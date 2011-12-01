package de.lmu.ifi.dbs.elki.math;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
 */
@Reference(authors = "B. P. Welford", title = "Note on a method for calculating corrected sums of squares and products", booktitle = "Technometrics 4(3)")
public class MeanVariance extends Mean {
  /**
   * nVariance
   */
  protected double nvar = 0.0;

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
    this.mean = other.mean;
    this.nvar = other.nvar;
    this.wsum = other.wsum;
  }

  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  @Override
  public void put(double val) {
    wsum += 1.0;
    final double delta = val - mean;
    mean += delta / wsum;
    // The next line needs the *new* mean!
    nvar += delta * (val - mean);
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
  public void put(double val, double weight) {
    final double nwsum = weight + wsum;
    final double delta = val - mean;
    final double rval = delta * weight / nwsum;
    mean += rval;
    // Use old and new weight sum here:
    nvar += wsum * delta * rval;
    wsum = nwsum;
  }

  /**
   * Join the data of another MeanVariance instance.
   * 
   * @param other Data to join with
   */
  @Override
  public void put(Mean other) {
    if(other instanceof MeanVariance) {
      final double nwsum = other.wsum + this.wsum;
      final double delta = other.mean - this.mean;
      final double rval = delta * other.wsum / nwsum;

      // this.mean += rval;
      // This supposedly is more numerically stable:
      this.mean = (this.wsum * this.mean + other.wsum * other.mean) / nwsum;
      this.nvar += ((MeanVariance) other).nvar + delta * this.wsum * rval;
      this.wsum = nwsum;
    }
    else {
      throw new AbortException("I cannot combine Mean and MeanVariance to a MeanVariance.");
    }
  }

  /**
   * Get the number of points the average is based on.
   * 
   * @return number of data points
   */
  @Override
  public double getCount() {
    return wsum;
  }

  /**
   * Return mean
   * 
   * @return mean
   */
  @Override
  public double getMean() {
    return mean;
  }

  /**
   * Return the naive variance (not taking sampling into account)
   * 
   * Note: usually, you should be using {@link #getSampleVariance} instead!
   * 
   * @return variance
   */
  public double getNaiveVariance() {
    return nvar / wsum;
  }

  /**
   * Return sample variance.
   * 
   * @return sample variance
   */
  public double getSampleVariance() {
    assert (wsum > 1) : "Cannot compute a reasonable sample variance with weight <= 1.0!";
    return nvar / (wsum - 1);
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
    return "MeanVariance(mean=" + getMean() + ",var=" + getSampleVariance() + ")";
  }
}