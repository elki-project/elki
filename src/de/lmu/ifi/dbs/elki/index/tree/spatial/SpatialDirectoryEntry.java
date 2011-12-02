package de.lmu.ifi.dbs.elki.index.tree.spatial;

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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.index.tree.AbstractDirectoryEntry;

/**
 * Represents an entry in a directory node of a spatial index.
 * 
 * A SpatialDirectoryEntry consists of an id (representing the unique id of the
 * underlying spatial node) and the minimum bounding rectangle of the underlying
 * spatial node.
 * 
 * @author Elke Achtert
 */
public class SpatialDirectoryEntry extends AbstractDirectoryEntry implements SpatialEntry {
  private static final long serialVersionUID = 1;

  /**
   * The minimum bounding rectangle of the underlying spatial node.
   */
  private ModifiableHyperBoundingBox mbr;

  /**
   * Empty constructor for serialization purposes.
   */
  public SpatialDirectoryEntry() {
    // empty constructor
  }

  /**
   * Constructs a new SpatialDirectoryEntry object with the given parameters.
   * 
   * @param id the unique id of the underlying spatial node
   * @param mbr the minimum bounding rectangle of the underlying spatial node
   */
  public SpatialDirectoryEntry(int id, ModifiableHyperBoundingBox mbr) {
    super(id);
    this.mbr = mbr;
  }

  @Override
  public int getDimensionality() {
    return mbr.getDimensionality();
  }

  /**
   * @return the coordinate at the specified dimension of the minimum hyper
   *         point of the MBR of the underlying node
   */
  @Override
  public double getMin(int dimension) {
    return mbr.getMin(dimension);
  }

  /**
   * @return the coordinate at the specified dimension of the maximum hyper
   *         point of the MBR of the underlying node
   */
  @Override
  public double getMax(int dimension) {
    return mbr.getMax(dimension);
  }

  /**
   * Test whether this entry already has an MBR.
   * 
   * @return True when an MBR exists.
   */
  public boolean hasMBR() {
    return (this.mbr != null);
  }

  /**
   * Sets the MBR of this entry.
   * 
   * @param mbr the MBR to be set
   */
  public void setMBR(ModifiableHyperBoundingBox mbr) {
    this.mbr = mbr;
  }

  /**
   * Calls the super method and writes the MBR object of this entry to the
   * specified output stream.
   * 
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    mbr.writeExternal(out);
  }

  /**
   * Calls the super method and reads the MBR object of this entry from the
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
    this.mbr = new ModifiableHyperBoundingBox();
    this.mbr.readExternal(in);
  }

  /**
   * Extend the MBR of this node.
   * 
   * @param responsibleMBR
   * @return true when the MBR changed
   */
  public boolean extendMBR(SpatialComparable responsibleMBR) {
    return this.mbr.extend(responsibleMBR);
  }
}