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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.GammaDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.WeibullDistribution;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate parameters of the Weibull distribution using the method of L-Moments
 * (LMM).
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.has WeibullDistribution
 */
public class WeibullLMMEstimator extends AbstractLMMEstimator<WeibullDistribution> {
  /**
   * Static instance.
   */
  public static final WeibullLMMEstimator STATIC = new WeibullLMMEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private WeibullLMMEstimator() {
    super();
  }

  @Override
  public int getNumMoments() {
    return 3;
  }

  @Override
  public WeibullDistribution estimateFromLMoments(double[] xmom) {
    double l = xmom[2], l2 = l * l, l3 = l2 * l, l4 = l3 * l, l5 = l4 * l, l6 = l5 * l;
    double k = 285.3 * l6 - 658.6 * l5 + 622.8 * l4 - 317.2 * l3 + 98.52 * l2 - 21.256 * l + 3.516;

    double gam = GammaDistribution.gamma(1. + 1. / k);
    double lambda = xmom[1] / (1. - Math.pow(2., -1. / k) * gam);
    double mu = xmom[0] - lambda * gam;

    return new WeibullDistribution(k, lambda, mu);
  }

  @Override
  public Class<? super WeibullDistribution> getDistributionClass() {
    return WeibullDistribution.class;
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
    protected WeibullLMMEstimator makeInstance() {
      return STATIC;
    }
  }
}
