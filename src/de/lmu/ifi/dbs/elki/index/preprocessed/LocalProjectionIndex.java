package de.lmu.ifi.dbs.elki.index.preprocessed;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.math.linearalgebra.ProjectionResult;

/**
 * Abstract index interface for local projections
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has ProjectionResult
 * 
 * @param <V> Vector type
 * @param <P> Projection result type
 */
public interface LocalProjectionIndex<V extends NumberVector<?, ?>, P extends ProjectionResult> extends Index {
  /**
   * Get the precomputed local projection for a particular object ID.
   * 
   * @param objid Object ID
   * @return local projection
   */
  public P getLocalProjection(DBID objid);

  /**
   * Factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.has LocalProjectionIndex oneway - - «create»
   * 
   * @param <V> Vector type
   * @param <I> Index type
   */
  public static interface Factory<V extends NumberVector<?, ?>, I extends LocalProjectionIndex<V, ?>> extends IndexFactory<V, I> {
    /**
     * Instantiate the index for a given database.
     * 
     * @param relation Relation to use
     * 
     * @return Index
     */
    @Override
    public I instantiate(Relation<V> relation);
  }
}