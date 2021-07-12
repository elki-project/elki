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
package elki.clustering.hierarchical.betula;

import elki.data.NumberVector;

/**
 * Distance function for BIRCH clustering.
 *
 * For performance we (usually) use squared distances.
 *
 * The exception to this rule is Manhattan.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @assoc - - - ClusteringFeature
 * @assoc - - - NumberVector
 */
public interface CFDistance<L extends CFInterface> {
  /**
   * Distance of a vector to a clustering feature.
   *
   * @param v Vector
   * @param cf Clustering Feature
   * @return Distance
   */
  double squaredDistance(NumberVector v, L cf);

  /**
   * Distance between two clustering features.
   *
   * @param c1 First clustering feature
   * @param c2 Second clustering feature
   * @return Distance
   */
  double squaredDistance(L c1, L c2);

  /**
   * Initialization for self measure for new Combinatorial clustering Methods
   * (Podani 89)
   * 
   * @param cf Clusterin Feature
   * @return internal measure
   */
  default double matSelfInit(L cf) {
    return 0;
  }
}
