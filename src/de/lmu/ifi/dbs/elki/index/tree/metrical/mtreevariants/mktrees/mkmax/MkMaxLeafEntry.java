package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeLeafEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents an entry in a leaf node of an {@link MkMaxTree}. Additionally to
 * an MTreeLeafEntry an MkMaxLeafEntry holds the k-nearest neighbor distance of
 * the underlying data object.
 * 
 * @author Elke Achtert
 * @param <D> the type of Distance used in the MkMaxTree
 */
class MkMaxLeafEntry<D extends Distance<D>> extends MTreeLeafEntry<D> implements MkMaxEntry<D> {
  private static final long serialVersionUID = 1;

  /**
   * The k-nearest neighbor distance of the underlying data object.
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
   * @param objectID the id of the underlying data object
   * @param parentDistance the distance from the underlying data object to its
   *        parent's routing object
   * @param knnDistance the knn distance of the underlying data object
   */
  public MkMaxLeafEntry(Integer objectID, D parentDistance, D knnDistance) {
    super(objectID, parentDistance);
    this.knnDistance = knnDistance;
  }

  public D getKnnDistance() {
    return knnDistance;
  }

  public void setKnnDistance(D knnDistance) {
    this.knnDistance = knnDistance;
  }

  /**
   * Calls the super method and writes the knn distance of this entry to the
   * specified stream.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(knnDistance);
  }

  /**
   * Calls the super method and reads the knn distance of this entry from the
   * specified input stream.
   */
  @Override
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.knnDistance = (D) in.readObject();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   * 
   * @param o the object to be tested
   * @return true, if the super method returns true and o is an MkMaxLeafEntry
   *         and has the same knnDistance as this entry.
   */
  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(o == null || getClass() != o.getClass()) {
      return false;
    }
    if(!super.equals(o)) {
      return false;
    }

    final MkMaxLeafEntry<D> that = (MkMaxLeafEntry<D>) o;

    return !(knnDistance != null ? !knnDistance.equals(that.knnDistance) : that.knnDistance != null);
  }
}