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

import net.jafama.FastMath;

/**
 * Scaling function to invert values by computing -1 * Math.log(x)
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class MinusLogScaling implements StaticScalingFunction {
  @Override
  public double getScaled(double value) {
    return -FastMath.log(value);
  }

  @Override
  public double getMin() {
    return Double.NEGATIVE_INFINITY;
  }

  @Override
  public double getMax() {
    return Double.POSITIVE_INFINITY;
  }
}