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

import java.util.List;

import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;

/**
 * Factory for initializing the EM models.
 * <p>
 * References:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees<br>
 * Information Systems
 *
 * @author Andreas Lang
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @navassoc - creates - BetulaClusterModel
 *
 * @param <M> Cluster model type
 */
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS22")
public interface BetulaClusterModelFactory<M extends BetulaClusterModel> {
  /**
   * Build the initial models.
   * 
   * @param cfs List of clustering features
   * @param k Number of clusters.
   * @param tree CF tree
   * @return Initial models
   */
  List<M> buildInitialModels(List<? extends ClusterFeature> cfs, int k, CFTree<?> tree);

  /**
   * Parameter to specify the cluster center initialization.
   */
  public static final OptionID INIT_ID = new OptionID("em.centers", "Method to choose the initial cluster centers.");
}
