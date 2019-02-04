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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.LogNormalDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Alternate estimate the parameters of a log Gamma Distribution, using the
 * methods of L-Moments (LMM) for the Generalized Normal Distribution.
 * <p>
 * Reference:
 * <p>
 * D. Bílková<br>
 * Lognormal distribution and using L-moment method for estimating its
 * parameters<br>
 * Int. Journal of Mathematical Models and Methods in Applied Sciences (NAUN)
 * <p>
 * See also {@link LogNormalLMMEstimator} for a similar estimator, based on the
 * generalized normal distribution, as used by Hosking.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @has - - - LogNormalDistribution
 */
@Reference(authors = "D. Bílková", //
    title = "Lognormal distribution and using L-moment method for estimating its parameters", //
    booktitle = "Int. Journal of Mathematical Models and Methods in Applied Sciences (NAUN)", //
    url = "http://www.naun.org/multimedia/NAUN/m3as/17-079.pdf", //
    bibkey = "journals/naun/Bilkova12")
public class LogNormalBilkovaLMMEstimator implements LMMDistributionEstimator<LogNormalDistribution> {
  /**
   * Static instance.
   */
  public static final LogNormalBilkovaLMMEstimator STATIC = new LogNormalBilkovaLMMEstimator();

  /**
   * Scaling constant.
   */
  private static final double SQRT8_3 = Math.sqrt(8. / 3.);

  /**
   * Constructor. Private: use static instance.
   */
  private LogNormalBilkovaLMMEstimator() {
    super();
  }

  @Override
  public int getNumMoments() {
    return 3;
  }

  @Override
  public LogNormalDistribution estimateFromLMoments(double[] xmom) {
    if(!(xmom[1] > 0.) || !(Math.abs(xmom[2]) < 1.0) || !(xmom[2] > 0.)) {
      throw new ArithmeticException("L-Moments invalid");
    }
    final double z = SQRT8_3 * NormalDistribution.standardNormalQuantile(.5 * (1. + xmom[2]));
    final double z2 = z * z;
    final double sigma = 0.999281 * z - 0.006118 * z * z2 + 0.000127 * z * z2 * z2;
    final double sigmasqhalf = sigma * sigma * .5;
    final double logmu = FastMath.log(xmom[1] / NormalDistribution.erf(.5 * sigma)) - sigmasqhalf;
    return new LogNormalDistribution(logmu, Math.max(sigma, Double.MIN_NORMAL), xmom[0] - FastMath.exp(logmu + sigmasqhalf));
  }

  @Override
  public Class<? super LogNormalDistribution> getDistributionClass() {
    return LogNormalDistribution.class;
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
    protected LogNormalBilkovaLMMEstimator makeInstance() {
      return STATIC;
    }
  }
}
