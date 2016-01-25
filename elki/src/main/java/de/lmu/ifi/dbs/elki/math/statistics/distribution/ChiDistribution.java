package de.lmu.ifi.dbs.elki.math.statistics.distribution;

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

import java.util.Random;

import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.exceptions.NotImplementedException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Chi distribution.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @apiviz.composedOf ChiSquaredDistribution
 */
public class ChiDistribution extends AbstractDistribution {
  /**
   * Degrees of freedom. Usually integer.
   */
  private double dof;

  /**
   * Chi squared distribution (for random generation)
   */
  private ChiSquaredDistribution chisq;

  /**
   * Constructor.
   * 
   * @param dof Degrees of freedom. Usually integer.
   */
  public ChiDistribution(double dof) {
    this(dof, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param dof Degrees of freedom. Usually integer.
   * @param random Random number generator.
   */
  public ChiDistribution(double dof, Random random) {
    super(random);
    this.dof = dof;
    this.chisq = new ChiSquaredDistribution(dof, random);
  }

  /**
   * Constructor.
   * 
   * @param dof Degrees of freedom. Usually integer.
   * @param random Random number generator.
   */
  public ChiDistribution(double dof, RandomFactory random) {
    super(random);
    this.dof = dof;
    this.chisq = new ChiSquaredDistribution(dof, random);
  }

  @Override
  public double nextRandom() {
    return Math.sqrt(chisq.nextRandom());
  }

  @Override
  public double pdf(double val) {
    return pdf(val, dof);
  }

  /**
   * PDF function
   * 
   * @param val Value
   * @param dof Degrees of freedom
   * @return Pdf value
   */
  public static double pdf(double val, double dof) {
    if (val < 0) {
      return 0.0;
    }
    return Math.sqrt(ChiSquaredDistribution.pdf(val, dof));
  }

  @Override
  public double cdf(double val) {
    return cdf(val, dof);
  }

  /**
   * Cumulative density function.
   * 
   * @param val Value
   * @param dof Degrees of freedom.
   * @return CDF value
   */
  public static double cdf(double val, double dof) {
    return GammaDistribution.regularizedGammaP(dof * .5, val * val * .5);
  }

  // FIXME: implement!
  @Override
  public double quantile(double val) {
    throw new NotImplementedException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  @Override
  public String toString() {
    return "ChiDistribution(dof=" + dof + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /** Parameters. */
    double dof;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter dofP = new DoubleParameter(ChiSquaredDistribution.Parameterizer.DOF_ID);
      if (config.grab(dofP)) {
        dof = dofP.doubleValue();
      }
    }

    @Override
    protected ChiDistribution makeInstance() {
      return new ChiDistribution(dof, rnd);
    }
  }
}
