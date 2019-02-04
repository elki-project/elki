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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.birch;

import de.lmu.ifi.dbs.elki.data.NumberVector;

/**
 * BIRCH absorption criterion.
 *
 * @author Erich Schubert
 * @since 0.7.5
 * 
 * @depend - - - ClusteringFeature
 * @depend - - - NumberVector
 */
public interface BIRCHAbsorptionCriterion {
  /**
   * Quality of a CF when adding a data point
   *
   * @param f1 Clustering feature
   * @param n Data point
   * @return Quality
   */
  double squaredCriterion(ClusteringFeature f1, NumberVector n);

  /**
   * Quality when merging two CFs.
   *
   * @param f1 First clustering feature
   * @param f2 Second clustering feature
   * @return Quality
   */
  double squaredCriterion(ClusteringFeature f1, ClusteringFeature f2);
}
