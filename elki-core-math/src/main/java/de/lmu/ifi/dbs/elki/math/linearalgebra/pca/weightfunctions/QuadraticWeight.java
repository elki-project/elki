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
 * Quadratic weight function, scaled using the maximum to reach 0.1 at that
 * point.
 * <p>
 * 1.0 - 0.9 * (distance/max)²
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public final class QuadraticWeight implements WeightFunction {
  /**
   * Evaluate quadratic weight. stddev is ignored.
   */
  @Override
  public double getWeight(double distance, double max, double stddev) {
    if(max <= 0) {
      return 1.0;
    }
    double relativedistance = distance / max;
    return 1.0 - 0.9 * relativedistance * relativedistance;
  }
}