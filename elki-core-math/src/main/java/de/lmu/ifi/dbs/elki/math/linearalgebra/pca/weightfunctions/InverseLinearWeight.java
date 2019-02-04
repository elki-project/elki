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
package de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions;

/**
 * Inverse Linear Weight Function.
 * 
 * This weight is not particularly reasonable. Instead it serves the purpose of
 * testing the effects of a badly chosen weight function.
 * 
 * This function has increasing weight, from 0.1 to 1.0 at distance == max.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public final class InverseLinearWeight implements WeightFunction {
  /**
   * Linear increasing weight, from 0.1 to 1.0
   * 
   * NOTE: increasing weights are non-standard, and mostly for testing
   */
  @Override
  public double getWeight(double distance, double max, double stddev) {
    if(max <= 0) {
      return 0.1;
    }
    double relativedistance = distance / max;
    return .1 + relativedistance * .9;
  }
}