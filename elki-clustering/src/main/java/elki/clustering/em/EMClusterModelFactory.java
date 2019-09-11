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
package elki.clustering.em;

import java.util.List;

import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;

/**
 * Factory for initializing the EM models.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @navassoc - creates - EMClusterModel
 *
 * @param <V> Vector type
 */
public interface EMClusterModelFactory<V extends NumberVector, M extends MeanModel> {
  /**
   * Build the initial models
   *
   * @param database Database
   * @param relation Relation
   * @param k Number of clusters
   * @param df Distance function
   * @return Initial models
   */
  List<? extends EMClusterModel<M>> buildInitialModels(Database database, Relation<V> relation, int k, NumberVectorDistance<? super V> df);
}
