package de.lmu.ifi.dbs.elki.math;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Do some simple statistics (mean, variance) using a numerically stable online algorithm.
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
 * @author Erich Schubert
 */
@Reference(authors="B. P. Welford", title="Note on a method for calculating corrected sums of squares and products", booktitle = "Technometrics 4(3)")
public final class MeanVariance {
  /**
   * Mean of values
   */
  public double mean = 0.0;

  /**
   * nVariance
   */
  public double nvar = 0.0;

  /**
   * Weight sum (number of samples)
   */
  public double wsum = 0;

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
  public void put(double val) {
    wsum += 1.0;
    final double delta = val - mean;
    mean += delta / wsum;
    // The next line needs the *new* mean!
    nvar += delta * (val - mean);
  }

  /**
   * Add data with a given weight
   * 
   * @param val data
   * @param weight weight
   */
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
  public void put(MeanVariance other) {
    final double nwsum = other.wsum + this.wsum; 
    final double delta = other.mean - this.mean;
    final double rval = delta * other.wsum / nwsum;

    // this.mean += rval;
    // This supposedly is more numerically stable:
    this.mean = (this.wsum * this.mean + other.wsum * other.mean) / nwsum;
    this.nvar += other.nvar + delta * this.wsum * rval;
    this.wsum = nwsum;
  }

  /**
   * Get the number of points the average is based on.
   * 
   * @return number of data points
   */
  public double getCount() {
    return wsum;
  }

  /**
   * Return mean
   * 
   * @return mean
   */
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
    assert(wsum > 1);
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