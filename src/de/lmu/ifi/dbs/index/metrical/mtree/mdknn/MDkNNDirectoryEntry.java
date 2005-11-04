package de.lmu.ifi.dbs.index.metrical.mtree.mdknn;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.metrical.mtree.DirectoryEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * The class MDkNNDirectoryEntry represents an entry in a directory node of a MDkNN-Tree.
 * A MDkNNDirectoryEntry consists of an id (representing the unique id
 * of the underlying node), the routing object, the covering radius of the entry,
 * the distance from the routing object of the entry
 * to its parent (routing object) in the M-Tree and its knn distance.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MDkNNDirectoryEntry<D extends Distance> extends DirectoryEntry<D> {
  /**
   * The knn distance of the object.
   */
  private D knnDistance;

  /**
   * Empty constructor for serialization purposes.
   */
  public MDkNNDirectoryEntry() {
    super();
  }

  /**
   * Constructs a new Entry object with the given parameters.
   *
   * @param objectID       the id of the routing object
   * @param parentDistance the distance from the object to its parent
   * @param nodeID         the id of the underlying node
   * @param coveringRadius the covering radius of the entry
   * @param knnDistance    the knn distance of the object
   */
  public MDkNNDirectoryEntry(Integer objectID, D parentDistance, Integer nodeID,
                             D coveringRadius, D knnDistance) {
    super(objectID, parentDistance, nodeID, coveringRadius);
    this.knnDistance = knnDistance;
  }

  /**
   * Returns the knn distance of the object.
   * @return the knn distance of the object
   */
  public D getKnnDistance() {
    return knnDistance;
  }

  /**
   * Sets the knn distance of the object.
   * @param knnDistance the knn distance of the object to be set
   */
  public void setKnnDistance(D knnDistance) {
    this.knnDistance = knnDistance;
  }

  /**
   * Writes the knn distance of this entry to the specified output stream.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(knnDistance);
  }

  /**
   * Reads the knn distance of this entry from the specified input stream.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   */
  @SuppressWarnings({"unchecked"})
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.knnDistance = (D) in.readObject();
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
    if (!super.equals(o)) return false;

    final MDkNNDirectoryEntry that = (MDkNNDirectoryEntry) o;

    return knnDistance.equals(that.knnDistance);
  }

  /**
   * Returns a hash code value for this object
   *
   * @return a hash code value for this object.
   */
  public int hashCode() {
    int result = super.hashCode();
    result = 29 * result + knnDistance.hashCode();
    return result;
  }
}
