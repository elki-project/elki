package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.index.spatial.*;
import de.lmu.ifi.dbs.persistent.PageFile;

/**
 * The class LeafNode represents a leaf node in a RTree index structure.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class LeafNode extends Node implements SpatialLeafNode {

  /**
   * Empty constructor for Externalizable interface.
   */
  public LeafNode() {
  }

  /**
   * Creates a new LeafNode object.
   *
   * @param file the file storing the RTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow) of this node
   */
  public LeafNode(PageFile<Node> file, int capacity) {
    super(file, capacity);
  }

  /**
   * @return true
   * @see de.lmu.ifi.dbs.index.spatial.SpatialNode#isLeaf()
   */
  public boolean isLeaf() {
    return true;
  }

  /**
   * Returns a string representation of this leaf node.
   *
   * @return a string representation of this leaf node
   */
  public String toString() {
    return "LeafNode " + getID();
  }

  /**
   * Adds a new entry to this node's children.
   *
   * @param obj the entry to be added
   */
  protected void addEntry(SpatialObject obj) {
    SpatialData data = (SpatialData) obj;
    // add entry
    entries[numEntries++] = new LeafEntry(data.getObjectID(), data.getValues());
  }

  /**
   * Deletes an entry from this node's children.
   *
   * @param index the index of the entry to be deleted
   */
  protected void deleteEntry(int index) {
    // delete entry at index in entries
    System.arraycopy(entries, index + 1, entries, index, numEntries - index - 1);
    entries[--numEntries] = null;
  }

  /**
   * Initializes a reinsert operation. Deletes all entries in this
   * node and adds all entries from start index on to this node's children.
   *
   * @param start           the start index of the entries that will be reinserted
   * @param reInsertEntries the array of entries to be reinserted
   */
  protected void initReInsert(int start, ReinsertEntry[] reInsertEntries) {
    this.entries = new Entry[entries.length];
    this.numEntries = 0;
    for (int i = start; i < reInsertEntries.length; i++) {
      LeafEntry entry = (LeafEntry) reInsertEntries[i].getEntry();
      entries[numEntries++] = new LeafEntry(entry.getID(),
                                            entry.getValues());
    }
  }

  /**
   * Splits the entries of this node into a new node at the specified splitPoint
   * and returns the newly created node.
   *
   * @param sorting    the sorted entries of this node
   * @param splitPoint the split point of the entries
   * @return the newly created split node
   */
  protected Node splitEntries(Entry[] sorting, int splitPoint) {
    LeafNode newNode = new LeafNode(file, entries.length);
    file.writePage(newNode);

    this.entries = new Entry[entries.length];
    this.numEntries = 0;

    String msg = "\n";
    for (int i = 0; i < splitPoint; i++) {
      msg += "n_" + getID() + " " + sorting[i] + "\n";
      entries[numEntries++] = sorting[i];
    }

    for (int i = 0; i < sorting.length - splitPoint; i++) {
      msg += "n_" + newNode.getID() + " " + sorting[splitPoint + i] + "\n";
      newNode.entries[newNode.numEntries++] = sorting[splitPoint + i];
    }
    logger.fine(msg);

    return newNode;
  }

  /**
   * Tests this node (for debugging purposes).
   */
  protected void test() {
    for (int i = 0; i < entries.length; i++) {
      Entry e = entries[i];
      if (i < numEntries && e == null)
        throw new RuntimeException("i < numEntries && entry == null");
      if (i >= numEntries && e != null)
        throw new RuntimeException("i >= numEntries && entry != null");
    }
  }


}
