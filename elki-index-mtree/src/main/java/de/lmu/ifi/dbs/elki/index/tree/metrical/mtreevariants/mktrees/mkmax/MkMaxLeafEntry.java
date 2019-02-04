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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeLeafEntry;

/**
 * Represents an entry in a leaf node of an {@link MkMaxTree}. Additionally to
 * an MTreeLeafEntry an MkMaxLeafEntry holds the k-nearest neighbor distance of
 * the underlying data object.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
class MkMaxLeafEntry extends MTreeLeafEntry implements MkMaxEntry {
  /**
   * Serial version number
   */
  private static final long serialVersionUID = 1;

  /**
   * The k-nearest neighbor distance of the underlying data object.
   */
  private double knnDistance;

  /**
   * Empty constructor for serialization purposes.
   */
  public MkMaxLeafEntry() {
    // empty constructor
  }

  /**
   * Constructor.
   * 
   * @param objectID the id of the underlying data object
   * @param parentDistance the distance from the underlying data object to its
   *        parent's routing object
   * @param knnDistance the knn distance of the underlying data object
   */
  public MkMaxLeafEntry(DBID objectID, double parentDistance, double knnDistance) {
    super(objectID, parentDistance);
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
    out.writeObject(knnDistance);
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
