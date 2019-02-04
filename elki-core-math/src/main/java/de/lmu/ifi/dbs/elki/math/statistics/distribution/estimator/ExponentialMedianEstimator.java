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
 * Robust Estimators for Transformed Location Scale Families<br>
 * D. J. Olive
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @has - - - ExponentialDistribution
 */
@Reference(title = "Robust Estimators for Transformed Location Scale Families", //
    authors = "D. J. Olive", booktitle = "", //
    bibkey = "preprints/Olive06")
public class ExponentialMedianEstimator implements MADDistributionEstimator<ExponentialDistribution> {
  /**
   * Static instance.
   */
  public static final ExponentialMedianEstimator STATIC = new ExponentialMedianEstimator();

  /**
   * Private constructor, use static instance!
   */
  private ExponentialMedianEstimator() {
    // Do not instantiate
  }

  @Override
  public ExponentialDistribution estimateFromMedianMAD(double median, double mad) {
    final double scale = 1.441 * median;
    if(!(scale > 0.)) {
      throw new ArithmeticException("Data with non-positive mean cannot be exponential distributed.");
    }
    return new ExponentialDistribution(1. / scale);
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
    protected ExponentialMedianEstimator makeInstance() {
      return STATIC;
    }
  }
}
