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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.LogLogisticDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

import net.jafama.FastMath;

/**
 * Estimate Logistic distribution parameters using Median and MAD.
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
 * @has - - - LogLogisticDistribution
 */
@Reference(authors = "D. J. Olive", //
    title = "Applied Robust Statistics", booktitle = "", //
    url = "http://lagrange.math.siu.edu/Olive/preprints.htm", //
    bibkey = "books/Olive08")
public class LogLogisticMADEstimator implements LogMADDistributionEstimator<LogLogisticDistribution> {
  /**
   * Static instance.
   */
  public static final LogLogisticMADEstimator STATIC = new LogLogisticMADEstimator();

  /**
   * Private constructor, use static instance!
   */
  private LogLogisticMADEstimator() {
    // Do not instantiate
  }

  @Override
  public LogLogisticDistribution estimateFromLogMedianMAD(double median, double mad, double shift) {
    return new LogLogisticDistribution(mad / MathUtil.LOG3, shift, FastMath.exp(median) - shift);
  }

  @Override
  public Class<? super LogLogisticDistribution> getDistributionClass() {
    return LogLogisticDistribution.class;
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
    protected LogLogisticMADEstimator makeInstance() {
      return STATIC;
    }
  }
}
