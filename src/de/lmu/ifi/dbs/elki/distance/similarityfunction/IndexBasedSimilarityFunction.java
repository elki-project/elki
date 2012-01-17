package de.lmu.ifi.dbs.elki.distance.similarityfunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.Index;

/**
 * Interface for preprocessor/index based similarity functions.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 *
 * @param <O> Object type
 * @param <D> Distance type
 */
public interface IndexBasedSimilarityFunction<O, D extends Distance<D>> extends SimilarityFunction<O, D> {
  /**
   * Preprocess the database to get the actual distance function.
   * 
   * @param database
   * @return Actual distance query.
   */
  @Override
  public abstract <T extends O> Instance<T, ?, D> instantiate(Relation<T> database);

  /**
   * Instance interface for index/preprocessor based distance functions.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Object type
   * @param <D> Distance type
   */
  public static interface Instance<T, I extends Index, D extends Distance<D>> extends SimilarityQuery<T, D> {
    /**
     * Get the index used.
     * 
     * @return the index used
     */
    public I getIndex();
  }
}