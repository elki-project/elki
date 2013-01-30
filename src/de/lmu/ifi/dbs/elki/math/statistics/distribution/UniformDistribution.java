package de.lmu.ifi.dbs.elki.math.statistics.distribution;

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

import java.util.Random;

import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Uniform distribution.
 * 
 * @author Erich Schubert
 */
public class UniformDistribution implements DistributionWithRandom {
  /**
   * The most naive estimator possible: uses minimum and maximum.
   */
  public static final NaiveMinMaxEstimator NAIVE_MINMAX_ESTIMATION = new NaiveMinMaxEstimator();

  /**
   * Slightly more refined estimator: takes sample size into account.
   */
  public static final RefinedMinMaxEstimator REFINED_MINMAX_ESTIMATION = new RefinedMinMaxEstimator();

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
   * 
   * @param min Minimum value
   * @param max Maximum value
   * @param random Random generator
   */
  public UniformDistribution(double min, double max, Random random) {
    super();
    // Swap parameters if they were given incorrectly.
    if (min > max) {
      double tmp = min;
      min = max;
      max = tmp;
    }
    this.min = min;
    this.max = max;
    this.len = max - min;
    this.random = random;
  }

  /**
   * Constructor for a uniform distribution on the interval [min, max[
   * 
   * @param min Minimum value
   * @param max Maximum value
   */
  public UniformDistribution(double min, double max) {
    this(min, max, new Random());
  }

  @Override
  public double pdf(double val) {
    if (val < min || val >= max) {
      return 0.0;
    }
    return 1.0 / len;
  }

  @Override
  public double cdf(double val) {
    if (val < min) {
      return 0.0;
    }
    if (val > max) {
      return 1.0;
    }
    return (val - min) / len;
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
   * Estimate the uniform distribution by computing min and max.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has UniformDistribution - - estimates
   */
  public static class NaiveMinMaxEstimator implements DistributionEstimator<UniformDistribution> {
    @Override
    public <A> UniformDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
      final int len = adapter.size(data);
      DoubleMinMax mm = new DoubleMinMax();
      for (int i = 0; i < len; i++) {
        mm.put(adapter.getDouble(data, i));
      }
      return estimate(mm);
    }

    /**
     * Estimate parameters from minimum and maximum observed.
     * 
     * @param mm Minimum and Maximum
     * @return Estimation
     */
    public UniformDistribution estimate(DoubleMinMax mm) {
      return new UniformDistribution(mm.getMin(), mm.getMax());
    }

    @Override
    public Class<? super UniformDistribution> getDistributionClass() {
      return UniformDistribution.class;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected NaiveMinMaxEstimator makeInstance() {
        return NAIVE_MINMAX_ESTIMATION;
      }
    }
  }

  /**
   * Slightly improved estimation, that takes sample size into account.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has UniformDistribution - - estimates
   */
  public static class RefinedMinMaxEstimator implements DistributionEstimator<UniformDistribution> {
    @Override
    public <A> UniformDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
      final int len = adapter.size(data);
      DoubleMinMax mm = new DoubleMinMax();
      for (int i = 0; i < len; i++) {
        mm.put(adapter.getDouble(data, i));
      }
      double grow = 0.5 * mm.getDiff() / (len - 1);
      return new UniformDistribution(mm.getMin() - grow, mm.getMax() + grow);
    }

    @Override
    public Class<? super UniformDistribution> getDistributionClass() {
      return UniformDistribution.class;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected RefinedMinMaxEstimator makeInstance() {
        return REFINED_MINMAX_ESTIMATION;
      }
    }
  }
}
