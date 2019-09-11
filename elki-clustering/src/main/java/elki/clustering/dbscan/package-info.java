/**
 * DBSCAN and its generalizations.
 * <p>
 * Generalized DBSCAN is an abstraction of the original DBSCAN idea,
 * that allows the use of arbitrary "neighborhood" and "core point" predicates.
 * <p>
 * For each object, the neighborhood as defined by the "neighborhood" predicate
 * is retrieved - in original DBSCAN, this is the objects within an epsilon
 * sphere around the query object. Then the core point predicate is evaluated to
 * decide if the object is considered dense. If so, a cluster is started (or
 * extended) to include the neighbors as well.
 * <p>
 * Reference:
 * <p>
 * Jörg Sander, Martin Ester, Hans-Peter Kriegel, Xiaowei Xu<br>
 * Density-Based Clustering in Spatial Databases:
 * The Algorithm GDBSCAN and Its Applications<br>
 * Data Mining and Knowledge Discovery, 1998.
 *
 * @opt include .*elki.clustering.ClusteringAlgorithm
 * @opt include .*elki.clustering.correlation.FourC
 * @opt include .*elki.clustering.correlation.COPAC
 * @opt include .*elki.clustering.correlation.ERiC
 * @opt include .*elki.clustering.subspace.PreDeCon
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
package elki.clustering.dbscan;
