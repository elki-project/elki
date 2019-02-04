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
 * WeightFunction interface that allows the use of various distance-based weight
 * functions. In addition to the distance parameter, the maximum distance and
 * standard deviation are also given, to allow distance functions to be
 * normalized according to the maximum or standard deviation.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public interface WeightFunction {
  /**
   * Evaluate weight function with given parameters.
   * 
   * Note that usually implementations will ignore either max or stddev.
   * 
   * @param distance distance of the query point
   * @param max maximum distance of all included points
   * @param stddev standard deviation (i.e. quadratic mean / RMS) of the
   *        included points
   * @return weight for the query point
   */
  double getWeight(double distance, double max, double stddev);
}
