/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
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
package elki.clustering.hierarchical.betula.initialization;

import elki.clustering.hierarchical.betula.CFInterface;
import elki.data.NumberVector;

/**
 * Interface for distance calculation on any of the BETULA cluster features.
 *
 * @author Andreas Lang
 */
public interface CFIDistance {
  /**
   * Distance of a vector to a clustering feature.
   *
   * @param v Vector
   * @param cf Clustering Feature
   * @return Distance
   */
  double squaredDistance(NumberVector v, CFInterface cf);

  /**
   * Distance of a vector to a clustering feature.
   *
   * @param v double Array
   * @param cf Clustering Feature
   * @return Distance
   */
  double squaredDistance(double[] v, CFInterface cf);

  /**
   * Distance between two clustering features.
   *
   * @param c1 First clustering feature
   * @param c2 Second clustering feature
   * @return Distance
   */
  double squaredDistance(CFInterface c1, CFInterface c2);
}
