package de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Gaussian Weight function, scaled such using standard deviation
 * 
 * factor * exp(-.5 * (distance/stddev)^2)
 * 
 * with factor being 1 / sqrt(2 * PI)
 * 
 * @author Erich Schubert
 */
public final class GaussStddevWeight implements WeightFunction {
  /**
   * Constant scaling factor of Gaussian distribution.
   * 
   * In fact, in most use cases we could leave this away.
   */
  private final static double scaling = 1 / MathUtil.SQRTTWOPI;

  /**
   * Get Gaussian Weight using standard deviation for scaling. max is ignored.
   */
  @Override
  public double getWeight(double distance, @SuppressWarnings("unused") double max, double stddev) {
    if(stddev <= 0) {
      return 1;
    }
    double normdistance = distance / stddev;
    return scaling * java.lang.Math.exp(-.5 * normdistance * normdistance) / stddev;
  }
}
