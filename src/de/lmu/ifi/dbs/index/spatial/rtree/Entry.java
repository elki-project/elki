package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.index.spatial.MBR;

/**
 * The class Entry represents an entry in a node of a RTree. An entry consists
 * of a pair of id (representing the unique id of the underlying spatial object)
 * and the minmum bounding rectangle of the underlying spatial object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class Entry {
  /**
   * The unique id of the underlying spatial object.
   */
  private final int id;

  /**
   * The minmum bounding rectangle of the underlying spatial object.
   */
  private MBR mbr;

  /**
   * Constructs a new Entry object with the given parameters.
   *
   * @param id  the unique id of the underlying spatial object
   * @param mbr the minmum bounding rectangle of the underlying spatial object
   */
  public Entry(int id, MBR mbr) {
    this.id = id;
    this.mbr = mbr;
  }

  /**
   * Returns the id of the underlying spatial object of this entry.
   *
   * @return the id of the underlying spatial object of this entry
   */
  public int getID() {
    return id;
  }

  /**
   * Returns the MBR of the underlying spatial object of this entry.
   *
   * @return the MBR of the underlying spatial object of this entry
   */
  public MBR getMBR() {
    return mbr;
  }

  /**
   * Sets the MBR of this entry.
   *
   * @param mbr the MBR to be set
   */
  public void setMBR(MBR mbr) {
    this.mbr = mbr;
  }

  /**
   * Returns the id as a string representation of this entry.
   *
   * @return a string representation of this entry
   */
  public String toString() {
    return "" + id + ", mbr " + mbr;
  }

  /**
   * @see Object#equals(Object)
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Entry)) return false;

    final Entry entry = (Entry) o;

    if (id != entry.id) return false;
    if (! mbr.equals(entry.mbr)) return false;

    return true;
  }

  /**
   * @see Object#hashCode()
   */
  public int hashCode() {
    return 29 * id + mbr.hashCode();
  }
}

