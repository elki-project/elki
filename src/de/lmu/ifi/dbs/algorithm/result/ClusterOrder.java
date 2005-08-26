package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class representing the cluster order of the OPTICS algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ClusterOrder implements Result {
  /**
   * The database containing the objects.
   */
  private final Database database;

  /**
   * The distance function of the OPTICS algorithm.
   */
  private final DistanceFunction distanceFunction;

  /**
   * The cluster order.
   */
  private final List<COEntry> co;

  /**
   * The maximum reachability in this cluster order.
   */
  private Distance maxReachability;

  public ClusterOrder(final Database database, final DistanceFunction distanceFunction) {
    this.co = new ArrayList<COEntry>();
    this.database = database;
    this.distanceFunction = distanceFunction;
  }

  /**
   * Adds an object with the given predecessor and the given reachability to this cluster order.
   *
   * @param objectID      the id of the object to be added
   * @param predecessorID the id of the object's predecessor
   * @param reachability  the reachability of the object
   */
  public void add(Integer objectID, Integer predecessorID, Distance reachability) {
    co.add(new COEntry(objectID, predecessorID, reachability));

    if (! distanceFunction.isInfiniteDistance(reachability) &&
        (maxReachability == null || maxReachability.compareTo(reachability) < 0)) {
      maxReachability = reachability;
    }
  }

  /**
   * Returns the size of this cluster order.
   *
   * @return the size of this cluster order
   */
  public final int size() {
    return co.size();
  }

  /**
   * Writes the clustering result to the given file.
   *
   * @param out file, which designates the location to write the results,
   *            or which's name designates the prefix of any locations to write the results,
   *            or which could remain null to designate the standard-out as location for output.
   */
  public void output(File out) {
    PrintStream outStream;
    try {
      outStream = new PrintStream(new FileOutputStream(out));
    }
    catch (Exception e) {
      outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
    }

    for (COEntry entry : co) {
      final Distance reachability = ! distanceFunction.isInfiniteDistance(entry.reachability) ?
                                    entry.reachability :
                                    maxReachability.plus(maxReachability);
      outStream.println(entry.objectID + " " + reachability + " " +
                        database.getAssociation(Database.ASSOCIATION_ID_LABEL, entry.objectID));
    }

    outStream.flush();
  }

  /**
   * Returns a string representation of this cluster order.
   *
   * @return a string representation of this cluster order
   */
  public final String toString() {
    return Arrays.asList(co).toString();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the reference object with which to compare.
   * @return <code>true</code> if this object has the same attribute
   *         values as the o argument; <code>false</code> otherwise.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ClusterOrder other = (ClusterOrder) o;
    if (this.size() != other.size()) return false;

    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < co.size(); i++) {
      COEntry entry = co.get(i);
      COEntry otherEntry = other.co.get(i);
      if (!entry.equals(otherEntry)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for the object
   */
  public int hashCode() {
    return (co != null ? co.hashCode() : 0);
  }

  /**
   * Encapsulates an entry in the cluster order.
   */
  class COEntry {
    /**
     * The id of the entry.
     */
    Integer objectID;

    /**
     * The id of the entry's predecessor.
     */
    Integer predecessorID;

    /**
     * The reachability of the entry.
     */
    Distance reachability;

    /**
     * Creates a new entry with the specified parameters.
     *
     * @param objectID      the id of the entry
     * @param predecessorID the id of the entry's predecessor
     * @param reachability  the reachability of the entry
     */
    public COEntry(Integer objectID, Integer predecessorID, Distance reachability) {
      this.objectID = objectID;
      this.predecessorID = predecessorID;
      this.reachability = reachability;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return <code>true</code> if this object has the same attribute
     *         values as the o argument; <code>false</code> otherwise.
     */
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final COEntry coEntry = (COEntry) o;

      if (!objectID.equals(coEntry.objectID)) return false;

      if (predecessorID != null ?
          !predecessorID.equals(coEntry.predecessorID) :
          coEntry.predecessorID != null) return false;

      return reachability.equals(coEntry.reachability);
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for the object
     */
    public int hashCode() {
      int result;
      result = objectID.hashCode();
      result = 29 * result + (predecessorID != null ? predecessorID.hashCode() : 0);
      result = 29 * result + reachability.hashCode();
      return result;
    }
  }
}
