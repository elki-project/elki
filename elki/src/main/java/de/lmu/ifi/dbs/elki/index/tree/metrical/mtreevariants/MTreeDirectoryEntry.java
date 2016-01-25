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
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.index.tree.AbstractDirectoryEntry;

/**
 * Represents an entry in a directory node of an M-Tree. A MTreeDirectoryEntry
 * consists of an id (representing the unique id of the underlying node), the id
 * of the routing object, the covering radius of the entry and the distance from
 * the routing object of the entry to its parent's routing object in the M-Tree.
 * 
 * @author Elke Achtert
 * @since 0.2
 */
public class MTreeDirectoryEntry extends AbstractDirectoryEntry implements MTreeEntry {
  private static final long serialVersionUID = 2;

  /**
   * The id of routing object of this entry.
   */
  private DBID routingObjectID;

  /**
   * The distance from the routing object of this entry to its parent's routing
   * object.
   */
  private double parentDistance;

  /**
   * The covering radius of the entry.
   */
  private double coveringRadius;

  /**
   * Empty constructor for serialization purposes.
   */
  public MTreeDirectoryEntry() {
    // empty constructor
  }

  /**
   * Constructor.
   * 
   * @param objectID the id of the routing object
   * @param parentDistance the distance from the routing object of this entry to
   *        its parent's routing object
   * @param nodeID the id of the underlying node
   * @param coveringRadius the covering radius of the entry
   */
  public MTreeDirectoryEntry(DBID objectID, double parentDistance, Integer nodeID, double coveringRadius) {
    super(nodeID);
    this.routingObjectID = objectID;
    this.parentDistance = parentDistance;
    this.coveringRadius = coveringRadius;
  }

  /**
   * Returns the covering radius of this entry.
   * 
   * @return the covering radius of this entry
   */
  @Override
  public final double getCoveringRadius() {
    return coveringRadius;
  }

  /**
   * Sets the covering radius of this entry.
   * 
   * @param coveringRadius the covering radius to be set
   */
  @Override
  public final boolean setCoveringRadius(double coveringRadius) {
    if(this.coveringRadius == coveringRadius) {
      return false;
    }
    this.coveringRadius = coveringRadius;
    return true;
  }

  /**
   * Returns the id of the routing object of this entry.
   * 
   * @return the id of the routing object
   */
  @Override
  public final DBID getRoutingObjectID() {
    return routingObjectID;
  }

  /**
   * Sets the id of the routing object of this entry.
   * 
   * @param objectID the id to be set
   */
  @Override
  public final boolean setRoutingObjectID(DBID objectID) {
    if(objectID == routingObjectID || DBIDUtil.equal(objectID, routingObjectID)) {
      return false;
    }
    this.routingObjectID = objectID;
    return true;
  }

  /**
   * Returns the distance from the routing object of this entry to its parent's
   * routing object.
   * 
   * @return the distance from the routing object of this entry to its parent's
   *         routing object.
   */
  @Override
  public final double getParentDistance() {
    return parentDistance;
  }

  /**
   * Sets the distance from the object to its parent object.
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
   * Calls the super method and writes the routingObjectID, the parentDistance
   * and the coveringRadius of this entry to the specified stream.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeInt(DBIDUtil.asInteger(routingObjectID));
    out.writeDouble(parentDistance);
    out.writeDouble(coveringRadius);
  }

  /**
   * Calls the super method and reads the routingObjectID, the parentDistance
   * and the coveringRadius of this entry from the specified input stream.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.routingObjectID = DBIDUtil.importInteger(in.readInt());
    this.parentDistance = in.readDouble();
    this.coveringRadius = in.readDouble();
  }

  /**
   * Returns the id as a string representation of this entry.
   * 
   * @return a string representation of this entry
   */
  @Override
  public String toString() {
    return super.toString() + " (o.id = " + getRoutingObjectID() + ")";
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   * 
   * @param o the object to be tested
   * @return true, if the super method returns true and o is an
   *         MTreeDirectoryEntry and has the same coveringRadius, parentDistance
   *         and routingObjectID as this entry.
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

    final MTreeDirectoryEntry that = (MTreeDirectoryEntry) o;

    if(Math.abs(coveringRadius - that.coveringRadius) < Double.MIN_NORMAL) {
      return false;
    }
    if(Math.abs(parentDistance - that.parentDistance) < Double.MIN_NORMAL) {
      return false;
    }
    return !(routingObjectID != null ? !DBIDUtil.equal(routingObjectID, that.routingObjectID) : that.routingObjectID != null);
  }
}
