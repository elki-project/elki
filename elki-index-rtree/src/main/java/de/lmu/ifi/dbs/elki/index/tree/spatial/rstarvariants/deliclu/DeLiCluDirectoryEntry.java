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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;

/**
 * Defines the requirements for a directory entry in an DeLiClu-Tree node.
 * Additionally to a directory entry in an R*-Tree two boolean flags that
 * indicate whether this entry's node contains handled or unhandled data
 * objects.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
public class DeLiCluDirectoryEntry extends SpatialDirectoryEntry implements DeLiCluEntry {
  private static final long serialVersionUID = 1;

  /**
   * Indicates that the node (or its child nodes) which is represented by this
   * entry contains handled data objects.
   */
  private boolean hasHandled;

  /**
   * Indicates that the node (or its child nodes) which is represented by this
   * entry contains unhandled data objects.
   */
  private boolean hasUnhandled;

  /**
   * Empty constructor for serialization purposes.
   */
  public DeLiCluDirectoryEntry() {
    // empty constructor
  }

  /**
   * Constructs a new DeLiCluDirectoryEntry object with the given parameters.
   * 
   * @param id the unique id of the underlying spatial node
   * @param mbr the minimum bounding rectangle of the underlying spatial node
   * @param hasHandled indicates if this entry has handled nodes
   * @param hasUnhandled indicates if this entry has unhandled nodes
   */
  public DeLiCluDirectoryEntry(int id, ModifiableHyperBoundingBox mbr, boolean hasHandled, boolean hasUnhandled) {
    super(id, mbr);
    this.hasHandled = hasHandled;
    this.hasUnhandled = hasUnhandled;
  }

  @Override
  public boolean hasHandled() {
    return hasHandled;
  }

  @Override
  public boolean hasUnhandled() {
    return hasUnhandled;
  }

  @Override
  public void setHasHandled(boolean hasHandled) {
    this.hasHandled = hasHandled;
  }

  @Override
  public void setHasUnhandled(boolean hasUnhandled) {
    this.hasUnhandled = hasUnhandled;
  }

  /**
   * Returns the id as a string representation of this entry.
   * 
   * @return a string representation of this entry
   */
  @Override
  public String toString() {
    return super.toString() + "[" + hasHandled + "-" + hasUnhandled + "]";
  }
}