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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.GeneralizedParetoDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate the parameters of a Generalized Pareto Distribution (GPD), using the
 * methods of L-Moments (LMM).
 * <p>
 * Reference:
 * <p>
 * J. R. M. Hosking, J. R. Wallis, E. F. Wood<br>
 * Estimation of the generalized extreme-value distribution by the method of
 * probability-weighted moments.<br>
 * Technometrics 27.3
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - GeneralizedParetoDistribution
 */
@Reference(authors = "J. R. M. Hosking, J. R. Wallis, E. F. Wood", //
    title = "Estimation of the generalized extreme-value distribution by the method of probability-weighted moments.", //
    booktitle = "Technometrics 27.3", //
    url = "https://doi.org/10.1080/00401706.1985.10488049", //
    bibkey = "doi:10.1080/00401706.1985.10488049")
public class GeneralizedParetoLMMEstimator implements LMMDistributionEstimator<GeneralizedParetoDistribution> {
  /**
   * Static instance.
   */
  public static final GeneralizedParetoLMMEstimator STATIC = new GeneralizedParetoLMMEstimator();

  /**
   * Constructor. Private: use static instance.
   */
  private GeneralizedParetoLMMEstimator() {
    super();
  }

  @Override
  public int getNumMoments() {
    return 3;
  }

  @Override
  public GeneralizedParetoDistribution estimateFromLMoments(double[] xmom) {
    double t3 = xmom[2];
    if(Math.abs(t3) >= 1.) {
      throw new ArithmeticException("Invalid moment estimation.");
    }
    double kappa = (1. - 3. * t3) / (1. + t3);
    double sigma = (1 + kappa) * (2 + kappa) * xmom[1];
    double mu = xmom[0] - sigma / (1 + kappa);
    return new GeneralizedParetoDistribution(mu, sigma, -kappa);
  }

  @Override
  public Class<? super GeneralizedParetoDistribution> getDistributionClass() {
    return GeneralizedParetoDistribution.class;
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
    protected GeneralizedParetoLMMEstimator makeInstance() {
      return STATIC;
    }
  }
}
