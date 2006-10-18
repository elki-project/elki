package de.lmu.ifi.dbs.algorithm.result.clustering;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.algorithm.result.AbstractResult;

/**
 * A class representing the cluster order of the OPTICS algorithm.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ClusterOrder<O extends DatabaseObject, D extends Distance<D>>
    extends AbstractResult<O> {

  /**
   * The distance function of the OPTICS algorithm.
   */
  private final DistanceFunction<O, D> distanceFunction;

  /**
   * The cluster order.
   */
  private final List<ClusterOrderEntry<D>> co;

  /**
   * The maximum reachability in this cluster order.
   */
  private D maxReachability;

  /**
   * Provides the cluster order of the OPTICS algorithm.
   *
   * @param database
   *            the database containing the objects
   * @param distanceFunction
   *            the distance function of the OPTICS algorithm
   */
  public ClusterOrder(final Database<O> database,
                      final DistanceFunction<O, D> distanceFunction) {
    super(database);
    this.co = new ArrayList<ClusterOrderEntry<D>>();
    this.distanceFunction = distanceFunction;
  }

  /**
   * Adds an object with the given predecessor and the given reachability to
   * this cluster order.
   *
   * @param objectID
   *            the id of the object to be added
   * @param predecessorID
   *            the id of the object's predecessor
   * @param reachability
   *            the reachability of the object
   */
  public void add(Integer objectID, Integer predecessorID, D reachability) {
    co.add(new ClusterOrderEntry<D>(objectID, predecessorID, reachability));

    if (!distanceFunction.isInfiniteDistance(reachability)
        && (maxReachability == null || maxReachability
            .compareTo(reachability) < 0)) {
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
   * @param out
   *            file, which designates the location to write the results, or
   *            which's name designates the prefix of any locations to write
   *            the results, or which could remain null to designate the
   *            standard-out as location for output.
   * @param normalization
   *            unused since no values are printed out
   * @param settings
   * @see de.lmu.ifi.dbs.algorithm.result.Result#output(File, Normalization, List)
   */
  public void output(File out, Normalization<O> normalization,
                     List<AttributeSettings> settings) throws UnableToComplyException {
    PrintStream outStream;
    try {
      outStream = new PrintStream(new FileOutputStream(out));
    } catch (Exception e) {
      outStream = new PrintStream(
          new FileOutputStream(FileDescriptor.out));
    }
    output(outStream, normalization, settings);
  }

  public void output(PrintStream outStream, Normalization<O> normalization,
                     List<AttributeSettings> settings) throws UnableToComplyException {
    try {
      writeHeader(outStream, settings, null);

      for (ClusterOrderEntry<D> entry : co) {
        if (maxReachability == null)
          maxReachability = distanceFunction.infiniteDistance();
        D reachability = !distanceFunction.isInfiniteDistance(entry
            .getReachability()) ? entry.getReachability()
                                : maxReachability.plus(maxReachability);

        O object = normalization == null ? db.get(entry.getID())
                   : normalization.restore(db.get(entry.getID()));
        outStream.println(entry.getID()
                          + " "
                          + reachability
                          + " "
                          + entry.getPredecessorID()
                          + " "
                          + object.toString()
                          + " "
                          + db.getAssociation(AssociationID.LABEL, entry.getID()));
      }

      outStream.flush();
    } catch (NonNumericFeaturesException e) {
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
   * @param o
   *            the reference object with which to compare.
   * @return <code>true</code> if this object has the same attribute values
   *         as the o argument; <code>false</code> otherwise.
   */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    final ClusterOrder<O, D> other = (ClusterOrder<O, D>) o;
    if (this.size() != other.size()) {
      return false;
    }

    // noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < co.size(); i++) {
      ClusterOrderEntry<D> entry = co.get(i);
      ClusterOrderEntry<D> otherEntry = other.co.get(i);
      if (!entry.equals(otherEntry)) {
        if (this.debug) {
          debugFine("index " + i + ": " + entry + " != "
                    + otherEntry);
        }
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
   * Returns an iterator over the elements in this cluster order in proper
   * sequence.
   *
   * @return an iterator over the elements in this cluster order in proper
   *         sequence.
   */
  public Iterator<ClusterOrderEntry<D>> iterator() {
    return co.iterator();
  }
}
