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
package elki.clustering.em.models;

import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.documentation.Reference;

/**
 * Models usable in Betula EM clustering.
 * <p>
 * References:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees<br>
 * Information Systems
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS22")
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
