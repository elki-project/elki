package de.lmu.ifi.dbs.elki.math.statistics.distribution;


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
 * Student's t distribution.
 * 
 * @author Jan Brusis
 */
public class StudentsTDistribution implements Distribution {
  /**
   * Degrees of freedom
   */
  private final int v;

  /**
   * Constructor.
   * 
   * @param v Degrees of freedom
   */
  public StudentsTDistribution(int v) {
    this.v = v;
  }

  @Override
  public double pdf(double val) {
    return pdf(val, v);
  }

  @Override
  public double cdf(double val) {
    return cdf(val, v);
  }

  /**
   * Static version of the t distribution's PDF.
   * 
   * @param val value to evaluate
   * @param v degrees of freedom
   * @return f(val,v)
   */
  public static double pdf(double val, int v) {
    return Math.exp(GammaDistribution.logGamma((v + 1) / 2) - GammaDistribution.logGamma(v / 2)) * (1 / Math.sqrt(v * Math.PI)) * Math.pow(1 + (val * val) / v, -((v + 1) / 2));
  }

  /**
   * Static version of the CDF of the t-distribution for t > 0
   * 
   * @param val value to evaluate
   * @param v degrees of freedom
   * @return F(val, v)
   */
  public static double cdf(double val, int v) {
    double x = v / (val * val + v);
    return 1 - (0.5 * BetaDistribution.regularizedIncBeta(x, v / 2, 0.5));
  }
}