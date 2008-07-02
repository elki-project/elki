package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mkapp;

import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeLeafEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents an entry in a leaf node of a MkApp-Tree.
 * Additionally to an MTreeLeafEntry an MkAppLeafEntry holds the
 * polynomial approximation of its knn-distances.
 *
 * @author Elke Achtert 
 */
class MkAppLeafEntry<D extends NumberDistance<D>> extends MTreeLeafEntry<D> implements MkAppEntry<D> {
  /**
   * The polynomial approximation.
   */
  private PolynomialApproximation approximation;

  /**
   * Empty constructor for serialization purposes.
   */
  public MkAppLeafEntry() {
	  // empty constructor
  }

  /**
   * Provides a new MkAppLeafEntry with the given parameters.
   *
   * @param objectID       the id of the underlying data object
   * @param parentDistance the distance from the underlying data object to its parent's routing object
   * @param approximation  the polynomial approximation of the knn distances
   */
  public MkAppLeafEntry(Integer objectID,
                        D parentDistance,
                        PolynomialApproximation approximation) {
    super(objectID, parentDistance);
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
   * Calls the super method and writes the polynomiale approximation
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

  /**
   * Returns a string representation of this entry.
   *
   * @return a string representation of this entry
   */
  public String toString() {
    return super.toString() +
           "\napprox " + approximation;
  }
}
