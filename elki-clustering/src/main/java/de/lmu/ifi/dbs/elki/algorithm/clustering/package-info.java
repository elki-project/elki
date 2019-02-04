/**
 * Clustering algorithms
 * <p>
 * Clustering algorithms are supposed to implement the
 * {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}-Interface.
 * The more specialized interface
 * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm}
 * requires an implementing algorithm to provide a special result class suitable
 * as a partitioning of the database. More relaxed clustering algorithms are
 * allowed to provide a result that is a fuzzy clustering, does not partition
 * the database complete or is in any other sense a relaxed clustering result.
 *
 * @opt include .*elki.data.Clustering
 * @opt include .*elki.data.Model
 *
 * @see de.lmu.ifi.dbs.elki.algorithm
 */
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
