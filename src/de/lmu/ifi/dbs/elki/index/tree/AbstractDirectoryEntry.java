package de.lmu.ifi.dbs.elki.index.tree;
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

/**
 * Abstract superclass for entries in an tree based index structure.
 * 
 * @author Elke Achtert
 */
public abstract class AbstractDirectoryEntry implements DirectoryEntry {
  /**
   * Holds the id of the object (node or data object) represented by this entry.
   */
  private Integer id;

  /**
   * Empty constructor for serialization purposes.
   */
  public AbstractDirectoryEntry() {
    // empty constructor
  }

  /**
   * Provides a new AbstractEntry with the specified id.
   * 
   * @param id the id of the object (node or data object) represented by this
   *        entry.
   */
  protected AbstractDirectoryEntry(Integer id) {
    this.id = id;
  }

  @Override
  public final boolean isLeafEntry() {
    return false;
  }
  
  /**
   * Returns the id of the node or data object that is represented by this
   * entry.
   * 
   * @return the id of the node or data object that is represented by this entry
   */
  @Override
  public final Integer getPageID() {
    return id;
  }

  /**
   * Returns the id of the node or data object that is represented by this
   * entry.
   * 
   * @return the id of the node or data object that is represented by this entry
   */
  @Override
  public final Integer getEntryID() {
    return id;
  }
  
  /**
   * Sets the id of the node or data object that is represented by this entry.
   * 
   * @param id the id to be set
   */
  // Should be set by the constructor, then final.
  /*public final void setPageID(Integer id) {
    this.id = id;
  }*/

  /**
   * Writes the id of the object (node or data object) that is represented by
   * this entry to the specified stream.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(id);
  }

  /**
   * Restores the id of the object (node or data object) that is represented by
   * this entry from the specified stream.
   * 
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.id = in.readInt();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   * 
   * @param o the object to be tested
   * @return true, if o is an AbstractEntry and has the same id as this entry.
   */
  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(o == null || getClass() != o.getClass()) {
      return false;
    }

    final AbstractDirectoryEntry that = (AbstractDirectoryEntry) o;

    return id == that.id;
  }

  /**
   * Returns as hash code for the entry its id.
   * 
   * @return the id of the entry
   */
  @Override
  public int hashCode() {
    return id;
  }

  /**
   * Returns the id as a string representation of this entry.
   * 
   * @return a string representation of this entry
   */
  @Override
  public String toString() {
    return "n_" + id;
  }
}