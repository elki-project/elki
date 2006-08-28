package de.lmu.ifi.dbs.index.spatial.rstarvariants.rdknn;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.spatial.MBR;
import de.lmu.ifi.dbs.index.spatial.SpatialDirectoryEntry;

/**
 * Represents an entry in a directory node of an RdKNN-Tree.
 * Additionally to a SpatialDirectoryEntry a RdKNNDirectoryEntry holds the knn distance
 * of the underlying RdKNN-Tree node.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RdKNNDirectoryEntry<D extends NumberDistance> extends SpatialDirectoryEntry implements RdKNNEntry<D> {
  /**
   * The aggregated knn distance of this entry.
   */
  private D knnDistance;

  /**
   * Empty constructor for serialization purposes.
   */
  public RdKNNDirectoryEntry() {
  }

  /**
   * Constructs a new RDkNNDirectoryEntry object with the given parameters.
   *
   * @param id          the unique id of the underlying node
   * @param mbr         the minmum bounding rectangle of the underlying node
   * @param knnDistance the aggregated knn distance of this entry
   */
  public RdKNNDirectoryEntry(int id, MBR mbr, D knnDistance) {
    super(id, mbr);
    this.knnDistance = knnDistance;
  }

  /**
   * @see de.lmu.ifi.dbs.index.spatial.rstarvariants.rdknn.RdKNNEntry#getKnnDistance()
   */
  public D getKnnDistance() {
    return knnDistance;
  }

  /**
   * @see de.lmu.ifi.dbs.index.spatial.rstarvariants.rdknn.RdKNNEntry#setKnnDistance(de.lmu.ifi.dbs.distance.NumberDistance)
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
   *         o is an RDkNNDirectoryEntry and has the same
   *         knnDistance as this entry.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    final RdKNNDirectoryEntry that = (RdKNNDirectoryEntry) o;

    return knnDistance.equals(that.knnDistance);
  }
}
