package de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution;

import java.util.Random;

/**
 * Simple generator for a Gaussian = Normal Distribution
 * 
 * @author Erich Schubert
 */
public final class NormalDistribution implements Distribution {
  /**
   * Mean value for the generator
   */
  private double mean;

  /**
   * Standard deviation
   */
  private double stddev;

  /**
   * The random generator.
   */
  private Random random;

  /**
   * Constructor for Gaussian generator
   * 
   * @param mean Mean
   * @param stddev Standard Deviation
   * @param random Random generator
   */
  public NormalDistribution(double mean, double stddev, Random random) {
    this.mean = mean;
    this.stddev = stddev;
    this.random = random;
  }

  /**
   * Standardized Gaussian PDF
   * 
   * @param x query value
   * @return probability density
   */
  // TODO: make a math.distributions package with various PDF, CDF, Error
  // functions etc.?
  private static double phi(double x) {
    return Math.exp(-x * x / 2) / Math.sqrt(2 * Math.PI);
  }

  /**
   * Gaussian distribution PDF
   * 
   * @param x query value
   * @param mu mean
   * @param sigma standard distribution
   * @return probability density
   */
  public static double phi(double x, double mu, double sigma) {
    return phi((x - mu) / sigma) / sigma;
  }

  /**
   * Return the PDF of the generators distribution
   */
  public double explain(double val) {
    return phi(val, mean, stddev);
  }

  /**
   * Generate a random value with the generators parameters
   */
  public double generate() {
    return mean + random.nextGaussian() * stddev;
  }

  /**
   * Simple toString explaining the distribution parameters.
   * 
   * Used in producing a model description.
   */
  @Override
  public String toString() {
    return "Normal Distribution (mean="+mean+", stddev="+stddev+")";
  }
}
