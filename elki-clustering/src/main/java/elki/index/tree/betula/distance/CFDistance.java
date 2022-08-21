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
package elki.index.tree.betula.distance;

import elki.data.NumberVector;
import elki.index.tree.betula.features.ClusterFeature;

/**
 * Distance function for BIRCH clustering.
 * <p>
 * For performance we (usually, except Manhattan) use squared distances.
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @assoc - - - ClusteringFeature
 * @assoc - - - NumberVector
 */
public interface CFDistance {
  /**
   * Distance of a vector to a clustering feature.
   *
   * @param v Vector
   * @param cf Clustering Feature
   * @return Distance
   */
  double squaredDistance(NumberVector v, ClusterFeature cf);

  /**
   * Distance between two clustering features.
   *
   * @param c1 First clustering feature
   * @param c2 Second clustering feature
   * @return Distance
   */
  double squaredDistance(ClusterFeature c1, ClusterFeature c2);

  /**
   * Initialization for self measure for new Combinatorial clustering Methods
   * (Podani 1989)
   * 
   * @param cf Clustering Feature
   * @return internal measure
   */
  default double matSelfInit(ClusterFeature cf) {
    return 0;
  }
}
