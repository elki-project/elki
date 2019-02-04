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
package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;

/**
 * Interface for Algorithms that are capable to provide a {@link Clustering
 * Clustering} as Result. in general, clustering algorithms are supposed to
 * implement the {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}-Interface. The
 * more specialized interface {@link ClusteringAlgorithm} requires an
 * implementing algorithm to provide a special result class suitable as a
 * partitioning of the database. More relaxed clustering algorithms are allowed
 * to provide a result that is a fuzzy clustering, does not partition the
 * database complete or is in any other sense a relaxed clustering result.
 *
 * @author Arthur Zimek
 * @since 0.1
 *
 * @opt operations
 * @assoc - produces - Clustering
 * @assoc - - - Model
 *
 * @param <C> Clustering type
 */
public interface ClusteringAlgorithm<C extends Clustering<? extends Model>> extends Algorithm {
  @Override
  C run(Database database);
}
