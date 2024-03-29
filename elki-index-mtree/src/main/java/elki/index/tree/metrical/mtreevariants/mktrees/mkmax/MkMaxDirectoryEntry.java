/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import elki.database.ids.DBID;
import elki.index.tree.metrical.mtreevariants.MTreeDirectoryEntry;

/**
 * Represents an entry in a directory node of an {@link MkMaxTree}. Additionally
 * to an MTreeDirectoryEntry an MkMaxDirectoryEntry holds the knn distance of
 * the underlying MkMax-Tree node.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
class MkMaxDirectoryEntry extends MTreeDirectoryEntry implements MkMaxEntry {
  /** Serial version UID */
  private static final long serialVersionUID = 2;

  /**
   * The aggregated k-nearest neighbor distance of the underlying MkMax-Tree
   * node.
   */
  private double knnDistance;

  /**
   * Empty constructor for serialization purposes.
   */
  public MkMaxDirectoryEntry() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param objectID the id of the routing object
   * @param parentDistance the distance from the routing object of this entry to
   *        its parent's routing object
   * @param nodeID the id of the underlying node
   * @param coveringRadius the covering radius of the entry
   * @param knnDistance the aggregated knn distance of the underlying MkMax-Tree
   *        node
   */
  public MkMaxDirectoryEntry(DBID objectID, double parentDistance, int nodeID, double coveringRadius, double knnDistance) {
    super(objectID, parentDistance, nodeID, coveringRadius);
    this.knnDistance = knnDistance;
  }

  @Override
  public double getKnnDistance() {
    return knnDistance;
  }

  @Override
  public void setKnnDistance(double knnDistance) {
    this.knnDistance = knnDistance;
  }

  /**
   * Calls the super method and writes the knn distance of this entry to the
   * specified stream.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeDouble(knnDistance);
  }

  /**
   * Calls the super method and reads the knn distance of this entry from the
   * specified input stream.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.knnDistance = in.readDouble();
  }
}
