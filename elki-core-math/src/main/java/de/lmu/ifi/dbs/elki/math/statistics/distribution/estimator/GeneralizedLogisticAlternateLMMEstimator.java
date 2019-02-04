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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.GeneralizedLogisticAlternateDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Estimate the parameters of a Generalized Logistic Distribution, using the
 * methods of L-Moments (LMM).
 * <p>
 * Reference:
 * <p>
 * J. R. M. Hosking<br>
 * Fortran routines for use with the method of L-moments Version 3.03<br>
 * IBM Research.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @has - - - GeneralizedLogisticAlternateDistribution
 */
@Reference(authors = "J. R. M. Hosking", //
    title = "Fortran routines for use with the method of L-moments Version 3.03", //
    booktitle = "IBM Research Technical Report", //
    bibkey = "tr/ibm/Hosking00")
public class GeneralizedLogisticAlternateLMMEstimator implements LMMDistributionEstimator<GeneralizedLogisticAlternateDistribution> {
  /**
   * Static instance.
   */
  public static final GeneralizedLogisticAlternateLMMEstimator STATIC = new GeneralizedLogisticAlternateLMMEstimator();

  /**
   * Constructor. Private: use static instance.
   */
  private GeneralizedLogisticAlternateLMMEstimator() {
    super();
  }

  @Override
  public int getNumMoments() {
    return 3;
  }

  @Override
  public GeneralizedLogisticAlternateDistribution estimateFromLMoments(double[] xmom) {
    double shape = -xmom[2];
    if(!(shape >= -1 && shape <= 1)) {
      throw new ArithmeticException("Invalid moment estimation.");
    }
    if(Math.abs(shape) < 1e-6) {
      // Effectively zero, so non-generalized.
      return new GeneralizedLogisticAlternateDistribution(xmom[0], xmom[1], 0.);
    }
    double tmp = shape * Math.PI / FastMath.sin(shape * Math.PI);
    double scale = xmom[1] / tmp;
    double location = xmom[0] - scale * (1. - tmp) / shape;
    return new GeneralizedLogisticAlternateDistribution(location, scale, shape);
  }

  @Override
  public Class<? super GeneralizedLogisticAlternateDistribution> getDistributionClass() {
    return GeneralizedLogisticAlternateDistribution.class;
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
    protected GeneralizedLogisticAlternateLMMEstimator makeInstance() {
      return STATIC;
    }
  }
}
