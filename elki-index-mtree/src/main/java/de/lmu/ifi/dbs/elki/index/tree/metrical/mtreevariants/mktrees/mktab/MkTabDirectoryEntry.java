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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mktab;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeDirectoryEntry;

/**
 * Represents an entry in a directory node of a MkTab-Tree. Additionally to a
 * MTreeLeafEntry a MkTabDirectoryEntry holds a list of its knn distances for
 * parameters k &lt;= k_max.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
class MkTabDirectoryEntry extends MTreeDirectoryEntry implements MkTabEntry {
  private static final long serialVersionUID = 2;

  /**
   * The aggregated knn distances of the underlying node.
   */
  private double[] knnDistances;

  /**
   * Empty constructor for serialization purposes.
   */
  public MkTabDirectoryEntry() {
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
   * @param knnDistances the aggregated knn distances of the underlying node
   */
  public MkTabDirectoryEntry(DBID objectID, double parentDistance, int nodeID, double coveringRadius, double[] knnDistances) {
    super(objectID, parentDistance, nodeID, coveringRadius);
    this.knnDistances = knnDistances;
  }

  @Override
  public double[] getKnnDistances() {
    return knnDistances;
  }

  @Override
  public void setKnnDistances(double[] knnDistances) {
    this.knnDistances = knnDistances;
  }

  @Override
  public double getKnnDistance(int k) {
    if(k >= this.knnDistances.length) {
      throw new IllegalArgumentException("Parameter k = " + k + " is not supported!");
    }

    return knnDistances[k - 1];
  }

  /**
   * Calls the super method and writes the parameter k_max and the knn distances
   * of this entry to the specified stream.
   * 
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    int k_max = knnDistances.length;
    out.writeInt(k_max);
    for(int i = 0; i < k_max; i++) {
      out.writeDouble(knnDistances[i]);
    }
  }

  /**
   * Calls the super method and reads the parameter k_max and knn distance of
   * this entry from the specified input stream.
   * 
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    int k_max = in.readInt();
    knnDistances = new double[k_max];
    for(int i = 0; i < k_max; i++) {
      knnDistances[i] = in.readDouble();
    }
  }
}
