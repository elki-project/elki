package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.spatial.MBR;

/**
 * The class ReinsertEntry defines an entry of a RTree that has to be reinserted after
 * deletion / insertion operations on the RTree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class ReinsertEntry extends Entry implements Comparable<ReinsertEntry> {
  /**
   * The center-distance of this entry's MBR and the MBR of its (current) parent.
   */
  private Distance dist;

  /**
   * Creates a new ReinsertEntry
   *
   * @param id   the id of the underlying spatial object
   * @param mbr  the minmum bounding rectangle of the underlying spatial object
   * @param dist the center-distance of this entry's MBR and
   *             the MBR of its (current) parent
   */
  public ReinsertEntry(int id, MBR mbr, Distance dist) {
    super(id, mbr);
    this.dist = dist;
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
    int comp = -1 * this.dist.compareTo(reinsertEntry.dist);
    if (comp != 0) return comp;
    return this.getID() - reinsertEntry.getID();
  }
}

