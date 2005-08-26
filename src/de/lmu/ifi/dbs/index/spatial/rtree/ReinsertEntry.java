package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.spatial.Entry;

/**
 * The class ReinsertEntry defines an entry of a RTree that has to be
 * reinserted after deletion / insertion operations on the RTree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class ReinsertEntry implements Comparable<ReinsertEntry> {
  /**
   * The netry to be reinserted.
   */
  private Entry entry;

  /**
   * The center-distance of this entry's MBR and the MBR of its (current) parent.
   */
  private Distance distance;

  /**
   * Creates a new ReinsertEntry
   *
   * @param entry   the entry to be reinsert
   * @param dist the center-distance of this entry's MBR and
   *             the MBR of its (current) parent
   */
  public ReinsertEntry(Entry entry, Distance dist) {
    this.entry= entry;
    this.distance = dist;
  }

  /**
   * Compares the distance value of this ReinsertEntry with the distance
   * value of the specified ReinsertEntry o. A negative integer is returned
   * if this distance is greater than the other distance, a positive
   * integer is returned if this distance is lower than the other distance.
   * If both values are equal the ids of the two entries are compared.
   *
   * @param reinsertEntry the ReinsertEntry to be compared
   * @return a negative integer, zero, or a positive integer as this object
   *         is less than, equal to, or greater than the specified object.
   */
  public int compareTo(ReinsertEntry reinsertEntry) {
    int comp = -1 * this.distance.compareTo(reinsertEntry.distance);
    if (comp != 0) return comp;
    return this.entry.getID() - reinsertEntry.entry.getID();
  }

  /**
   * Returns the entry to be reinserted.
   * @return the entry to be reinserted
   */
  public Entry getEntry() {
    return entry;
  }
}

