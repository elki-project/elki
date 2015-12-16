package de.lmu.ifi.dbs.elki.index;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;

/**
 * Index with support for similarity queries (e.g. precomputed similarity
 * matrixes, caches)
 *
 * @author Erich Schubert
 *
 * @apiviz.landmark
 * @apiviz.excludeSubtypes
 * @apiviz.has SimilarityQuery oneway - - «provides»
 *
 * @param <O> Object type
 */
public interface SimilarityIndex<O> extends Index {
  /**
   * Get a KNN query object for the given distance query and k.
   *
   * This function MAY return null, when the given distance is not supported!
   *
   * @param distanceFunction Distance function to use.
   * @param hints Hints for the optimizer
   * @return KNN Query object or {@code null}
   */
  SimilarityQuery<O> getSimilarityQuery(SimilarityFunction<? super O> simFunction, Object... hints);
}