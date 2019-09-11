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
package elki.utilities.scaling;

import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import net.jafama.FastMath;

/**
 * Non-linear scaling function using a Gamma curve.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * @since 0.3
 */
public class GammaScaling implements StaticScalingFunction {
  /**
   * Gamma value.
   */
  private double gamma;

  /**
   * Constructor without options.
   */
  public GammaScaling() {
    this(1.0);
  }

  /**
   * Constructor with Gamma value.
   * 
   * @param gamma Gamma value.
   */
  public GammaScaling(double gamma) {
    this.gamma = gamma;
  }

  @Override
  public double getScaled(double d) {
    return FastMath.pow(d, gamma);
  }

  @Override
  public double getMin() {
    return Double.NEGATIVE_INFINITY;
  }

  @Override
  public double getMax() {
    return Double.POSITIVE_INFINITY;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * OptionID for the gamma value.
     */
    public static final OptionID GAMMA_ID = new OptionID("scaling.gamma", "Gamma value for scaling.");

    double gamma = 1.0;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(GAMMA_ID) //
          .grab(config, x -> gamma = x);
    }

    @Override
    public GammaScaling make() {
      return new GammaScaling(gamma);
    }
  }
}