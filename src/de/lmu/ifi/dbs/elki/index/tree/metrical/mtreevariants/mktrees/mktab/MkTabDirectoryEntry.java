package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mktab;

import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeDirectoryEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an entry in a directory node of a MkTab-Tree. Additionally to a
 * MTreeLeafEntry a MkTabDirectoryEntry holds a list of its knn distances for
 * parameters k <= k_max.
 * 
 * @author Elke Achtert
 * 
 */
class MkTabDirectoryEntry<D extends Distance<D>> extends MTreeDirectoryEntry<D> implements MkTabEntry<D> {
  private static final long serialVersionUID = 1;

  /**
   * The maximal number of knn distances to be stored.
   */
  private int k_max;

  /**
   * The aggregated knn distances of the underlying node.
   */
  private List<D> knnDistances;

  /**
   * Empty constructor for serialization purposes.
   */
  public MkTabDirectoryEntry() {
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
   * @param knnDistances the aggregated knn distances of the underlying node
   */
  public MkTabDirectoryEntry(Integer objectID, D parentDistance, Integer nodeID, D coveringRadius, List<D> knnDistances) {
    super(objectID, parentDistance, nodeID, coveringRadius);
    this.knnDistances = knnDistances;
    this.k_max = knnDistances.size();
  }

  public List<D> getKnnDistances() {
    return knnDistances;
  }

  public void setKnnDistances(List<D> knnDistances) {
    this.knnDistances = knnDistances;
  }

  public D getKnnDistance(int k) {
    if(k > this.k_max) {
      throw new IllegalArgumentException("Parameter k = " + k + " is not supported!");
    }

    return knnDistances.get(k - 1);
  }

  public int getK_max() {
    return k_max;
  }

  /**
   * Calls the super method and writes the parameter k_max and the knn distances
   * of this entry to the specified stream.
   * 
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeInt(k_max);
    for(int i = 0; i < k_max; i++) {
      out.writeObject(knnDistances.get(i));
    }
  }

  /**
   * Calls the super method and reads the parameter k_max and knn distance of
   * this entry from the specified input stream.
   * 
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  @Override
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    k_max = in.readInt();
    knnDistances = new ArrayList<D>();
    for(int i = 0; i < k_max; i++) {
      knnDistances.add((D) in.readObject());
    }
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   * 
   * @param o the object to be tested
   * @return true, if the super method returns true and o is an
   *         MkTabDirectoryEntry and has the same parameter k_max and
   *         knnDistances as this entry.
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

    final MkTabDirectoryEntry<D> that = (MkTabDirectoryEntry<D>) o;

    if(k_max != that.k_max) {
      return false;
    }
    return !(knnDistances != null ? !knnDistances.equals(that.knnDistances) : that.knnDistances != null);
  }
}