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
package de.lmu.ifi.dbs.elki.algorithm.projection;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * Interface for computing an affinity matrix.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <O> Input relation type
 *
 * @has - - - AffinityMatrix
 */
public interface AffinityMatrixBuilder<O> {
  /**
   * Compute the affinity matrix.
   *
   * @param relation Data relation
   * @param initialScale initial scale
   * @return Affinity matrix
   * @param <T> Relation type
   */
  <T extends O> AffinityMatrix computeAffinityMatrix(Relation<T> relation, double initialScale);

  /**
   * Supported input data.
   *
   * @return Input data type information.
   */
  TypeInformation getInputTypeRestriction();

}