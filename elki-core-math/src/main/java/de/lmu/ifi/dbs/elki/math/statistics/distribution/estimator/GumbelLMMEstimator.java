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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.GumbelDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate the parameters of a Gumbel Distribution, using the methods of
 * L-Moments (LMM).
 * <p>
 * Reference:
 * <p>
 * J. R. M. Hosking<br>
 * Fortran routines for use with the method of L-moments Version 3.03<br>
 * IBM Research
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @has - - - GumbelDistribution
 */
@Reference(authors = "J. R. M. Hosking", //
    title = "Fortran routines for use with the method of L-moments Version 3.03", //
    booktitle = "IBM Research Technical Report", //
    bibkey = "tr/ibm/Hosking00")
public class GumbelLMMEstimator implements LMMDistributionEstimator<GumbelDistribution> {
  /**
   * Static instance.
   */
  public static final GumbelLMMEstimator STATIC = new GumbelLMMEstimator();

  /**
   * Constructor. Private: use static instance.
   */
  private GumbelLMMEstimator() {
    super();
  }

  @Override
  public int getNumMoments() {
    return 2;
  }

  @Override
  public GumbelDistribution estimateFromLMoments(double[] xmom) {
    double scale = xmom[1] * MathUtil.ONE_BY_LOG2;
    // TODO: Apparently we use a different parameterization?
    return new GumbelDistribution(xmom[0] - scale / Math.E, scale);
  }

  @Override
  public Class<? super GumbelDistribution> getDistributionClass() {
    return GumbelDistribution.class;
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
    protected GumbelLMMEstimator makeInstance() {
      return STATIC;
    }
  }
}
