package de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution;

import java.util.Random;

/**
 * Simple uniform distribution class
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public final class UniformDistribution implements Distribution {
  /**
   * Minimum
   */
  private double min;
  /**
   * Maximum
   */
  private double max;
  /**
   * Len := max - min
   */
  private double len;

  /**
   * The random generator.
   */
  private Random random;

  /**
   * Constructor for a uniform distribution on the interval [min, max[
   * @param min Minimum value
   * @param max Maximum value
   */
  public UniformDistribution(double min, double max, Random random) {
    // Swap parameters if they were given incorrectly.
    if (min > max) {
      double tmp = min; min = max; max = tmp;
    }
    this.min = min;
    this.max = max;
    this.len = max - min;
    this.random = random;
  }

  /**
   * Return the PDF of the generators distribution
   */
  public double explain(double val) {
    if (val < min || val >= max) return 0.0;
    return 1.0/len;
  }

  /**
   * Generate a random value with the generators parameters
   */
  public double generate() {
    return min + random.nextDouble() * len;
  }

  /**
   * Simple toString explaining the distribution parameters.
   * 
   * Used in describing cluster models.
   */
  @Override
  public String toString() {
    return "Uniform Distribution (min="+min+", max="+max+")";
  }
}
