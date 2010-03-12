package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeDirectoryEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents an entry in a directory node of an {@link MkMaxTree}. Additionally
 * to an MTreeDirectoryEntry an MkMaxDirectoryEntry holds the knn distance of
 * the underlying MkMax-Tree node.
 * 
 * @author Elke Achtert
 * @param <D> the type of Distance used in the MkMaxTree
 */
class MkMaxDirectoryEntry<D extends Distance<D>> extends MTreeDirectoryEntry<D> implements MkMaxEntry<D> {
  private static final long serialVersionUID = 1;

  /**
   * The aggregated k-nearest neighbor distance of the underlying MkMax-Tree
   * node.
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
   * @param objectID the id of the routing object
   * @param parentDistance the distance from the routing object of this entry to
   *        its parent's routing object
   * @param nodeID the id of the underlying node
   * @param coveringRadius the covering radius of the entry
   * @param knnDistance the aggregated knn distance of the underlying MkMax-Tree
   *        node
   */
  public MkMaxDirectoryEntry(Integer objectID, D parentDistance, Integer nodeID, D coveringRadius, D knnDistance) {
    super(objectID, parentDistance, nodeID, coveringRadius);
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
   * @return true, if the super method returns true and o is an
   *         MkMaxDirectoryEntry and has the same knnDistance as this entry.
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

    final MkMaxDirectoryEntry<D> that = (MkMaxDirectoryEntry<D>) o;

    return !(knnDistance != null ? !knnDistance.equals(that.knnDistance) : that.knnDistance != null);
  }
}