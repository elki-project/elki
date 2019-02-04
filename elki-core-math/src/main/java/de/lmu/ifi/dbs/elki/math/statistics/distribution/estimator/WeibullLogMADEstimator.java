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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.WeibullDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Parameter estimation via median and median absolute deviation from median
 * (MAD).
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
 * @navassoc - estimates - WeibullDistribution
 */
@Reference(authors = "D. J. Olive", //
    title = "Applied Robust Statistics", booktitle = "", //
    url = "http://lagrange.math.siu.edu/Olive/preprints.htm", //
    bibkey = "books/Olive08")
public class WeibullLogMADEstimator implements LogMADDistributionEstimator<WeibullDistribution> {
  /**
   * The more robust median based estimator.
   */
  public static final WeibullLogMADEstimator STATIC = new WeibullLogMADEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private WeibullLogMADEstimator() {
    super();
  }

  @Override
  public WeibullDistribution estimateFromLogMedianMAD(double median, double mad, double shift) {
    double k = 1. / (1.30370 * mad);
    double lambda = FastMath.exp(median - MathUtil.LOGLOG2 / k);

    return new WeibullDistribution(k, lambda);
  }

  @Override
  public Class<? super WeibullDistribution> getDistributionClass() {
    return WeibullDistribution.class;
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
    protected WeibullLogMADEstimator makeInstance() {
      return STATIC;
    }
  }
}
