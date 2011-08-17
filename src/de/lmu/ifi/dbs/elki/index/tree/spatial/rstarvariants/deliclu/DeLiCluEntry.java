package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu;
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

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;

/**
 * Defines the requirements for an entry in an DeLiClu-Tree node.
 * Additionally to an entry in an R*-Tree two boolean flags that indicate whether this entry's node
 * contains handled or unhandled data objects.
 *
 * @author Elke Achtert 
 */
public interface DeLiCluEntry extends SpatialEntry {
  /**
   * Returns true, if the node (or its child nodes) which is represented by this entry
   * contains handled data objects.
   *
   * @return true, if the node (or its child nodes) which is represented by this entry
   *         contains handled data objects,
   *         false otherwise.
   */
  public boolean hasHandled();

  /**
   * Returns true, if the node (or its child nodes) which is represented by this entry
   * contains unhandled data objects.
   *
   * @return true, if the node (or its child nodes) which is represented by this entry
   *         contains unhandled data objects,
   *         false otherwise.
   */
  public boolean hasUnhandled();

  /**
   * Sets the flag to marks the node (or its child nodes) which is represented by this entry
   * to contain handled data objects.
   *
   * @param hasHandled the flag to be set
   */
  public void setHasHandled(boolean hasHandled);

  /**
   * Sets the flag to marks the node (or its child nodes) which is represented by this entry
   * to contain unhandled data objects.
   *
   * @param hasUnhandled the flag to be set
   */
  public void setHasUnhandled(boolean hasUnhandled);
}
