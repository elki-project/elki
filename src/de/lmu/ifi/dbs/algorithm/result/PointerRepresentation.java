package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.algorithm.SLINK;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * Provides the result of the single link algorithm SLINK.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PointerRepresentation implements Result {
  /**
   * The values of the function Pi of the pointer representation.
   */
  private HashMap<Integer, Integer> pi = new HashMap<Integer, Integer>();

  /**
   * The values of the function Lambda of the pointer representation.
   */
  private HashMap<Integer, SLINK.SLinkDistance> lambda = new HashMap<Integer, SLINK.SLinkDistance>();

  /**
   * The distance function this pointer representation was computed with.
   */
  private DistanceFunction distanceFunction;

  /**
   * The database containing the objects.
   */
  protected Database database;

  /**
   * Creates a new pointer representation.
   *
   * @param pi               the values of the function Pi of the pointer representation
   * @param lambda           the values of the function Lambda of the pointer representation
   * @param distanceFunction the distance function this pointer representation was computed with
   * @param database         the database containing the objects
   */
  public PointerRepresentation(HashMap<Integer, Integer> pi, HashMap<Integer, SLINK.SLinkDistance> lambda,
                               DistanceFunction distanceFunction, Database database) {
    this.pi = pi;
    this.lambda = lambda;
    this.distanceFunction = distanceFunction;
    this.database = database;
  }

  /**
   * @see Result#output(java.io.File)
   */
  public void output(File out) {
    PrintStream outStream;
    try {
      outStream = new PrintStream(new FileOutputStream(out));
    }
    catch (Exception e) {
      outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
    }
    outStream.println(this.toString());
    outStream.flush();
  }

  /**
   * Returns a string representation of this pointer representation.
   *
   * @return a string representation of this pointer representation
   */
  public String toString() {
    StringBuffer result = new StringBuffer();

    SortedSet<Integer> keys = new TreeSet<Integer>(pi.keySet());
    for (Integer id : keys) {
      result.append("P(");
      result.append(id);
      result.append(") = ");
      result.append(pi.get(id));
      result.append("   L(");
      result.append(id);
      result.append(") = ");
      result.append(lambda.get(id));
      result.append("\n");
    }
    return result.toString();
  }

  /**
   * Returns the clustering result for a given distance threshold.
   *
   * @param distancePattern the pattern of the threshold
   * @return the clustering result: each element of the returned collection is a list of
   *         ids representing one cluster
   */
  public Collection<List<Integer>> getClusters(String distancePattern) {
    Distance distance = distanceFunction.valueOf(distancePattern);

    HashMap<Integer, List<Integer>> partitions = new HashMap<Integer, List<Integer>>();
    for (Integer id : pi.keySet()) {
      Integer partitionID = id;
      while (lambda.get(partitionID).getDistance().compareTo(distance) <= 0) {
        partitionID = pi.get(partitionID);
      }

      List<Integer> partition = partitions.get(partitionID);
      if (partition == null) {
        partition = new ArrayList<Integer>();
        partitions.put(partitionID, partition);
      }
      partition.add(id);
    }
    return partitions.values();
  }
}
