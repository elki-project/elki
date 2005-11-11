package de.lmu.ifi.dbs.index.metrical.mtree.mcop;

import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.index.metrical.mtree.DirectoryEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * The class MCopDirectoryEntry represents an entry in a directory node of a MCop-Tree.
 * Additionally to a DirectoryEntry, a MCopDirectoryEntry holds its knn distances.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MkCoPDirectoryEntry extends DirectoryEntry<DoubleDistance> implements MkCoPEntry {
  /**
   * The maximal number of knn distances to be stored.
   */
  private int k;

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
  public MkCoPDirectoryEntry() {
    super();
  }

  /**
   * Constructs a new MCopDirectoryEntry object with the given parameters.
   *
   * @param objectID                  the id of the routing object
   * @param parentDistance            the distance from the object to its parent
   * @param nodeID                    the id of the underlying node
   * @param coveringRadius            the covering radius of the entry
   * @param k                         the maximal number of knn distances to be stored
   * @param conservativeApproximation the conservative approximation of the knn distances
   * @param progressiveApproximation  the progressive approximation of the knn distances
   */
  public MkCoPDirectoryEntry(Integer objectID, DoubleDistance parentDistance, Integer nodeID,
                             DoubleDistance coveringRadius, int k,
                             ApproximationLine conservativeApproximation,
                             ApproximationLine progressiveApproximation) {
    super(objectID, parentDistance, nodeID, coveringRadius);
    this.k = k;
    this.conservativeApproximation = conservativeApproximation;
    this.progressiveApproximation = progressiveApproximation;
  }

  /**
   * Returns the conservative approximated knn distance of the entry.
   *
   * @param k the parameter k of the knn distance
   * @return the conservative approximated knn distance of the entry
   */
  public DoubleDistance approximateConservativeKnnDistance(int k) {
    if (k > this.k)
      throw new IllegalArgumentException("Parameter k = " + k + " is not supported!");

    return conservativeApproximation.getApproximatedKnnDistance(k);
  }

  /**
   * Returns the progressive approximated knn distance of the entry.
   *
   * @param k the parameter k of the knn distance
   * @return the progressive approximated knn distance of the entry
   */
  public DoubleDistance approximateProgressiveKnnDistance(int k) {
    if (k > this.k)
      throw new IllegalArgumentException("Parameter k = " + k + " is not supported!");

    return progressiveApproximation.getApproximatedKnnDistance(k);
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
    out.writeInt(k);
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
    k = in.readInt();
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

    final MkCoPDirectoryEntry that = (MkCoPDirectoryEntry) o;

    if (k != that.k) return false;
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
    result = 29 * result + k;
    result = 29 * result + conservativeApproximation.hashCode();
    result = 29 * result + progressiveApproximation.hashCode();
    return result;
  }

}
