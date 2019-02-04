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

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.LogisticDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate Logistic distribution parameters using Median and MAD.
 * <p>
 * Reference:
 * <p>
 * Robust Estimators for Transformed Location Scale Families<br>
 * D. J. Olive
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @has - - - LogisticDistribution
 */
@Reference(title = "Robust Estimators for Transformed Location Scale Families", //
    authors = "D. J. Olive", booktitle = "", //
    bibkey = "preprints/Olive06")
public class LogisticMADEstimator implements MADDistributionEstimator<LogisticDistribution> {
  /**
   * Static instance.
   */
  public static final LogisticMADEstimator STATIC = new LogisticMADEstimator();

  /**
   * Private constructor, use static instance!
   */
  private LogisticMADEstimator() {
    // Do not instantiate
  }

  @Override
  public LogisticDistribution estimateFromMedianMAD(double median, double mad) {
    return new LogisticDistribution(median, mad / MathUtil.LOG3);
  }

  @Override
  public Class<? super LogisticDistribution> getDistributionClass() {
    return LogisticDistribution.class;
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
    protected LogisticMADEstimator makeInstance() {
      return STATIC;
    }
  }
}
