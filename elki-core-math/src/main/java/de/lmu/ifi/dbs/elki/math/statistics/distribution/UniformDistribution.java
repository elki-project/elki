package de.lmu.ifi.dbs.elki.math.statistics.distribution;

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

import java.util.Random;

import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Uniform distribution.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
@Alias("de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution.UniformDistribution")
public class UniformDistribution extends AbstractDistribution {
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
   * Constructor for a uniform distribution on the interval [min, max[
   * 
   * @param min Minimum value
   * @param max Maximum value
   * @param random Random generator
   */
  public UniformDistribution(double min, double max, RandomFactory random) {
    super(random);
    if (Double.isInfinite(min) || Double.isInfinite(max)) {
      throw new ArithmeticException("Infinite values given for uniform distribution.");
    }
    if (Double.isNaN(min) || Double.isNaN(max)) {
      throw new ArithmeticException("NaN values given for uniform distribution.");
    }
    // Swap parameters if they were given incorrectly.
    if (min > max) {
      double tmp = min;
      min = max;
      max = tmp;
    }
    this.min = min;
    this.max = max;
    this.len = max - min;
  }

  /**
   * Constructor for a uniform distribution on the interval [min, max[
   * 
   * @param min Minimum value
   * @param max Maximum value
   * @param random Random generator
   */
  public UniformDistribution(double min, double max, Random random) {
    super(random);
    if (Double.isInfinite(min) || Double.isInfinite(max)) {
      throw new ArithmeticException("Infinite values given for uniform distribution.");
    }
    if (Double.isNaN(min) || Double.isNaN(max)) {
      throw new ArithmeticException("NaN values given for uniform distribution.");
    }
    // Swap parameters if they were given incorrectly.
    if (min > max) {
      double tmp = min;
      min = max;
      max = tmp;
    }
    this.min = min;
    this.max = max;
    this.len = max - min;
  }

  /**
   * Constructor for a uniform distribution on the interval [min, max[
   * 
   * @param min Minimum value
   * @param max Maximum value
   */
  public UniformDistribution(double min, double max) {
    this(min, max, (Random) null);
  }

  @Override
  public double pdf(double val) {
    if (!(val >= min) || val > max) {
      return 0.0;
    }
    return (len > 0.) ? 1.0 / len : Double.POSITIVE_INFINITY;
  }

  @Override
  public double logpdf(double val) {
    if (!(val >= min) || val > max) {
      return Double.NEGATIVE_INFINITY;
    }
    return (len > 0.) ? Math.log(1.0 / len) : Double.POSITIVE_INFINITY;
  }

  @Override
  public double cdf(double val) {
    if (!(val > min)) {
      return 0.;
    }
    if (val >= max) {
      return 1.;
    }
    return (len > 0.) ? (val - min) / len : .5;
  }

  @Override
  public double quantile(double val) {
    return min + len * val;
  }

  @Override
  public double nextRandom() {
    return min + random.nextDouble() * len;
  }

  @Override
  public String toString() {
    return "UniformDistribution(min=" + min + ", max=" + max + ")";
  }

  /**
   * @return the minimum value
   */
  public double getMin() {
    return min;
  }

  /**
   * @return the maximum value
   */
  public double getMax() {
    return max;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /**
     * Minimum value
     */
    public static final OptionID MIN_ID = new OptionID("distribution.min", "Minimum value of distribution.");

    /**
     * Maximum value
     */
    public static final OptionID MAX_ID = new OptionID("distribution.max", "Maximum value of distribution.");

    /** Parameters. */
    double min, max;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter minP = new DoubleParameter(MIN_ID);
      if (config.grab(minP)) {
        min = minP.doubleValue();
      }

      DoubleParameter maxP = new DoubleParameter(MAX_ID);
      if (config.grab(maxP)) {
        max = maxP.doubleValue();
      }
    }

    @Override
    protected UniformDistribution makeInstance() {
      return new UniformDistribution(min, max, rnd);
    }
  }
}
