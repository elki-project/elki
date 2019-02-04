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
package de.lmu.ifi.dbs.elki.index.preprocessed.localpca;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;

/**
 * Interface for an index providing local PCA results.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <NV> Vector type
 */
public interface FilteredLocalPCAIndex<NV extends NumberVector> extends Index {
  /**
   * Get the precomputed local PCA for a particular object ID.
   * 
   * @param objid Object ID
   * @return Matrix
   */
  PCAFilteredResult getLocalProjection(DBIDRef objid);

  /**
   * Factory interface
   *
   * @author Erich Schubert
   *
   * @stereotype factory
   * @navassoc - create - FilteredLocalPCAIndex
   *
   * @param <NV> Vector type
   */
  interface Factory<NV extends NumberVector> extends IndexFactory<NV> {
    /**
     * Instantiate the index for a given database.
     *
     * @param relation Relation to use
     *
     * @return Index
     */
    @Override
    FilteredLocalPCAIndex<NV> instantiate(Relation<NV> relation);
  }
}
