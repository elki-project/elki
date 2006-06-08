package de.lmu.ifi.dbs.index;

import de.lmu.ifi.dbs.distance.Distance;

/**
 * Helper class: encapsulates an entry in an Index and a distance value
 * belonging to this entry.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DistanceEntry<D extends Distance<D>> implements Comparable<DistanceEntry<D>> {
  /**
   * The entry of the Index.
   */
  private Entry entry;

  /**
   * The distance value belonging to the entry.
   */
  private D distance;

  /**
   * The index of the entry in its parent's child array.
   */
  private int index;

  /**
   * Constructs a new DistanceEntry object with the specified parameters.
   *
   * @param entry    the entry of the Index
   * @param distance the distance value belonging to the entry
   * @param index    the index of the entry in its parent' child array
   */
  public DistanceEntry(Entry entry, D distance, int index) {
    this.entry = entry;
    this.distance = distance;
    this.index = index;
  }

  /**
   * Returns the entry of the Index.
   *
   * @return the entry of the Index
   */
  public Entry getEntry() {
    return entry;
  }

  /**
   * Returns the distance value belonging to the entry.
   *
   * @return the distance value belonging to the entry
   */
  public D getDistance() {
    return distance;
  }

  /**
   * Returns the index of this entry in its parents child array.
   * @return  the index of this entry in its parents child array
   */
  public int getIndex() {
    return index;
  }


  /**
   * Compares this object with the specified object for order.
   *
   * @param o the Object to be compared.
   * @return a negative integer, zero, or a positive integer as this object is
   *         less than, equal to, or greater than the specified object.
   * @throws ClassCastException if the specified object's type prevents it from being
   *                            compared to this Object.
   */
  public int compareTo(DistanceEntry<D> o) {
    int comp = distance.compareTo(o.distance);
    if (comp != 0)
      return comp;

    return entry.getID().compareTo(o.entry.getID());
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return "" + entry.getID() + "(" + distance + ")";
  }
}
