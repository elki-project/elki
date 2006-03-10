package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class representing the cluster order of the OPTICS algorithm.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ClusterOrder<O extends DatabaseObject, D extends Distance<D>> extends AbstractResult<O> {
  /**
   * The distance function of the OPTICS algorithm.
   */
  private final DistanceFunction<O, D> distanceFunction;

  /**
   * The cluster order.
   */
  private final List<COEntry> co;

  /**
   * The maximum reachability in this cluster order.
   */
  private D maxReachability;

  /**
   * Provides the cluster order of the OPTICS algorithm.
   *
   * @param database         the database containing the objects
   * @param distanceFunction the distance function of the OPTICS algorithm
   */
  public ClusterOrder(final Database<O> database, final DistanceFunction<O, D> distanceFunction) {
    super(database);
    this.co = new ArrayList<COEntry>();
    this.distanceFunction = distanceFunction;
  }

  /**
   * Adds an object with the given predecessor and the given reachability to
   * this cluster order.
   *
   * @param objectID      the id of the object to be added
   * @param predecessorID the id of the object's predecessor
   * @param reachability  the reachability of the object
   */
  public void add(Integer objectID, Integer predecessorID, D reachability) {
    co.add(new COEntry(objectID, predecessorID, reachability));

    if (!distanceFunction.isInfiniteDistance(reachability) && (maxReachability == null || maxReachability.compareTo(reachability) < 0))
    {
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
   * @param out           file, which designates the location to write the results, or
   *                      which's name designates the prefix of any locations to write
   *                      the results, or which could remain null to designate the
   *                      standard-out as location for output.
   * @param normalization unused since no values are printed out
   * @param settings
   * @see Result#output(File, Normalization, List)
   */
  public void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    PrintStream outStream;
    try {
      outStream = new PrintStream(new FileOutputStream(out));
    }
    catch (Exception e) {
      outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
    }
    output(outStream, normalization, settings);
  }

  public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    try {
      writeHeader(outStream, settings, null);

      for (COEntry entry : co) {
        if (maxReachability == null) maxReachability = distanceFunction.infiniteDistance();
        D reachability = !distanceFunction.isInfiniteDistance(entry.reachability) ? entry.reachability : maxReachability.plus(maxReachability);

        O object = normalization == null ? db.get(entry.objectID) : normalization.restore(db.get(entry.objectID));
        outStream.println(entry.objectID + " " + reachability + " " + object.toString() + " " + db.getAssociation(AssociationID.LABEL, entry.objectID));
      }

      outStream.flush();
    }
    catch (NonNumericFeaturesException e) {
      throw new UnableToComplyException(e);
    }
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
   * @return <code>true</code> if this object has the same attribute values
   *         as the o argument; <code>false</code> otherwise. todo:
   *         system.out wieder raus
   */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    final ClusterOrder<O, D> other = (ClusterOrder<O, D>) o;
    if (this.size() != other.size()) {
      System.out.println("wrong size");
      return false;
    }

    // noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < co.size(); i++) {
      COEntry entry = co.get(i);
      COEntry otherEntry = other.co.get(i);
      if (!entry.equals(otherEntry)) {
        System.out.println("index " + i + ": " + entry + " != " + otherEntry);
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
   * Returns the maximum reachability in this cluster order.
   *
   * @return the maximum reachability in this cluster order
   */
  public D getMaxReachability() {
    return maxReachability;
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
    D reachability;

    /**
     * Creates a new entry with the specified parameters.
     *
     * @param objectID      the id of the entry
     * @param predecessorID the id of the entry's predecessor
     * @param reachability  the reachability of the entry
     */
    public COEntry(Integer objectID, Integer predecessorID, D reachability) {
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
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      final COEntry coEntry = (COEntry) o;

      if (!objectID.equals(coEntry.objectID))
        return false;

      if (predecessorID != null ? !predecessorID.equals(coEntry.predecessorID) : coEntry.predecessorID != null)
        return false;

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

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
      return objectID + "(" + predecessorID + "," + reachability + ")";
    }
  }
}
