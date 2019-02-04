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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.LogNormalDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimator using Medians. More robust to outliers, and just slightly more
 * expensive (needs to copy the data for partial sorting to find the median).
 * <p>
 * References:
 * <p>
 * F. R. Hampel<br>
 * The Influence Curve and Its Role in Robust Estimation<br>
 * Journal of the American Statistical Association, June 1974, Vol. 69, No. 346
 * <p>
 * P. J. Rousseeuw, C. Croux<br>
 * Alternatives to the Median Absolute Deviation<br>
 * Journal of the American Statistical Association, December 1993, Vol. 88,
 * No. 424, Theory and Methods
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @navassoc - estimates - LogNormalDistribution
 */
@Reference(authors = "F. R. Hampel", //
    title = "The Influence Curve and Its Role in Robust Estimation", //
    booktitle = "Journal of the American Statistical Association, June 1974, Vol. 69, No. 346", //
    url = "https://doi.org/10.2307/2285666", //
    bibkey = "doi:10.2307/2285666")
public class LogNormalLogMADEstimator implements LogMADDistributionEstimator<LogNormalDistribution> {
  /**
   * Static estimator, more robust to outliers by using the median.
   */
  public static LogNormalLogMADEstimator STATIC = new LogNormalLogMADEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private LogNormalLogMADEstimator() {
    super();
  }

  @Override
  public LogNormalDistribution estimateFromLogMedianMAD(double median, double mad, double shift) {
    return new LogNormalDistribution(median, Math.max(NormalDistribution.ONEBYPHIINV075 * mad, Double.MIN_NORMAL), shift);
  }

  @Override
  public Class<? super LogNormalDistribution> getDistributionClass() {
    return LogNormalDistribution.class;
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
    protected LogNormalLogMADEstimator makeInstance() {
      return STATIC;
    }
  }
}
