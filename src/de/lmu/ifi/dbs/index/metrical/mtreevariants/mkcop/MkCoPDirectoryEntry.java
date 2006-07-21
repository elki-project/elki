package de.lmu.ifi.dbs.index.metrical.mtreevariants.mkcop;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.MTreeDirectoryEntry;

/**
 * Represents an entry in a directory node of a MCop-Tree.
 * Additionally to a MTreeDirectoryEntry a MkCoPLeafEntry holds the conservative
 * approximation of its knn-distances.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MkCoPDirectoryEntry<D extends NumberDistance<D>> extends MTreeDirectoryEntry<D> implements MkCoPEntry<D> {
  /**
   * The conservative approximation.
   */
  private ApproximationLine conservativeApproximation;

  /**
   * Empty constructor for serialization purposes.
   */
  public MkCoPDirectoryEntry() {
    super();
  }

  /**
   * Provides a new MkCoPDirectoryEntry with the given parameters.
   *
   * @param objectID                  the id of the routing object
   * @param parentDistance            the distance from the object to its parent
   * @param nodeID                    the id of the underlying node
   * @param coveringRadius            the covering radius of the entry
   * @param conservativeApproximation the conservative approximation of the knn distances
   */
  public MkCoPDirectoryEntry(Integer objectID,
                             D parentDistance,
                             Integer nodeID,
                             D coveringRadius,
                             ApproximationLine conservativeApproximation) {
    super(objectID, parentDistance, nodeID, coveringRadius);
    this.conservativeApproximation = conservativeApproximation;
  }

  /**
   * Returns the conservative approximated knn distance of the entry.
   *
   * @param k                the parameter k of the knn distance
   * @param distanceFunction the distance function
   * @return the conservative approximated knn distance of the entry
   */
  public <O extends DatabaseObject> D approximateConservativeKnnDistance(int k, DistanceFunction<O, D> distanceFunction) {
    return conservativeApproximation.getApproximatedKnnDistance(k, distanceFunction);
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
   * Sets the conservative approximation line
   *
   * @param conservativeApproximation the conservative approximation line to be set
   */
  public void setConservativeKnnDistanceApproximation(ApproximationLine conservativeApproximation) {
    this.conservativeApproximation = conservativeApproximation;
  }

  /**
   * Calls the super method and writes the conservative approximation
   * of the knn distances of this entry to the specified stream.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(conservativeApproximation);
  }

  /**
   * Calls the super method and reads the the conservative approximation
   * of the knn distances of this entry from the specified input stream.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    conservativeApproximation = (ApproximationLine) in.readObject();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the object to be tested
   * @return true, if the super method returns true and
   *         o is an MkCoPLeafEntry and has the same
   *         conservative approximation as this entry.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    final MkCoPDirectoryEntry that = (MkCoPDirectoryEntry) o;

    return !(conservativeApproximation != null ? !conservativeApproximation.equals(that.conservativeApproximation) : that.conservativeApproximation != null);
  }
}
