package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeLeafEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents an entry in a leaf node of a MkMax-Tree.
 * Additionally to a MTreeLeafEntry a MkMaxLeafEntry holds its knn distance.
 *
 * @author Elke Achtert
 */
class MkMaxLeafEntry<D extends Distance<D>> extends MTreeLeafEntry<D> implements MkMaxEntry<D> {

  /**
   * The knn distance of the underlying data object.
   */
  private D knnDistance;

  /**
   * Empty constructor for serialization purposes.
   */
  public MkMaxLeafEntry() {
	  // empty constructor
  }

  /**
   * Provides a new MkMaxLeafEntry with the given parameters.
   *
   * @param objectID       the id of the underlying data object
   * @param parentDistance the distance from the underlying data object to its parent's routing object
   * @param knnDistance    the knn distance of the underlying data object
   */
  public MkMaxLeafEntry(Integer objectID, D parentDistance, D knnDistance) {
    super(objectID, parentDistance);
    this.knnDistance = knnDistance;
  }

  /**
   * @see de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax.MkMaxEntry#getKnnDistance()
   */
  public D getKnnDistance() {
    return knnDistance;
  }

  /**
   * @see de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax.MkMaxEntry#setKnnDistance(de.lmu.ifi.dbs.elki.distance.Distance)
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
   *         o is an MkMaxLeafEntry and has the same
   *         knnDistance as this entry.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    final MkMaxLeafEntry<D> that = (MkMaxLeafEntry<D>) o;

    return !(knnDistance != null ? !knnDistance.equals(that.knnDistance) : that.knnDistance != null);
  }
}
