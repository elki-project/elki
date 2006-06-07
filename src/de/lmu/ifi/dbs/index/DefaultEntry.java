package de.lmu.ifi.dbs.index;

/**
 * Default implementation of the entry interface.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DefaultEntry implements Entry {
  /**
   * Holds the id of the object (node or data object) represented by this entry.
   */
  private int id;

  /**
   * True, if this entry is a leaf entry (i.e. this entry represents a data object),
   * false otherwise.
   */
  private boolean isLeafEntry;

  /**
   * Provides a new DefaultEntry with the specified parameters.
   *
   * @param id          the id of this entry
   * @param isLeafEntry indicates if this entry is a leaf entry
   */
  public DefaultEntry(int id, boolean isLeafEntry) {
    this.id = id;
    this.isLeafEntry = isLeafEntry;
  }

  /**
   * Returns the id of this entry.
   *
   * @return the id of this entry
   */
  public Integer getID() {
    return id;
  }

  /**
   * Returns true if this entry is an entry in a leaf node
   * (i.e. this entry represents a data object),  false otherwise.
   *
   * @return true if this entry is an entry in a leaf node, false otherwise
   */
  public boolean isLeafEntry() {
    return isLeafEntry;
  }
}
