package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialLeafEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents an entry in a leaf node of an RdKNN-Tree.
 * Additionally to a SpatialLeafEntry a RdKNNLeafEntry holds the knn distance
 * of the underlying data object.
 *
 * @author Elke Achtert 
 */
public class RdKNNLeafEntry<D extends NumberDistance<D,N>, N extends Number>
    extends SpatialLeafEntry implements RdKNNEntry<D,N> {
    
  /**
   * The knn distance of the underlying data object.
   */
  private D knnDistance;

  /**
   * Empty constructor for serialization purposes.
   */
  public RdKNNLeafEntry() {
    super();
  }

  /**
   * Constructs a new RDkNNLeafEntry object with the given parameters.
   *
   * @param id          the unique id of the underlying data object
   * @param values      the values of the underlying data object
   * @param knnDistance the knn distance of the underlying data object
   */
  public RdKNNLeafEntry(int id, double[] values, D knnDistance) {
    super(id, values);
    this.knnDistance = knnDistance;
  }

  public D getKnnDistance() {
    return knnDistance;
  }

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
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException,
                                                  ClassNotFoundException {
    super.readExternal(in);
    this.knnDistance = (D) in.readObject();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the object to be tested
   * @return true, if the super method returns true and
   *         o is an RDkNNLeafEntry and has the same
   *         knnDistance as this entry.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    final RdKNNLeafEntry<?,?> that = (RdKNNLeafEntry<?,?>) o;

    return knnDistance.equals(that.knnDistance);
  }
}
