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
package de.lmu.ifi.dbs.elki.index.tree.spatial;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;

/**
 * Represents an entry in a leaf node of a spatial index. A SpatialLeafEntry
 * consists of an id (representing the unique id of the underlying data object)
 * and the values of the underlying data object.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
public class SpatialPointLeafEntry implements LeafEntry, SpatialEntry, NumberVector {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 2;

  /**
   * Holds the id of the object (node or data object) represented by this entry.
   */
  private DBID id;

  /**
   * The values of the underlying data object.
   */
  private double[] values;

  /**
   * Empty constructor for serialization purposes.
   */
  public SpatialPointLeafEntry() {
    super();
  }

  /**
   * Constructs a new LeafEntry object with the given parameters.
   * 
   * @param id the unique id of the underlying data object
   * @param values the values of the underlying data object
   */
  public SpatialPointLeafEntry(DBID id, double[] values) {
    super();
    this.id = id;
    this.values = values;
  }

  /**
   * Constructor from number vector.
   * 
   * @param id Object id
   * @param vector Number vector
   */
  public SpatialPointLeafEntry(DBID id, NumberVector vector) {
    super();
    this.id = id;
    int dim = vector.getDimensionality();
    this.values = new double[dim];
    for(int i = 0; i < dim; i++) {
      values[i] = vector.doubleValue(i);
    }
  }

  @Override
  public DBID getDBID() {
    return id;
  }

  @Override
  public int getDimensionality() {
    return values.length;
  }

  /**
   * Calls the super method and writes the values of this entry to the specified
   * stream.
   * 
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(DBIDUtil.asInteger(id));
    out.writeInt(values.length);
    for(double v : values) {
      out.writeDouble(v);
    }
  }

  /**
   * Calls the super method and reads the values of this entry from the
   * specified input stream.
   * 
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    id = DBIDUtil.importInteger(in.read());
    values = new double[in.readInt()];
    for(int d = 0; d < values.length; d++) {
      values[d] = in.readDouble();
    }
  }

  @Override
  public double doubleValue(int dimension) {
    return values[dimension];
  }

  @Override
  public long longValue(int dimension) {
    return (long) values[dimension];
  }

  @Override
  public double[] toArray() {
    return values.clone();
  }

  @Override
  public boolean equals(Object obj) {
    // Compare ID only.
    return this == obj || (obj != null && this.getClass() == obj.getClass() && //
        DBIDUtil.equal(this.id, ((SpatialPointLeafEntry) obj).id));
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
