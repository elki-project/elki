package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.AbstractLeafEntry;

/**
 * Represents an entry in a leaf node of an M-Tree. A MTreeLeafEntry consists of
 * an id (representing the unique id of the underlying object in the database)
 * and the distance from the data object to its parent routing object in the
 * M-Tree.
 * 
 * @author Elke Achtert
 * @since 0.2
 */
public class MTreeLeafEntry extends AbstractLeafEntry implements MTreeEntry {
  /**
   * Serialization version ID.
   */
  private static final long serialVersionUID = 2;

  /**
   * The distance from the underlying data object to its parent's routing
   * object.
   */
  private double parentDistance;

  /**
   * Empty constructor for serialization purposes.
   */
  public MTreeLeafEntry() {
    // empty
  }

  /**
   * Constructor.
   * 
   * @param objectID the id of the underlying data object
   * @param parentDistance the distance from the underlying data object to its
   *        parent's routing object
   */
  public MTreeLeafEntry(DBID objectID, double parentDistance) {
    super(objectID);
    this.parentDistance = parentDistance;
  }

  /**
   * Returns the id of the underlying data object of this entry.
   * 
   * @return the id of the underlying data object of this entry
   */
  @Override
  public final DBID getRoutingObjectID() {
    return getDBID();
  }

  /**
   * todo ok
   * 
   * @throws UnsupportedOperationException since leaf entries should not be
   *         assigned a routing object.
   */
  @Override
  public final boolean setRoutingObjectID(DBID objectID) {
    throw new UnsupportedOperationException("Leaf entries should not be assigned a routing object.");
  }

  /**
   * Returns the distance from the underlying data object to its parent's
   * routing object.
   * 
   * @return the distance from the underlying data object to its parent's
   *         routing object
   */
  @Override
  public final double getParentDistance() {
    return parentDistance;
  }

  /**
   * Sets the distance from the underlying data object to its parent's routing
   * object.
   * 
   * @param parentDistance the distance to be set
   */
  @Override
  public final boolean setParentDistance(double parentDistance) {
    if(this.parentDistance == parentDistance) {
      return false;
    }
    this.parentDistance = parentDistance;
    return true;
  }

  /**
   * Returns zero, since a leaf entry has no covering radius.
   * 
   * @return Zero
   */
  @Override
  public double getCoveringRadius() {
    return 0.0;
  }

  /**
   * Throws an UnsupportedOperationException, since a leaf entry has no covering
   * radius.
   * 
   * @throws UnsupportedOperationException thrown since a leaf has no covering
   *         radius
   */
  @Override
  public boolean setCoveringRadius(double coveringRadius) {
    throw new UnsupportedOperationException("This entry is not a directory entry!");
  }

  /**
   * Calls the super method and writes the parentDistance of this entry to the
   * specified stream.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeDouble(parentDistance);
  }

  /**
   * Calls the super method and reads the parentDistance of this entry from the
   * specified input stream.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.parentDistance = in.readDouble();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   * 
   * @param o the object to be tested
   * @return true, if the super method returns true and o is an MTreeLeafEntry
   *         and has the same parentDistance as this entry.
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

    final MTreeLeafEntry that = (MTreeLeafEntry) o;

    return Math.abs(parentDistance - that.parentDistance) < Double.MIN_NORMAL;
  }
}
