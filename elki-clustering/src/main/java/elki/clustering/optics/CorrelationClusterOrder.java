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
package elki.clustering.optics;

import elki.database.datastore.WritableDBIDDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDRef;

/**
 * Cluster order entry for correlation-based OPTICS variants.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.7.0
 */
public class CorrelationClusterOrder extends ClusterOrder {
  /**
   * The correlation dimension.
   */
  protected WritableIntegerDataStore correlationValue;

  /**
   * Constructor.
   *
   * @param ids Cluster order
   * @param reachability Reachability
   * @param predecessor Predecessor (may be {@code null})
   * @param corrdim Correlation dimensionality
   */
  public CorrelationClusterOrder(ArrayModifiableDBIDs ids, WritableDoubleDataStore reachability, WritableDBIDDataStore predecessor, WritableIntegerDataStore corrdim) {
    super(ids, reachability, predecessor);
    this.correlationValue = corrdim;
  }

  /**
   * Get the correlation dimensionality.
   * 
   * @return Correlation dimensionality
   */
  public int getCorrelationValue(DBIDRef id) {
    return correlationValue.intValue(id);
  }

  /**
   * Get the Euclidean distance in the orthogonal space.
   * 
   * @return Euclidean distance
   */
  public double getEuclideanValue(DBIDRef id) {
    return reachability.doubleValue(id);
  }
}
