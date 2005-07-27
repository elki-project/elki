package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.index.spatial.MBR;
import de.lmu.ifi.dbs.index.spatial.SpatialDirectoryNode;
import de.lmu.ifi.dbs.index.spatial.SpatialObject;

/**
 * The class DirectoryNode represents a directory node in a RTree index structure.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class DirectoryNode extends Node implements SpatialDirectoryNode {

  /**
   * Creates a new DirectoryNode object.
   *
   * @param pageFile the PageFile storing the RTree
   */
  public DirectoryNode(PageFile pageFile) {
    super(pageFile);
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
    return "DirNode " + getPageID();
  }

  /**
   * Adds a new entry to this node's children.
   *
   * @param obj the entry to be added
   */
  protected void addEntry(SpatialObject obj) {
    Node node = (Node) obj;
    Entry entry = new Entry(node.getPageID(), node.mbr());
    this.setEntry(numEntries++, entry);

    node.parentID = pageID;
    node.index = numEntries - 1;
    file.writeNode(node);
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
      Node help = file.readNode(entries[i].getID());
      help.index = i;
      file.writeNode(help);
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
    // löschen und nach vorne schieben
    this.entries = new Entry[entries.length];
    this.numEntries = 0;
    for (int i = start; i < reInsertEntries.length; i++) {
      ReinsertEntry reInsertEntry = reInsertEntries[i];
      Node node = file.readNode(reInsertEntry.getID());
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
    DirectoryNode newNode = new DirectoryNode(file);
    file.writeNode(newNode);

    this.entries = new Entry[entries.length];
    this.numEntries = 0;

    String msg = "\n";
    for (int i = 0; i < splitPoint; i++) {
      msg += "n_" + getPageID() + " " + sorting[i] + "\n";
      Node node = file.readNode(sorting[i].getID());
      addEntry(node);
    }

    for (int i = 0; i < sorting.length - splitPoint; i++) {
      msg += "n_" + newNode.getPageID() + " " + sorting[splitPoint + i] + "\n";
      Node node = file.readNode(sorting[splitPoint + i].getID());
      newNode.addEntry(node);
    }
    logger.fine(msg);

    return newNode;
  }

  /**
   * Tests this node (for debugging purposes).
   */
  protected void test() {
    Node tmp = file.readNode(entries[0].getID());
    boolean childIsLeaf = tmp.isLeaf();

    for (int i = 0; i < entries.length; i++) {
      Entry e = entries[i];

      if (i < numEntries && e == null)
        throw new RuntimeException("i < numEntries && entry == null");

      if (i >= numEntries && e != null)
        throw new RuntimeException("i >= numEntries && entry != null");

      if (e != null) {
        Node node = file.readNode(e.getID());

        if (childIsLeaf && !node.isLeaf()) {
          Node[] nodes = new Node[getNumEntries()];

          for (int k = 0; k < getNumEntries(); k++) {
            Entry ee = entries[k];
            nodes[k] = file.readNode(ee.getID());
          }

          throw new RuntimeException("Wrong Child in " + this + " at " + i);
        }

        if (!childIsLeaf && node.isLeaf())
          throw new RuntimeException("Wrong Child: child id no leaf, but node is leaf!");


        if (node.parentID != pageID)
          throw new RuntimeException("Wrong parent in node " + e.getID() +
                                     ": " + node.parentID + " != " + pageID);

        if (node.index != i) {
          throw new RuntimeException("Wrong index in node " + node +
                                     ": ist " + node.index + " != " + i +
                                     " soll, parent is " + this);
        }

        MBR mbr = node.mbr();
        if (!e.getMBR().equals(mbr)) {
          String soll = node.mbr().toString();
          String ist = e.getMBR().toString();
          throw new RuntimeException("Wrong MBR in node " + getPageID() + " at index "
                                     + i + " (node " + e.getID() + ")" +
                                     "\nsoll: " + soll + ",\n ist: " + ist);
        }
        node.test();
      }
    }

    logger.info("DirNode " + getPageID() + " ok!");
  }


  /**
   * Sets the specified entry at the specified index in this node's children.
   * @param index the index of the entry to be set
   * @param entry the entry to be set
   */
  private void setEntry(int index, Entry entry) {
    if (entries.length == index) {
      int newCapacity = file.increaseRootNode() + entries.length;
      Entry[] tmp = new Entry[newCapacity];
      System.arraycopy(entries, 0, tmp, 0, entries.length);
      entries = tmp;
    }
    entries[index] = entry;
  }
}
