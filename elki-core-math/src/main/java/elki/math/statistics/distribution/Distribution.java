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

/**
 * Statistical distributions, with their common functions.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public interface Distribution {
  /**
   * Return the density of an existing value
   * 
   * @param val existing value
   * @return distribution density
   */
  double pdf(double val);

  /**
   * Return the log density of an existing value
   * 
   * @param val existing value
   * @return log distribution density
   */
  double logpdf(double val);

  /**
   * Return the cumulative density function at the given value.
   * 
   * @param val existing value
   * @return cumulative density
   */
  double cdf(double val);

  /**
   * Quantile aka probit (for normal) aka inverse CDF (invcdf, cdf^-1) function.
   * 
   * @param val Quantile to find
   * @return Quantile position
   */
  double quantile(double val);

  /**
   * Generate a new random value
   * 
   * @param random Random number generator
   * @return new random value
   */
  default double nextRandom(Random random) {
    return quantile(random.nextDouble());
  }

  /**
   * Describe the distribution
   * 
   * @return description
   */
  @Override
  String toString();

  /**
   * Common distributions parameters.
   *
   * @author Erich Schubert
   */
  interface Parameterizer extends elki.utilities.optionhandling.Parameterizer {
    /**
     * Location parameter.
     */
    OptionID LOCATION_ID = new OptionID("distribution.location", "Distribution location parameter");

    /**
     * Scale parameter.
     */
    OptionID SCALE_ID = new OptionID("distribution.scale", "Distribution scale parameter");

    /**
     * Shape parameter.
     */
    OptionID SHAPE_ID = new OptionID("distribution.shape", "Distribution shape parameter");
  }
}
