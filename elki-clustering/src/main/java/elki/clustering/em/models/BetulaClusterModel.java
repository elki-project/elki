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
package elki.clustering.em.models;

import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.index.tree.betula.features.ClusterFeature;

/**
 * Models usable in Betula EM clustering.
 * 
 * @author Erich Schubert
 */
public interface BetulaClusterModel extends EMClusterModel<NumberVector, EMModel> {
  /**
   * Estimate the log likelihood of a clustering feature.
   * 
   * @param cf ClusteringFeature
   * @return log likelihood
   */
  double estimateLogDensity(ClusterFeature cf);

  /**
   * Process one clustering feature in the E step.
   * 
   * @param cf Clustering feature to process.
   * @param prob weight of the clustering feature.
   */
  void updateE(ClusterFeature cf, double prob);
}
