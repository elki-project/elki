package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.index.spatial.*;
import de.lmu.ifi.dbs.persistent.PageFile;

/**
 * The class DirectoryNode represents a directory node in a RTree index structure.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class DirectoryNode extends Node implements SpatialDirectoryNode {

  /**
   * Empty constructor for Externalizable interface.
   */
  public DirectoryNode() {
  }

  /**
   * Creates a new DirectoryNode object.
   *
   * @param file the file storing the RTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow) of this node
   */
  public DirectoryNode(PageFile<Node> file, int capacity) {
    super(file, capacity);
  }

  /**
   * @return false
   * @see de.lmu.ifi.dbs.index.spatial.SpatialNode#isLeaf()
   */
  public boolean isLeaf() {
    return false;
  }

  /**
   * Returns a string representation of this leaf node.
   *
   * @return a string representation of this leaf node
   */
  public String toString() {
    return "DirNode " + getID();
  }

  /**
   * Adds a new entry to this node's children.
   *
   * @param obj the entry to be added
   */
  protected void addEntry(SpatialObject obj) {
    Node node = (Node) obj;
    entries[numEntries++] = new DirectoryEntry(node.getID(), node.mbr());

    node.parentID = nodeID;
    node.index = numEntries - 1;
    file.writePage(node);
  }

  /**
   * Deletes an entry from this node's children.
   *
   * @param index the index of the entry to be deleted
   */
  protected void deleteEntry(int index) {
    System.arraycopy(entries, index + 1, entries, index, numEntries - index - 1);
    entries[--numEntries] = null;

    for (int i = 0; i < numEntries; i++) {
      Node help = file.readPage(entries[i].getID());
      help.index = i;
      file.writePage(help);
    }
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
      ReinsertEntry reInsertEntry = reInsertEntries[i];
      Node node = file.readPage(reInsertEntry.getEntry().getID());
      addEntry(node);
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
    DirectoryNode newNode = new DirectoryNode(file, entries.length);
    file.writePage(newNode);

    this.entries = new Entry[entries.length];
    this.numEntries = 0;

    String msg = "\n";
    for (int i = 0; i < splitPoint; i++) {
      msg += "n_" + getID() + " " + sorting[i] + "\n";
      Node node = file.readPage(sorting[i].getID());
      addEntry(node);
    }

    for (int i = 0; i < sorting.length - splitPoint; i++) {
      msg += "n_" + newNode.getID() + " " + sorting[splitPoint + i] + "\n";
      Node node = file.readPage(sorting[splitPoint + i].getID());
      newNode.addEntry(node);
    }
    logger.fine(msg);

    return newNode;
  }

  /**
   * Tests this node (for debugging purposes).
   */
  protected void test() {
    Node tmp = file.readPage(entries[0].getID());
    boolean childIsLeaf = tmp.isLeaf();

    for (int i = 0; i < entries.length; i++) {
      Entry e = entries[i];

      if (i < numEntries && e == null)
        throw new RuntimeException("i < numEntries && entry == null");

      if (i >= numEntries && e != null)
        throw new RuntimeException("i >= numEntries && entry != null");

      if (e != null) {
        Node node = file.readPage(e.getID());

        if (childIsLeaf && !node.isLeaf()) {
          for (int k = 0; k < getNumEntries(); k++) {
            Entry ee = entries[k];
            file.readPage(ee.getID());
          }

          throw new RuntimeException("Wrong Child in " + this + " at " + i);
        }

        if (!childIsLeaf && node.isLeaf())
          throw new RuntimeException("Wrong Child: child id no leaf, but node is leaf!");


        if (node.parentID != nodeID)
          throw new RuntimeException("Wrong parent in node " + e.getID() +
                                     ": " + node.parentID + " != " + nodeID);

        if (node.index != i) {
          throw new RuntimeException("Wrong index in node " + node +
                                     ": ist " + node.index + " != " + i +
                                     " soll, parent is " + this);
        }

        MBR mbr = node.mbr();
        if (!e.getMBR().equals(mbr)) {
          String soll = node.mbr().toString();
          String ist = e.getMBR().toString();
          throw new RuntimeException("Wrong MBR in node " + getID() + " at index "
                                     + i + " (node " + e.getID() + ")" +
                                     "\nsoll: " + soll + ",\n ist: " + ist);
        }
        node.test();
      }
    }

    logger.info("DirNode " + getID() + " ok!");
  }
}
