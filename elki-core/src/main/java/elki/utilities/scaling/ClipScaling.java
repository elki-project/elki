/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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

/**
 * Scale implementing a simple clipping. Values less than the specified minimum
 * will be set to the minimum, values larger than the maximum will be set to the
 * maximum.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class ClipScaling implements StaticScalingFunction {
  /**
   * Parameter to specify a fixed minimum to use.
   */
  public static final OptionID MIN_ID = new OptionID("clipscale.min", "Minimum value to allow.");

  /**
   * Parameter to specify the maximum value
   */
  public static final OptionID MAX_ID = new OptionID("clipscale.max", "Maximum value to allow.");

  /**
   * Field storing the minimum to use
   */
  private double min = Double.NaN;

  /**
   * Field storing the maximum to use
   */
  private double max = Double.NaN;

  /**
   * Constructor.
   *
   * @param min Minimum, may be null
   * @param max Maximum, may be null
   */
  public ClipScaling(double min, double max) {
    super();
    this.min = min;
    this.max = max;
  }

  @Override
  public double getScaled(double value) {
    return value < min ? min // min=NaN is false, ok
        : value > max ? max // max=NaN is false, ok
            : value;
  }

  @Override
  public double getMin() {
    return (min == min /* not NaN */) ? min : Double.NEGATIVE_INFINITY;
  }

  @Override
  public double getMax() {
    return (max == max /* not NaN */) ? max : Double.POSITIVE_INFINITY;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Minimum value
     */
    protected double min = Double.NaN;

    /**
     * Maximum value
     */
    protected double max = Double.NaN;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(MIN_ID) //
          .setOptional(true) //
          .grab(config, x -> min = x);
      new DoubleParameter(MAX_ID) //
          .setOptional(true) //
          .grab(config, x -> max = x);
    }

    @Override
    public ClipScaling make() {
      return new ClipScaling(min, max);
    }
  }
}
