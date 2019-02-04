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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;

/**
 * Interface for initializing K-Medoids. In contrast to k-means initializers,
 * this initialization will only return members of the original data set.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <V> Object type
 */
public interface KMedoidsInitialization<V> {
  /**
   * Choose initial means
   * 
   * @param k Parameter k
   * @param ids Candidate IDs.
   * @param distanceFunction Distance function
   * @return List of chosen means for k-means
   */
  DBIDs chooseInitialMedoids(int k, DBIDs ids, DistanceQuery<? super V> distanceFunction);
}