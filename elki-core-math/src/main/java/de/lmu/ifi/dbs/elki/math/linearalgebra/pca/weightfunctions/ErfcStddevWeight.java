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

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;

/**
 * Gaussian Error Function Weight function, scaled using stddev. This probably
 * is the most statistically sound weight.
 * 
 * erfc(1 / sqrt(2) * distance / stddev)
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public final class ErfcStddevWeight implements WeightFunction {
  /**
   * Return Erfc weight, scaled by standard deviation. max is ignored.
   */
  @Override
  public double getWeight(double distance, double max, double stddev) {
    if(stddev <= 0) {
      return 1;
    }
    return NormalDistribution.erfc(MathUtil.SQRTHALF * distance / stddev);
  }
}