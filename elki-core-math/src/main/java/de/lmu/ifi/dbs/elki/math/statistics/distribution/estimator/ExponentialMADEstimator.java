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
package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

import de.lmu.ifi.dbs.elki.math.statistics.distribution.ExponentialDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate Exponential distribution parameters using Median and MAD.
 * <p>
 * Reference:
 * <p>
 * D. J. Olive<br>
 * Applied Robust Statistics<br>
 * Preprint, University of Minnesota
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @has - - - ExponentialDistribution
 */
@Reference(authors = "D. J. Olive", //
    title = "Applied Robust Statistics", booktitle = "", //
    url = "http://lagrange.math.siu.edu/Olive/preprints.htm", //
    bibkey = "books/Olive08")
public class ExponentialMADEstimator implements MADDistributionEstimator<ExponentialDistribution> {
  /**
   * Static instance.
   */
  public static final ExponentialMADEstimator STATIC = new ExponentialMADEstimator();

  /**
   * Private constructor, use static instance!
   */
  private ExponentialMADEstimator() {
    // Do not instantiate
  }

  @Override
  public ExponentialDistribution estimateFromMedianMAD(double median, double mad) {
    final double location = median - 1.440 * mad;
    final double scale = 2.0781 * mad;
    if(!(scale > 0.)) {
      throw new ArithmeticException("Data with non-positive MAD cannot be exponential distributed.");
    }
    return new ExponentialDistribution(1. / scale, location);
  }

  @Override
  public Class<? super ExponentialDistribution> getDistributionClass() {
    return ExponentialDistribution.class;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected ExponentialMADEstimator makeInstance() {
      return STATIC;
    }
  }
}
