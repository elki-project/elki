package de.lmu.ifi.dbs.index.metrical.mtree.mkcop;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.metrical.mtree.MTreeLeafEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * The class MkMaxLeafEntry represents an entry in a leaf node of a MkMax-Tree.
 * Additionally to a LeafEntry, a MkMaxLeafEntry holds its knn-distances.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MkCoPLeafEntry<D extends NumberDistance<D>> extends MTreeLeafEntry<D> implements MkCoPEntry<D> {
  /**
   * The conservative approximation.
   */
  private ApproximationLine conservativeApproximation;

  /**
   * The progressive approximation.
   */
  private ApproximationLine progressiveApproximation;

  /**
   * Empty constructor for serialization purposes.
   */
  public MkCoPLeafEntry() {
  }

  /**
   * Constructs a new MkMaxLeafEntry object with the given parameters.
   *
   * @param objectID                  the id of the underlying data object
   * @param parentDistance            the distance from the object to its parent
   * @param conservativeApproximation the conservative approximation of the knn distances
   * @param progressiveApproximation  the progressive approximation of the knn distances
   */
  public MkCoPLeafEntry(Integer objectID,
                        D parentDistance,
                        ApproximationLine conservativeApproximation,
                        ApproximationLine progressiveApproximation) {
    super(objectID, parentDistance);
    this.conservativeApproximation = conservativeApproximation;
    this.progressiveApproximation = progressiveApproximation;
  }

  /**
   * Returns the conservative approximated knn distance of the entry.
   *
   * @param k                the parameter k of the knn distance
   * @param distanceFunction the distance function
   * @return the conservative approximated knn distance of the entry
   */
  public <O extends MetricalObject> D approximateConservativeKnnDistance(int k, DistanceFunction<O, D> distanceFunction) {
    return conservativeApproximation.getApproximatedKnnDistance(k, distanceFunction);
  }

  /**
   * Returns the progressive approximated knn distance of the entry.
   *
   * @param k                the parameter k of the knn distance
   * @param distanceFunction the distance function
   * @return the progressive approximated knn distance of the entry
   */
  public <O extends MetricalObject> D approximateProgressiveKnnDistance(int k, DistanceFunction<O, D> distanceFunction) {
    return progressiveApproximation.getApproximatedKnnDistance(k, distanceFunction);
  }

  /**
   * Returns the conservative approximation line.
   *
   * @return the conservative approximation line
   */
  public ApproximationLine getConservativeKnnDistanceApproximation() {
    return conservativeApproximation;
  }

  /**
   * Returns the progressive approximation line.
   *
   * @return the progressive approximation line
   */
  public ApproximationLine getProgressiveKnnDistanceApproximation() {
    return progressiveApproximation;
  }

  /**
   * Sets the conservative approximation line
   *
   * @param conservativeApproximation the conservative approximation line to be set
   */
  public void setConservativeKnnDistanceApproximation(ApproximationLine conservativeApproximation) {
    this.conservativeApproximation = conservativeApproximation;
  }

  /**
   * Sets the progressive approximation line
   *
   * @param progressiveApproximation the progressive approximation line to be set
   */
  public void setProgressiveKnnDistanceApproximation(ApproximationLine progressiveApproximation) {
    this.progressiveApproximation = progressiveApproximation;
  }

  /**
   * Writes the knn distances of this entry to the specified output stream.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(conservativeApproximation);
    out.writeObject(progressiveApproximation);
  }

  /**
   * Reads the knn distances of this entry from the specified input stream.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    conservativeApproximation = (ApproximationLine) in.readObject();
    progressiveApproximation = (ApproximationLine) in.readObject();
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

    final MkCoPLeafEntry that = (MkCoPLeafEntry) o;

    if (!conservativeApproximation.equals(that.conservativeApproximation)) return false;
    return progressiveApproximation.equals(that.progressiveApproximation);
  }

  /**
   * Returns a hash code value for this object
   *
   * @return a hash code value for this object.
   */
  public int hashCode() {
    int result = super.hashCode();
    result = 29 * result + conservativeApproximation.hashCode();
    result = 29 * result + progressiveApproximation.hashCode();
    return result;
  }

  /**
   * Returns the id as a string representation of this entry.
   *
   * @return a string representation of this entry
   */
  public String toString() {
    return super.toString() +
           "\ncons " + conservativeApproximation + "\n";
//           "prog " + progressiveApproximation;
  }
}
