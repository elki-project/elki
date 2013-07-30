package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.math.StatisticalMoments;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.GammaDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.WeibullDistribution;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate a Weibull distribution from the statistical moments.
 * 
 * FIXME: is this sound at all? There is currently no reference associated, and
 * this was probably just created during testing by copy and pasting from the
 * L-Moments based code!
 * 
 * @author Erich Schubert
 */
public class WeibullMOMEstimator extends AbstractMOMEstimator<WeibullDistribution> {
  /**
   * Static instance.
   */
  public static final WeibullMOMEstimator STATIC = new WeibullMOMEstimator();

  /**
   * Constructor. Private: use static instance.
   */
  private WeibullMOMEstimator() {
    super();
  }

  @Override
  public WeibullDistribution estimateFromStatisticalMoments(StatisticalMoments mom) {
    double l = mom.getSampleSkewness(), l2 = l * l, l3 = l2 * l, l4 = l3 * l, l5 = l4 * l, l6 = l5 * l;
    double k = 285.3 * l6 - 658.6 * l5 + 622.8 * l4 - 317.2 * l3 + 98.52 * l2 - 21.256 * l + 3.516;

    double gam = GammaDistribution.gamma(1. + 1. / k);
    double lambda = mom.getSampleStddev() / (1. - Math.pow(2., -1. / k) * gam);
    double mu = mom.getMean() - lambda * gam;

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
    protected WeibullMOMEstimator makeInstance() {
      return STATIC;
    }
  }
}
