package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;
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

import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;

/**
 * Defines the requirements for an entry in an RdKNN-Tree node. Additionally to
 * an entry in an R*-Tree an RDkNNEntry holds the knn distance of the underlying
 * data object or RdKNN-Tree node.
 * 
 * @author Elke Achtert
 */
interface RdKNNEntry<D extends NumberDistance<D, ?>> extends SpatialEntry {
  /**
   * Returns the knn distance of this entry.
   * 
   * @return the knn distance of this entry
   */
  public D getKnnDistance();

  /**
   * Sets the knn distance of this entry.
   * 
   * @param knnDistance the knn distance to be set
   */
  public void setKnnDistance(D knnDistance);
}
