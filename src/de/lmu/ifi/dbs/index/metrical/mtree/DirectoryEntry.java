package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.distance.Distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * The class DirectoryEntry represents an entry in a directory node of an M-Tree.
 * A DirectoryEntry consists of an id (representing the unique id
 * of the underlying node), the id of the routing object, the covering radius of the entry and
 * the distance from the routing object of the entry to its parent (routing object) in the M-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DirectoryEntry<D extends Distance> implements Entry<D> {
  /**
   * The id of routing object of this entry.
   */
  private Integer routingObjectID;

  /**
   * The distance from the object to its parent.
   */
  private D parentDistance;

  /**
   * The id of the underlying node.
   */
  private Integer nodeID;

  /**
   * The covering radius of the entry.
   */
  private D coveringRadius;

  /**
   * Empty constructor for serialization purposes.
   */
  public DirectoryEntry() {
  }

  /**
   * Constructs a new Entry object with the given parameters.
   *
   * @param objectID       the id of the routing object
   * @param parentDistance the distance from the object to its parent
   * @param nodeID         the id of the underlying node
   * @param coveringRadius the covering radius of the entry
   */
  public DirectoryEntry(Integer objectID, D parentDistance, Integer nodeID, D coveringRadius) {
    this.routingObjectID = objectID;
    this.parentDistance = parentDistance;
    this.nodeID = nodeID;
    this.coveringRadius = coveringRadius;
  }

  /**
   * Returns the covering radius of this entry.
   *
   * @return the covering radius of this entry
   */
  public D getCoveringRadius() {
    return coveringRadius;
  }

  /**
   * Sets the covering radius of this entry.
   *
   * @param coveringRadius the covering radius to be set
   */
  public void setCoveringRadius(D coveringRadius) {
    this.coveringRadius = coveringRadius;
  }

  /**
   * Return the id of the underlying node
   *
   * @return the id of the underlying node
   */
  public Integer getNodeID() {
    return nodeID;
  }

  /**
   * Returns the id of the routing object
   *
   * @return the id of the routing object
   */
  public Integer getObjectID() {
    return routingObjectID;
  }

  /**
   * Sets the id of the underlying metrical object of this entry, if this entry is a leaf entry,
   * the id of the routing object, otherwise.
   *
   * @param objectID the id to be set
   */
  public void setObjectID(Integer objectID) {
    this.routingObjectID = objectID;
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
    return false;
  }

  /**
   * Writes the objectID, the parent distance, the nodeID and
   * the covering radius of this entry to the specified output stream.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(routingObjectID);
    out.writeObject(parentDistance);
    out.writeInt(nodeID);
    out.writeObject(coveringRadius);
  }

  /**
   * Reads the objectID, the parent distance, the nodeID and
   * the covering radius of this entry from the specified input stream.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   */
  @SuppressWarnings({"unchecked"})
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.routingObjectID = in.readInt();
    this.parentDistance = (D) in.readObject();
    this.nodeID = in.readInt();
    this.coveringRadius = (D) in.readObject();
  }

  /**
   * Returns the id of the underlying node.
   *
   * @return the id of the underlying node
   */
  public Integer value() {
    return nodeID;
  }

  /**
   * Returns true.
   *
   * @return true
   */
  public boolean isNodeID() {
    return true;
  }

  /**
   * Returns the id as a string representation of this entry.
   *
   * @return a string representation of this entry
   */
  public String toString() {
    return "n_" + nodeID + " (o.id = " + getObjectID() + ")";
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

    final DirectoryEntry that = (DirectoryEntry) o;

    if (!coveringRadius.equals(that.coveringRadius)) return false;
    if (!nodeID.equals(that.nodeID)) return false;
    if (routingObjectID != null ? !routingObjectID.equals(that.routingObjectID) : that.routingObjectID != null) return false;
    return !(parentDistance != null ? !parentDistance.equals(that.parentDistance) : that.parentDistance != null);
  }

  /**
   * Returns a hash code value for this object
   *
   * @return a hash code value for this object.
   */
  public int hashCode() {
    int result;
    result = (routingObjectID != null ? routingObjectID.hashCode() : 0);
    result = 29 * result + (parentDistance != null ? parentDistance.hashCode() : 0);
    result = 29 * result + nodeID.hashCode();
    result = 29 * result + coveringRadius.hashCode();
    return result;
  }
}
