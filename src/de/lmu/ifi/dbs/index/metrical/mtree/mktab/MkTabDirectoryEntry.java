package de.lmu.ifi.dbs.index.metrical.mtree.mktab;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.metrical.mtree.DirectoryEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

/**
 * The class MkMaxDirectoryEntry represents an entry in a directory node of a MkMax-Tree.
 * Additionally to a DirectoryEntry, a MkMaxDirectoryEntry holds its knn distances.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MkTabDirectoryEntry<D extends Distance> extends DirectoryEntry<D> implements MkTabEntry<D> {
  /**
   * The maximal number of knn distances to be stored.
   */
  private int k;

  /**
   * The knn distances of the object.
   */
  private List<D> knnDistances;

  /**
   * Empty constructor for serialization purposes.
   */
  public MkTabDirectoryEntry() {
    super();
  }

  /**
   * Constructs a new MkMaxDirectoryEntry object with the given parameters.
   *
   * @param objectID       the id of the routing object
   * @param parentDistance the distance from the object to its parent
   * @param nodeID         the id of the underlying node
   * @param coveringRadius the covering radius of the entry
   * @param knnDistances   the knn distances of the object
   */
  public MkTabDirectoryEntry(Integer objectID, D parentDistance, Integer nodeID,
                             D coveringRadius, List<D> knnDistances) {
    super(objectID, parentDistance, nodeID, coveringRadius);
    this.knnDistances = knnDistances;
    this.k = knnDistances.size();
  }

  /**
   * Returns the knn distance of the object.
   *
   * @return the knn distance of the object
   */
  public List<D> getKnnDistances() {
    return knnDistances;
  }

  /**
   * Sets the knn distances of the object.
   *
   * @param knnDistances the knn distances to be set
   */
  public void setKnnDistances(List<D> knnDistances) {
    if (knnDistances.size() != this.k)
      throw new IllegalArgumentException("Wrong lenght of knn distances!");

    this.knnDistances = knnDistances;
  }

  /**
   * Returns the knn distance of the object.
   *
   * @param k the parameter k of the knn distance
   * @return the knn distance of the object
   */
  public D getKnnDistance(int k) {
    if (k > this.k)
      throw new IllegalArgumentException("Parameter k = " + k + " is not supported!");

    return knnDistances.get(k - 1);
  }

  /**
   * Returns the parameter k.
   *
   * @return the parameter k
   */
  public int getK() {
    return k;
  }

  /**
   * Writes the knn distances of this entry to the specified output stream.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeInt(k);
    for (int i = 0; i < k; i++) {
      out.writeObject(knnDistances.get(i));
    }
  }

  /**
   * Reads the knn distances of this entry from the specified input stream.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   */
  @SuppressWarnings({"unchecked"})
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    k = in.readInt();
    knnDistances = new ArrayList<D>();
    for (int i = 0; i < k; i++) {
      knnDistances.add((D) in.readObject());
    }
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

    final MkTabDirectoryEntry that = (MkTabDirectoryEntry) o;

    return knnDistances.equals(that.knnDistances);
  }

  /**
   * Returns a hash code value for this object
   *
   * @return a hash code value for this object.
   */
  public int hashCode() {
    int result = super.hashCode();
    result = 29 * result + k;
    return result;
  }
}
