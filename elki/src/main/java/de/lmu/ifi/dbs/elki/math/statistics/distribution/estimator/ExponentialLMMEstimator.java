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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.ExponentialDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate the parameters of a Gamma Distribution, using the methods of
 * L-Moments (LMM).
 * 
 * Reference:
 * <p>
 * J. R. M. Hosking<br />
 * Fortran routines for use with the method of L-moments Version 3.03<br />
 * IBM Research.
 * </p>
 * 
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.has ExponentialDistribution
 */
@Reference(authors = "J.R.M. Hosking", title = "Fortran routines for use with the method of L-moments Version 3.03", booktitle = "IBM Research Technical Report")
public class ExponentialLMMEstimator extends AbstractLMMEstimator<ExponentialDistribution> {
  /**
   * Static instance.
   */
  public static final ExponentialLMMEstimator STATIC = new ExponentialLMMEstimator();

  /**
   * Constructor. Private: use static instance.
   */
  private ExponentialLMMEstimator() {
    super();
  }

  @Override
  public int getNumMoments() {
    return 2;
  }

  @Override
  public ExponentialDistribution estimateFromLMoments(double[] xmom) {
    double scale = 2. * xmom[1];
    if (!(scale > 0.)) {
      throw new ArithmeticException("Data with non-positive scale cannot be exponential distributed.");
    }
    return new ExponentialDistribution(1. / scale, xmom[0] - scale);
  }

  @Override
  public Class<? super ExponentialDistribution> getDistributionClass() {
    return ExponentialDistribution.class;
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
    protected ExponentialLMMEstimator makeInstance() {
      return STATIC;
    }
  }
}
