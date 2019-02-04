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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import de.lmu.ifi.dbs.elki.database.datastore.DBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Extended pointer representation useful for HDBSCAN. In addition to the parent
 * object and the distance to the parent, it also includes the core distance,
 * which is a density estimation.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class PointerDensityHierarchyRepresentationResult extends PointerHierarchyRepresentationResult {
  /**
   * Core distance.
   */
  DoubleDataStore coreDistance;

  /**
   * Constructor.
   * 
   * @param ids IDs processed.
   * @param parent Parent pointer.
   * @param parentDistance Distance to parent.
   * @param isSquared Flag to indicate squared distances
   * @param coreDistance Core distances.
   */
  public PointerDensityHierarchyRepresentationResult(DBIDs ids, DBIDDataStore parent, DoubleDataStore parentDistance, boolean isSquared, DoubleDataStore coreDistance) {
    super(ids, parent, parentDistance, isSquared);
    this.coreDistance = coreDistance;
  }

  /**
   * Get the core distance.
   * 
   * @return Core distances.
   */
  public DoubleDataStore getCoreDistanceStore() {
    return coreDistance;
  }
}
