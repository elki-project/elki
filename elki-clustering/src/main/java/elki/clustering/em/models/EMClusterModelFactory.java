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

import elki.data.model.Model;
import elki.database.relation.Relation;
import elki.utilities.optionhandling.OptionID;

/**
 * Factory for initializing the EM models.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @navassoc - creates - EMClusterModel
 *
 * @param <O> Input object type
 * @param <M> Cluster model type
 */
public interface EMClusterModelFactory<O, M extends Model> {
  /**
   * Build the initial models
   *
   * @param relation Relation
   * @param k Number of clusters
   * @return Initial models
   */
  List<? extends EMClusterModel<O, M>> buildInitialModels(Relation<? extends O> relation, int k);

  /**
   * Parameter to specify the cluster center initialization.
   */
  static final OptionID INIT_ID = new OptionID("em.centers", "Method to choose the initial cluster centers.");
}
