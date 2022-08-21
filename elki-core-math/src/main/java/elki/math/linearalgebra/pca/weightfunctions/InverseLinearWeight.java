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
package elki.math.linearalgebra.pca.weightfunctions;

/**
 * Inverse linear weight function using \(.1+0.9\frac{\text{distance}}{\max}\).
 * <p>
 * This weight is not particularly reasonable. Instead it serves the purpose of
 * testing the effects of a badly chosen weight function.
 * <p>
 * This function has increasing weight, from 0.1 to 1.0 when the distance equals
 * the maximum.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public final class InverseLinearWeight implements WeightFunction {
  /**
   * Linear increasing weight, from 0.1 to 1.0
   * <p>
   * NOTE: increasing weights are non-standard, and mostly for testing
   */
  @Override
  public double getWeight(double distance, double max, double stddev) {
    return max <= 0 ? 0.1 : .1 + (distance / max) * .9;
  }
}
