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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.RayleighDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate the parameters of a RayleighDistribution using the MAD.
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
 * @navassoc - estimates - RayleighDistribution
 */
@Reference(authors = "D. J. Olive", //
    title = "Applied Robust Statistics", booktitle = "", //
    url = "http://lagrange.math.siu.edu/Olive/preprints.htm", //
    bibkey = "books/Olive08")
public class RayleighMADEstimator implements MADDistributionEstimator<RayleighDistribution> {
  /**
   * Static instance.
   */
  public static final RayleighMADEstimator STATIC = new RayleighMADEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private RayleighMADEstimator() {
    super();
  }

  /**
   * See reference for the derivation of this constants.
   */
  private static final double F1 = 1. / 0.448453, F2 = 1.17741 * F1;

  @Override
  public RayleighDistribution estimateFromMedianMAD(double median, double mad) {
    return new RayleighDistribution(median - F2 * mad, F1 * mad);
  }

  @Override
  public Class<? super RayleighDistribution> getDistributionClass() {
    return RayleighDistribution.class;
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
    protected RayleighMADEstimator makeInstance() {
      return STATIC;
    }
  }
}
