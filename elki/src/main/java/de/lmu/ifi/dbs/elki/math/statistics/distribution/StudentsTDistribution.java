package de.lmu.ifi.dbs.elki.math.statistics.distribution;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.util.Random;

import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.exceptions.NotImplementedException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Student's t distribution.
 * 
 * FIXME: add quantile and random function!
 * 
 * @author Jan Brusis
 */
public class StudentsTDistribution extends AbstractDistribution {
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
    this(v, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param v Degrees of freedom
   * @param random Random generator
   */
  public StudentsTDistribution(int v, Random random) {
    super(random);
    this.v = v;
  }

  /**
   * Constructor.
   * 
   * @param v Degrees of freedom
   * @param random Random generator
   */
  public StudentsTDistribution(int v, RandomFactory random) {
    super(random);
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

  // FIXME: implement!
  @Override
  public double quantile(double val) {
    throw new NotImplementedException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  // FIXME: implement!
  @Override
  public double nextRandom() {
    throw new NotImplementedException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  /**
   * Static version of the t distribution's PDF.
   * 
   * @param val value to evaluate
   * @param v degrees of freedom
   * @return f(val,v)
   */
  public static double pdf(double val, int v) {
    // TODO: improve precision by computing "exp" last?
    return Math.exp(GammaDistribution.logGamma((v + 1) * .5) - GammaDistribution.logGamma(v * .5)) * (1 / Math.sqrt(v * Math.PI)) * Math.pow(1 + (val * val) / v, -((v + 1) * .5));
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
    return 1 - (0.5 * BetaDistribution.regularizedIncBeta(x, v * .5, 0.5));
  }

  @Override
  public String toString() {
    return "StudentsTDistribution(v=" + v + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /**
     * Degrees of freedom.
     */
    public static final OptionID NU_ID = new OptionID("distribution.studentst.nu", "Degrees of freedom.");

    /** Parameters. */
    int nu;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter nuP = new IntParameter(NU_ID);
      if (config.grab(nuP)) {
        nu = nuP.intValue();
      }
    }

    @Override
    protected StudentsTDistribution makeInstance() {
      return new StudentsTDistribution(nu, rnd);
    }
  }
}
