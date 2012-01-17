package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents an entry in a leaf node of an RdKNN-Tree. Additionally to a
 * SpatialLeafEntry a RdKNNLeafEntry holds the knn distance of the underlying
 * data object.
 * 
 * @author Elke Achtert
 * @param <D> Distance type
 */
public class RdKNNLeafEntry<D extends NumberDistance<D, ?>> extends SpatialPointLeafEntry implements RdKNNEntry<D> {
  private static final long serialVersionUID = 1;

  /**
   * The knn distance of the underlying data object.
   */
  private D knnDistance;

  /**
   * Empty constructor for serialization purposes.
   */
  public RdKNNLeafEntry() {
    super();
  }

  /**
   * Constructs a new RDkNNLeafEntry object with the given parameters.
   * 
   * @param id the unique id of the underlying data object
   * @param values the values of the underlying data object
   * @param knnDistance the knn distance of the underlying data object
   */
  public RdKNNLeafEntry(DBID id, NumberVector<?, ?> vector, D knnDistance) {
    super(id, vector);
    this.knnDistance = knnDistance;
  }

  @Override
  public D getKnnDistance() {
    return knnDistance;
  }

  @Override
  public void setKnnDistance(D knnDistance) {
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
    out.writeObject(knnDistance);
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
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.knnDistance = (D) in.readObject();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   * 
   * @param o the object to be tested
   * @return true, if the super method returns true and o is an RDkNNLeafEntry
   *         and has the same knnDistance as this entry.
   */
  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(o == null || getClass() != o.getClass()) {
      return false;
    }
    if(!super.equals(o)) {
      return false;
    }

    final RdKNNLeafEntry<?> that = (RdKNNLeafEntry<?>) o;

    return knnDistance.equals(that.knnDistance);
  }
}