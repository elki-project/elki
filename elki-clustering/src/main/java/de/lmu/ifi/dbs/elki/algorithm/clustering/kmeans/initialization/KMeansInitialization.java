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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;

/**
 * Interface for initializing K-Means
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @opt nodefillcolor LemonChiffon
 */
public interface KMeansInitialization {
  /**
   * Choose initial means
   *
   * @param database Database context
   * @param relation Relation
   * @param k Parameter k
   * @param distanceFunction Distance function
   * @return List of chosen means for k-means
   */
  double[][] chooseInitialMeans(Database database, Relation<? extends NumberVector> relation, int k, NumberVectorDistanceFunction<?> distanceFunction);
}
