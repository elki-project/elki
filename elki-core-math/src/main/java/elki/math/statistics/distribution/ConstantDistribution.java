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
package elki.math.statistics.distribution;

import java.util.Random;

import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Pseudo distribution, that has a unique constant value.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class ConstantDistribution implements Distribution {
  /**
   * The constant
   */
  final double c;

  /**
   * Constructor.
   * 
   * @param c Constant
   */
  public ConstantDistribution(double c) {
    super();
    this.c = c;
  }

  @Override
  public double nextRandom(Random random) {
    return c;
  }

  /**
   * Probability mass function.
   *
   * @param val Value
   * @return probability mass pmf(val)
   */
  public double pmf(double val) {
    return (val == c) ? 1. : 0;
  }

  @Override
  public double pdf(double val) {
    return (val == c) ? Double.MAX_VALUE : 0;
  }

  @Override
  public double logpdf(double val) {
    return (val == c) ? Double.MAX_VALUE : Double.NEGATIVE_INFINITY;
  }

  @Override
  public double cdf(double val) {
    return (val < c) ? 0. : 1.;
  }

  @Override
  public double quantile(double val) {
    return c;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Constant value parameter
     */
    public static final OptionID CONSTANT_ID = new OptionID("distribution.constant", "Constant value.");

    /** Parameters. */
    double constant;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(CONSTANT_ID) //
          .grab(config, x -> constant = x);
    }

    @Override
    public ConstantDistribution make() {
      return new ConstantDistribution(constant);
    }
  }
}
