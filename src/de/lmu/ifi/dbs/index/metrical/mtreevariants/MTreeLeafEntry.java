package de.lmu.ifi.dbs.index.metrical.mtreevariants;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.AbstractEntry;

/**
 * Represents an entry in a leaf node of an M-Tree.
 * A MTreeLeafEntry consists of an id (representing the unique id of the underlying
 * object in the database) and the distance from the data object to its parent
 * routing object in the M-Tree.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MTreeLeafEntry<D extends Distance> extends AbstractEntry implements MTreeEntry<D> {
  /**
   * The distance from the underlying data object to its parent's routing object.
   */
  private D parentDistance;

  /**
   * Empty constructor for serialization purposes.
   */
  public MTreeLeafEntry() {
  }

  /**
   * Provides a new MTreeLeafEntry object with the given parameters.
   *
   * @param objectID       the id of the underlying data object
   * @param parentDistance the distance from the underlying data object to its parent's routing object
   */
  public MTreeLeafEntry(Integer objectID, D parentDistance) {
    super(objectID);
    this.parentDistance = parentDistance;
  }

  /**
   * Returns the id of the underlying database object of this entry.
   *
   * @return the id of the underlying database object of this entry
   */
  public final Integer getRoutingObjectID() {
    return getID();
  }

  /**
   * Sets the id of the underlying database object of this entry.
   *
   * @param objectID the id to be set
   */
  public final void setRoutingObjectID(Integer objectID) {
    super.setID(objectID);
  }

  /**
   * Returns the distance from the underlying data object to its parent's routing object.
   *
   * @return the distance from the underlying data object to its parent's routing object
   */
  public final D getParentDistance() {
    return parentDistance;
  }

  /**
   * Sets the distance from the underlying data object to its parent's routing object.
   *
   * @param parentDistance the distance to be set
   */
  public final void setParentDistance(D parentDistance) {
    this.parentDistance = parentDistance;
  }

  /**
   * Returns true if this entry is a leaf entry, false otherwise.
   *
   * @return true if this entry is a leaf entry, false otherwise
   */
  public final boolean isLeafEntry() {
    return true;
  }

 /**
   * Calls the super method and writes the parentDistance of this entry to the specified
   * stream.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(parentDistance);
  }

  /**
   * Calls the super method and reads the parentDistance of this entry from the specified
   * input stream.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException,
                                                  ClassNotFoundException {
    super.readExternal(in);
    //noinspection unchecked
    this.parentDistance = (D) in.readObject();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the object to be tested
   * @return true, if the super method returns true and
   *         o is an MTreeLeafEntry and has the same
   *         parentDistance as this entry.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    final MTreeLeafEntry that = (MTreeLeafEntry) o;

    return !(parentDistance != null ? !parentDistance.equals(that.parentDistance) : that.parentDistance != null);
  }
}
