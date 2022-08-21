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
 * Linear weight function, scaled using the maximum such that it goes from 1.0
 * to 0.1 using: \( 1 - 0.9 \frac{\text{distance}}{\max} \)
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public final class LinearWeight implements WeightFunction {
  /**
   * Linear decreasing weight, from 1.0 to 0.1. Stddev is ignored.
   */
  @Override
  public double getWeight(double distance, double max, double stddev) {
    return max <= 0 ? 1. : 1 - (distance / max) * .9;
  }
}
