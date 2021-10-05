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
package elki.clustering.kmeans.initialization.betula;

import elki.index.tree.betula.features.ClusterFeature;

/**
 * Interface for distance calculation on any of the BETULA cluster features.
 *
 * @author Andreas Lang
 */
public interface CFIDistance {
  /**
   * Distance between two clustering features.
   *
   * @param c1 First clustering feature
   * @param c2 Second clustering feature
   * @return Distance
   */
  double squaredDistance(ClusterFeature c1, ClusterFeature c2);
}
