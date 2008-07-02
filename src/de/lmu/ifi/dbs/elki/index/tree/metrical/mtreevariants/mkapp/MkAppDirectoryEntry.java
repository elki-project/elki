package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mkapp;

import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeDirectoryEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents an entry in a directory node of a MkApp-Tree.
 * Additionally to an MTreeDirectoryEntry an MkAppDirectoryEntry holds the
 * polynomial approximation of its knn-distances.
 *
 * @author Elke Achtert 
 */
class MkAppDirectoryEntry<D extends NumberDistance<D>> extends MTreeDirectoryEntry<D> implements MkAppEntry<D> {
  /**
   * The polynomial approximation.
   */
  private PolynomialApproximation approximation;

  /**
   * Empty constructor for serialization purposes.
   */
  public MkAppDirectoryEntry() {
    super();
  }

  /**
   * Provides a new MkCoPDirectoryEntry with the given parameters.
   *
   * @param objectID       the id of the routing object
   * @param parentDistance the distance from the object to its parent
   * @param nodeID         the id of the underlying node
   * @param coveringRadius the covering radius of the entry
   * @param approximation  the polynomial approximation of the knn distances
   */
  public MkAppDirectoryEntry(Integer objectID,
                             D parentDistance,
                             Integer nodeID,
                             D coveringRadius,
                             PolynomialApproximation approximation) {
    super(objectID, parentDistance, nodeID, coveringRadius);
    this.approximation = approximation;
  }

  /**
   * Returns the approximated value at the specified k.
   *
   * @param k the parameter k of the knn distance
   * @return the approximated value at the specified k
   */
  public double approximatedValueAt(int k) {
    return approximation.getValueAt(k);
  }

  /**
   * Returns the polynomial approximation.
   *
   * @return the polynomial approximation
   */
  public PolynomialApproximation getKnnDistanceApproximation() {
    return approximation;
  }

  /**
   * Sets the polynomial approximation.
   *
   * @param approximation the polynomial approximation to be set
   */
  public void setKnnDistanceApproximation(PolynomialApproximation approximation) {
    this.approximation = approximation;
  }

  /**
   * Calls the super method and writes the polynomial approximation
   * of the knn distances of this entry to the specified stream.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(approximation);
  }

  /**
   * Calls the super method and reads the the polynomial approximation
   * of the knn distances of this entry from the specified input stream.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    approximation = (PolynomialApproximation) in.readObject();
  }
}
