package de.lmu.ifi.dbs.elki.index.tree;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Abstract superclass for entries in an tree based index structure.
 * 
 * @author Elke Achtert
 */
public abstract class AbstractEntry implements Entry {
  /**
   * Holds the id of the object (node or data object) represented by this entry.
   */
  private Integer id;

  /**
   * Empty constructor for serialization purposes.
   */
  public AbstractEntry() {
    // empty constructor
  }

  /**
   * Provides a new AbstractEntry with the specified id.
   * 
   * @param id the id of the object (node or data object) represented by this
   *        entry.
   */
  protected AbstractEntry(Integer id) {
    this.id = id;
  }

  /**
   * Returns the id of the node or data object that is represented by this
   * entry.
   * 
   * @return the id of the node or data object that is represented by this entry
   */
  public final Integer getID() {
    return id;
  }

  /**
   * Sets the id of the node or data object that is represented by this entry.
   * 
   * @param id the id to be set
   */
  public final void setID(Integer id) {
    this.id = id;
  }

  /**
   * Writes the id of the object (node or data object) that is represented by
   * this entry to the specified stream.
   */
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

    final AbstractEntry that = (AbstractEntry) o;

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
    if(isLeafEntry()) {
      return "o_" + id;
    }
    else {
      return "n_" + id;
    }
  }
}