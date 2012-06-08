package de.lmu.ifi.dbs.elki.math.statistics.distribution;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

/**
 * Chi-Squared distribution (a specialization of the Gamma distribution).
 * 
 * @author Erich Schubert
 */
public class ChiSquaredDistribution extends GammaDistribution {
  /**
   * Constructor.
   * 
   * @param dof Degrees of freedom.
   */
  public ChiSquaredDistribution(double dof) {
    super(.5 * dof, .5);
  }

  /**
   * The CDF, static version.
   * 
   * @param val Value
   * @param dof Degrees of freedom.
   * @return cdf value
   */
  public static double cdf(double val, double dof) {
    return regularizedGammaP(.5 * dof, .5 * val);
  }

  /**
   * Chi-Squared distribution PDF (with 0.0 for x &lt; 0)
   * 
   * @param x query value
   * @param dof Degrees of freedom.
   * @return probability density
   */
  public static double pdf(double x, double dof) {
    if(x <= 0) {
      return 0.0;
    }
    if(x == 0) {
      return 0.0;
    }
    final double k = dof / 2;
    if(k == 1.0) {
      return Math.exp(-x * 2.0) * 2.0;
    }
    return Math.exp((k - 1.0) * Math.log(x * 2.0) - x * 2.0 - logGamma(k)) * 2.0;
  }

  /**
   * Return the quantile function for this distribution
   * 
   * Reference:
   * <p>
   * Algorithm AS 91: The percentage points of the $\chi$^2 distribution<br />
   * D.J. Best, D. E. Roberts<br />
   * Journal of the Royal Statistical Society. Series C (Applied Statistics)
   * </p>
   * 
   * @param x Quantile
   * @param dof Degrees of freedom
   * @return quantile position
   */
  @Reference(title = "Algorithm AS 91: The percentage points of the $\\chi^2$ distribution", authors = "D.J. Best, D. E. Roberts", booktitle = "Journal of the Royal Statistical Society. Series C (Applied Statistics)")
  public static double quantile(double x, double dof) {
    return GammaDistribution.quantile(x, .5 * dof, .5);
  }
}