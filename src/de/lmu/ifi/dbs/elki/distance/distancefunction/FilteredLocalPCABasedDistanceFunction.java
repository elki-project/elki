package de.lmu.ifi.dbs.elki.distance.distancefunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.preprocessed.localpca.FilteredLocalPCAIndex;

/**
 * Interface for local PCA based preprocessors.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Instance
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public interface FilteredLocalPCABasedDistanceFunction<O extends NumberVector<?, ?>, P extends FilteredLocalPCAIndex<? super O>, D extends Distance<D>> extends IndexBasedDistanceFunction<O, D> {
  /**
   * Instantiate with a database to get the actual distance query.
   * 
   * @param database
   * @return Actual distance query.
   */
  @Override
  public <T extends O> Instance<T, ?, D> instantiate(Relation<T> database);

  /**
   * Instance produced by the distance function.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Database object type
   * @param <I> Index type
   * @param <D> Distance type
   */
  public static interface Instance<T extends NumberVector<?, ?>, I extends Index, D extends Distance<D>> extends IndexBasedDistanceFunction.Instance<T, I, D> {
    // No additional restrictions
  }
}