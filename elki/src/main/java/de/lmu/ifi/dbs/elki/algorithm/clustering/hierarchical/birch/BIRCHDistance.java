package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.birch;

/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2017
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

import de.lmu.ifi.dbs.elki.data.NumberVector;

/**
 * Distance function for BIRCH clustering.
 * 
 * @author Erich Schubert
 */
public interface BIRCHDistance {
  /**
   * Distance of a vector to a clustering feature.
   * 
   * @param v Vector
   * @param cf Clustering Feature
   * @return Distance
   */
  double distance(NumberVector v, ClusteringFeature cf);

  /**
   * Distance between two clustering features.
   * 
   * @param c1 First clustering feature
   * @param c2 Second clustering feature
   * @return Distance
   */
  double distance(ClusteringFeature c1, ClusteringFeature c2);
}
