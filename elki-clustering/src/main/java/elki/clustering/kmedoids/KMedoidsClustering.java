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
package elki.clustering.kmedoids;

import elki.clustering.ClusteringAlgorithm;
import elki.data.Clustering;
import elki.data.model.MedoidModel;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;

/**
 * Interface for clustering algorithms that produce medoids.
 * <p>
 * These may be used to initialize PAMSIL clustering, for example.
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public interface KMedoidsClustering<O> extends ClusteringAlgorithm<Clustering<MedoidModel>> {
  /**
   * Run k-medoids clustering.
   *
   * @param relation relation to use
   * @return result
   */
  Clustering<MedoidModel> run(Relation<O> relation);

  /**
   * Run k-medoids clustering with a given distance query.<br>
   * Not a very elegant API, but needed for some types of nested k-medoids.
   *
   * @param relation relation to use
   * @param k Number of clusters
   * @param distQ Distance query to use
   * @return result
   */
  Clustering<MedoidModel> run(Relation<O> relation, int k, DistanceQuery<? super O> distQ);
}
