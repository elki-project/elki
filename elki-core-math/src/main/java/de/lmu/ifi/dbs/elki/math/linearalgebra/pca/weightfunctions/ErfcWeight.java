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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;

/**
 * Gaussian Error Function Weight function, scaled such that the result it 0.1
 * at distance == max
 * 
 * erfc(1.1630871536766736 * distance / max)
 * 
 * The value of 1.1630871536766736 is erfcinv(0.1), to achieve the intended
 * scaling.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public final class ErfcWeight implements WeightFunction {
  /**
   * Get Erfc Weight, using distance / max. stddev is ignored.
   */
  @Override
  public double getWeight(double distance, double max, double stddev) {
    if(max <= 0) {
      return 1.0;
    }
    double relativedistance = distance / max;
    // the scaling was picked such that getWeight(a,a,0) is 0.1
    // since erfc(1.1630871536766736) == 1.0
    return NormalDistribution.erfc(1.1630871536766736 * relativedistance);
  }
}
