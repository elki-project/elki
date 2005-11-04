package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.distance.Distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * The class LeafEntry represents an entry in a leaf node of a M-Tree.
 * A LeafEntry consists of an id (representing the unique id
 * of the underlying object in the database) and the distance from the object
 * to its parent (routing object) in the M-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LeafEntry<D extends Distance> implements Entry<D> {
  /**
   * The id of the underlying metrical object of this entry.
   */
  private Integer objectID;

  /**
   * The distance from the object to its parent.
   */
  private D parentDistance;

  /**
   * Empty constructor for serialization purposes.
   */
  public LeafEntry() {
  }

  /**
   * Constructs a new LeafEntry object with the given parameters.
   *
   * @param objectID         the id of the underlying data object
   * @param parentDistance the distance from the object to its parent
   */
  public LeafEntry(Integer objectID, D parentDistance) {
    this.objectID = objectID;
    this.parentDistance = parentDistance;
  }

  /**
   * Returns the id of the underlying metrical object of this entry.
   *
   * @return the id of the underlying metrical object of this entry
   */
  public Integer getObjectID() {
    return objectID;
  }

  /**
   * Sets the id of the underlying metrical object of this entry.
   *
   * @param objectID the id to be set
   */
  public void setObjectID(Integer objectID) {
    this.objectID = objectID;
  }

  /**
   * Returns the distance from the object to its parent object.
   *
   * @return the distance from the object to its parent object
   */
  public D getParentDistance() {
    return parentDistance;
  }

  /**
   * Sets the distance from the object to its parent object.
   *
   * @param parentDistance the distance to be set
   */
  public void setParentDistance(D parentDistance) {
    this.parentDistance = parentDistance;
  }

  /**
   * Returns true if this entry is a leaf entry, false otherwise.
   *
   * @return true if this entry is a leaf entry, false otherwise
   */
  public boolean isLeafEntry() {
    return true;
  }

  /**
   * Writes the id and the parent distance of this entry to the specified output stream.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(objectID);
    out.writeObject(parentDistance);
  }

  /**
   * Reads the id and the parent distance of this entry from the specified input stream.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   */
  @SuppressWarnings({"unchecked"})
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.objectID = in.readInt();
    this.parentDistance = (D) in.readObject();
  }

  /**
   * Returns the id of the underlying data object.
   *
   * @return the id of the underlying data object
   */
  public Integer value() {
    return objectID;
  }

  /**
   * Returns false.
   *
   * @return false
   */
  public boolean isNodeID() {
    return false;
  }

  /**
   * Returns the id as a string representation of this entry.
   *
   * @return a string representation of this entry
   */
  public String toString() {
    return "" + objectID;
  }

  /**
   * Returns true</code> if this object is the same as the o
   * argument; <code>false</code> otherwise.
   *
   * @param o the reference object with which to compare.
   * @return <code>true</code> if this object is the same as the obj
   *         argument; <code>false</code> otherwise.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final LeafEntry leafEntry = (LeafEntry) o;

    if (!objectID.equals(leafEntry.objectID)) return false;
    return parentDistance.equals(leafEntry.parentDistance);
  }

  /**
   * Returns a hash code value for this object
   *
   * @return a hash code value for this object.
   */
  public int hashCode() {
    int result;
    result = objectID.hashCode();
    result = 29 * result + parentDistance.hashCode();
    return result;
  }
}
