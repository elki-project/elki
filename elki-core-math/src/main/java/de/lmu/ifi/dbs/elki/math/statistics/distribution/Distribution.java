/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.math.statistics.distribution;

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
   * @return new random value
   */
  double nextRandom();

  /**
   * Describe the distribution
   * 
   * @return description
   */
  @Override
  String toString();
}
