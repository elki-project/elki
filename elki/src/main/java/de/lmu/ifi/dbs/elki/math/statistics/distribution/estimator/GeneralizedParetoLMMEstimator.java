package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.math.statistics.distribution.GeneralizedParetoDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate the parameters of a Generalized Pareto Distribution (GPD), using the
 * methods of L-Moments (LMM).
 *
 * Reference:
 * <p>
 * J. R. M. Hosking, J. R. Wallis, and E. F. Wood<br />
 * Estimation of the generalized extreme-value distribution by the method of
 * probability-weighted moments.<br />
 * Technometrics 27.3
 * </p>
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @apiviz.has GeneralizedParetoDistribution
 */
@Reference(authors = "J.R.M. Hosking, J. R. Wallis, and E. F. Wood", //
title = "Estimation of the generalized extreme-value distribution by the method of probability-weighted moments.", //
booktitle = "Technometrics 27.3", //
url = "http://dx.doi.org/10.1080/00401706.1985.10488049")
public class GeneralizedParetoLMMEstimator extends AbstractLMMEstimator<GeneralizedParetoDistribution> {
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
    double xi = (1. - 3. * t3) / (1. + t3);
    double sigma = 1. / ((1 + xi) * (2 + xi) + xmom[1]);
    double mu = xmom[0] - sigma / (1 + xi);
    return new GeneralizedParetoDistribution(mu, sigma, -xi);
  }

  @Override
  public Class<? super GeneralizedParetoDistribution> getDistributionClass() {
    return GeneralizedParetoDistribution.class;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected GeneralizedParetoLMMEstimator makeInstance() {
      return STATIC;
    }
  }
}
