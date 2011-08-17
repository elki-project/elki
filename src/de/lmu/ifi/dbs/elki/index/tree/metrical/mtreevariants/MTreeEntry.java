package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants;
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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.Entry;

/**
 * Defines the requirements for an entry in an M-Tree node.
 * 
 * @author Elke Achtert
 * @param <D> the type of Distance used in the M-Tree
 */
public interface MTreeEntry<D extends Distance<D>> extends Entry {
  /**
   * Returns the id of the underlying database object of this entry, if this
   * entry is a leaf entry, the id of the routing object, otherwise.
   * 
   * @return the id of the underlying database object of this entry, if this
   *         entry is a leaf entry, the id of the routing object, otherwise
   */
  DBID getRoutingObjectID();

  /**
   * Sets the id of the underlying database object of this entry, if this entry
   * is a leaf entry, the id of the routing object, otherwise.
   * 
   * @param objectID the id to be set
   */
  void setRoutingObjectID(DBID objectID);

  /**
   * Returns the distance from the routing object of this entry to the routing
   * object of its parent.
   * 
   * @return the distance from the object to its parent object
   */
  D getParentDistance();

  /**
   * Sets the distance from the routing object to routing object of its parent.
   * 
   * @param parentDistance the distance to be set
   */
  void setParentDistance(D parentDistance);

  /**
   * Returns the covering radius if this entry is a directory entry, null
   * otherwise.
   * 
   * @return the covering radius of this entry
   */
  D getCoveringRadius();

  /**
   * Sets the covering radius of this entry if this entry is a directory entry,
   * throws an UnsupportedOperationException otherwise.
   * 
   * @param coveringRadius the covering radius to be set
   */
  void setCoveringRadius(D coveringRadius);
}
