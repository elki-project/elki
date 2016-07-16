package de.lmu.ifi.dbs.elki.distance.similarityfunction;

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

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * Interface SimilarityFunction describes the requirements of any similarity
 * function.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @apiviz.landmark
 * 
 * @param <O> object type
 */
public interface SimilarityFunction<O> {
  /**
   * Is this function symmetric?
   * 
   * @return {@code true} when symmetric
   */
  boolean isSymmetric();

  /**
   * Get the input data type of the function.
   */
  TypeInformation getInputTypeRestriction();

  /**
   * Instantiate with a representation to get the actual similarity query.
   * 
   * @param relation Representation to use
   * @return Actual distance query.
   */
  public <T extends O> SimilarityQuery<T> instantiate(Relation<T> relation);
}