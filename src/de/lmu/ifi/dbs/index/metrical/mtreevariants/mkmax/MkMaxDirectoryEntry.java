package de.lmu.ifi.dbs.index.metrical.mtreevariants.mkmax;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.MTreeDirectoryEntry;

/**
 * Represents an entry in a directory node of an MkMax-Tree.
 * Additionally to a MTreeDirectoryEntry a MkMaxDirectoryEntry holds the knn distance
 * of the underlying MkMax-Tree node.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MkMaxDirectoryEntry<D extends Distance> extends MTreeDirectoryEntry<D> implements MkMaxEntry<D> {
  /**
   * The aggregated knn distance of the underlying MkMax-Tree node.
   */
  private D knnDistance;

  /**
   * Empty constructor for serialization purposes.
   */
  public MkMaxDirectoryEntry() {
    super();
  }

  /**
   * Provides a new MkMaxDirectoryEntry with the given parameters.
   *
   * @param objectID       the id of the routing object
   * @param parentDistance the distance from the routing object of this entry to its parent's routing object
   * @param nodeID         the id of the underlying node
   * @param coveringRadius the covering radius of the entry
   * @param knnDistance    the aggregated knn distance of the underlying MkMax-Tree node
   */
  public MkMaxDirectoryEntry(Integer objectID, D parentDistance, Integer nodeID, D coveringRadius, D knnDistance) {
    super(objectID, parentDistance, nodeID, coveringRadius);
    this.knnDistance = knnDistance;
  }

  /**
   * @see de.lmu.ifi.dbs.index.metrical.mtreevariants.mkmax.MkMaxEntry#getKnnDistance()
   */
  public D getKnnDistance() {
    return knnDistance;
  }

  /**
   * @see de.lmu.ifi.dbs.index.metrical.mtreevariants.mkmax.MkMaxEntry#setKnnDistance(de.lmu.ifi.dbs.distance.Distance)
   */
  public void setKnnDistance(D knnDistance) {
    this.knnDistance = knnDistance;
  }

  /**
   * Calls the super method and writes the knn distance of this entry to the specified
   * stream.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(knnDistance);
  }

  /**
   * Calls the super method and reads the knn distance of this entry from the specified
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
    this.knnDistance = (D) in.readObject();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the object to be tested
   * @return true, if the super method returns true and
   *         o is an MkMaxDirectoryEntry and has the same
   *         knnDistance as this entry.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    final MkMaxDirectoryEntry that = (MkMaxDirectoryEntry) o;

    return !(knnDistance != null ? !knnDistance.equals(that.knnDistance) : that.knnDistance != null);
  }
}
