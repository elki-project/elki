package de.lmu.ifi.dbs.index.metrical.mtree.util;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.metrical.mtree.DirectoryEntry;
import de.lmu.ifi.dbs.index.metrical.mtree.Entry;

/**
 * Helper class: encapsulates an entry in a M-Tree and a distance value belonging to this entry.
 * This class is used for splitting nodes.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DistanceEntry<D extends Distance> implements Comparable<DistanceEntry<D>> {
  /**
   * The entry of the M-Tree.
   */
  private Entry<D> entry;

  /**
   * The distance value belonging to the entry.
   */
  private D distance;

  /**
   * Constructs a new DistanceEntry object with the specified parameters.
   * @param entry the entry of the M-Tree
   * @param distance the distance value belonging to the entry
   */
  public DistanceEntry(Entry<D> entry, D distance) {
    this.entry = entry;
    this.distance = distance;
  }

  /**
   * Returns the entry of the M-Tree.
   * @return the entry of the M-Tree
   */
  public Entry<D> getEntry() {
    return entry;
  }

  /**
   * Returns the distance value belonging to the entry.
   * @return the distance value belonging to the entry
   */
  public D getDistance() {
    return distance;
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
  public int compareTo(DistanceEntry<D> o) {
    int comp = distance.compareTo(o.distance);
    if (comp != 0) return comp;

    if (entry.isLeafEntry() || o.entry.isLeafEntry())
      return entry.getObjectID().compareTo(o.entry.getObjectID());

    DirectoryEntry<D> dirEntry = (DirectoryEntry<D>) entry;
    DirectoryEntry<D> otherDirEntry = (DirectoryEntry<D>) o.entry;
    comp = dirEntry.getNodeID().compareTo(otherDirEntry.getObjectID());
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
