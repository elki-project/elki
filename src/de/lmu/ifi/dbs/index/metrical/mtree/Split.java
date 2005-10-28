package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.distance.Distance;

import java.util.List;

/**
 * Encapsulates the required parameters for a split of a node in a M-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class Split<D extends Distance> {
  /**
   * The id of the first promotion object.
   */
  Integer firstPromoted;

  /**
   * The id of the second promotion object.
   */
  Integer secondPromoted;

  /**
   * The first covering radius.
   */
  D firstCoveringRadius;

  /**
   * The second covering radius.
   */
  D secondCoveringRadius;

  /**
   * Entries assigned to first promotion object
   */
  List<Entry<D>> assignmentsToFirst;

  /**
   * Entries assigned to second promotion object
   */
  List<Entry<D>> assignmentsToSecond;

  /**
   * Creates a new split object.
   */
  public Split() {
  }

  protected class DistanceEntry implements Comparable<DistanceEntry> {
    Entry<D> entry;
    D distance;

    public DistanceEntry(Entry<D> entry, D distance) {
      this.entry = entry;
      this.distance = distance;
    }

    /**
     * Compares this object with the specified object for order.
     *
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *                            from being compared to this Object.
     */
    public int compareTo(DistanceEntry o) {
      int comp = distance.compareTo(o.distance);
      if (comp != 0) return comp;
      return entry.getObjectID().compareTo(o.entry.getObjectID());
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
      if (entry.isLeafEntry()) return "" + entry.getObjectID() + "(" + distance + ")";
      return "" + ((DirectoryEntry<D>) entry).getNodeID() + "(" + distance + ")";
    }
  }

}
