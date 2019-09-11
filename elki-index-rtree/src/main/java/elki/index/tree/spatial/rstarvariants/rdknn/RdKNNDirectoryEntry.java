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
package elki.index.tree.spatial.rstarvariants.rdknn;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import elki.data.ModifiableHyperBoundingBox;
import elki.index.tree.spatial.SpatialDirectoryEntry;

/**
 * Represents an entry in a directory node of an RdKNN-Tree. Additionally to a
 * SpatialDirectoryEntry a RdKNNDirectoryEntry holds the knn distance of the
 * underlying RdKNN-Tree node.
 * 
 * @author Elke Achtert
 * @since 0.1
 * @param Distance type
 */
public class RdKNNDirectoryEntry extends SpatialDirectoryEntry implements RdKNNEntry {
  private static final long serialVersionUID = 2;

  /**
   * The aggregated knn distance of this entry.
   */
  private double knnDistance;

  /**
   * Empty constructor for serialization purposes.
   */
  public RdKNNDirectoryEntry() {
    // empty constructor
  }

  /**
   * Constructs a new RDkNNDirectoryEntry object with the given parameters.
   * 
   * @param id the unique id of the underlying node
   * @param mbr the minimum bounding rectangle of the underlying node
   * @param knnDistance the aggregated knn distance of this entry
   */
  public RdKNNDirectoryEntry(int id, ModifiableHyperBoundingBox mbr, double knnDistance) {
    super(id, mbr);
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
   * 
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeDouble(knnDistance);
  }

  /**
   * Calls the super method and reads the knn distance of this entry from the
   * specified input stream.
   * 
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.knnDistance = in.readDouble();
  }
}
