package de.lmu.ifi.dbs.elki.math;

/**
 * Do some simple statistics (mean, average).
 * 
 * This class can repeatedly be fed with data using the add() methods,
 * The resulting values for mean and average can be queried at any time using
 * getMean() and getVariance().
 * 
 * Trivial code, but replicated a lot. The class is final so it should come at low cost.
 * 
 * @author Erich Schubert
 *
 */
public final class MeanVariance {
  /**
   * Sum of values
   */
  public double sum = 0.0;
  /**
   * Sum of Squares
   */
  public double sqrSum = 0.0;
  /**
   * Number of Samples.
   */
  public double count = 0.0;
  
  /**
   * Empty constructor
   */
  public MeanVariance() {
    // nothing to do here, initialization done above.
  }

  /**
   * Constructor from full internal data.
   * 
   * @param sum sum
   * @param sqrSum sum of squared values
   * @param count sum of weights
   */
  public MeanVariance(double sum, double sqrSum, double count) {
    this.sum = sum;
    this.sqrSum = sqrSum;
    this.count = count;
  }

  /**
   * Constructor from other instance
   * 
   * @param other other instance to copy data from.
   */
  public MeanVariance(MeanVariance other) {
    this.sum = other.sum;
    this.sqrSum = other.sqrSum;
    this.count = other.count;   
  }

  /**
   * Add data with a given weight
   * 
   * @param val data
   * @param weight weight
   */
  public void put(double val, double weight) {
    sum    += weight * val;
    sqrSum += weight * val * val;
    count  += weight;
  }
  
  /**
   * Add a single value with weight 1.0
   * 
   * @param val Value
   */
  public void put(double val) {
    put(val, 1.0);
  }

  /**
   * Join the data of another MeanVariance instance.
   * 
   * @param other
   */
  public void put(MeanVariance other) {
    this.sum += other.sum;
    this.sqrSum += other.sqrSum;
    this.count += other.count;
  }

  /**
   * Get the number of points the average is based on.
   * @return number of data points
   */
  public double getCount() {
    return count;
  }

  /**
   * Return mean
   * @return mean
   */
  public double getMean() {
    return sum / count;
  }

  /**
   * Return variance
   * @return variance
   */
  public double getVariance() {
    double mu = sum / count;
    return (sqrSum / count) - (mu * mu);
  }
  
  /**
   * Return standard deviation
   * @return stddev
   */
  public double getStddev() {
    return Math.sqrt(getVariance());
  }
}