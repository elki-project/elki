package de.lmu.ifi.dbs.elki.math.statistics.distribution;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Pseudo distribution, that has a unique constant value.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class ConstantDistribution implements Distribution {
  /**
   * The constant
   */
  final double c;

  /**
   * Constructor.
   * 
   * @param c Constant
   */
  public ConstantDistribution(double c) {
    super();
    this.c = c;
  }

  @Override
  public double nextRandom() {
    return c;
  }

  @Override
  public double pdf(double val) {
    return (val == c) ? 1 : 0;
  }
  
  @Override
  public double logpdf(double val) {
    return (val == c) ? 0 : Double.NEGATIVE_INFINITY;
  }

  @Override
  public double cdf(double val) {
    return (c < val) ? 0. : (c > val) ? 1. : .5;
  }

  @Override
  public double quantile(double val) {
    return c;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Constant value parameter
     */
    public static final OptionID CONSTANT_ID = new OptionID("distribution.constant", "Constant value.");

    /** Parameters. */
    double constant;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter constP = new DoubleParameter(CONSTANT_ID);
      if (config.grab(constP)) {
        constant = constP.doubleValue();
      }
    }

    @Override
    protected ConstantDistribution makeInstance() {
      return new ConstantDistribution(constant);
    }
  }
}
