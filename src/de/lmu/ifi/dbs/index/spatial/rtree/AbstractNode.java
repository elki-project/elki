package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.index.spatial.DirectoryEntry;
import de.lmu.ifi.dbs.index.spatial.Entry;
import de.lmu.ifi.dbs.index.spatial.LeafEntry;
import de.lmu.ifi.dbs.index.spatial.MBR;
import de.lmu.ifi.dbs.index.spatial.SpatialData;
import de.lmu.ifi.dbs.index.spatial.SpatialNode;
import de.lmu.ifi.dbs.index.spatial.SpatialObject;
import de.lmu.ifi.dbs.persistent.PageFile;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class for a node in a RTree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
abstract class AbstractNode implements SpatialNode {
  /**
   * Logger object for logging messages.
   */
  static Logger logger;

  /**
   * The level for logging messages.
   */
  static Level level = Level.OFF;

  /**
   * The file storing the RTree.
   */
  PageFile<AbstractNode> file;

  /**
   * The unique id if this node.
   */
  Integer nodeID;

  /**
   * The id of the parent of this node.
   */
  Integer parentID;

  /**
   * The index of this node in its parent node.
   */
  int index;

  /**
   * The number of entries in this node.
   */
  int numEntries;

  /**
   * Indicates wether this node is a leaf node.
   */
  boolean isLeaf;

  /**
   * The entries (children) of this node.
   */
  Entry[] entries;

  /**
   * Empty constructor for Externalizable interface.
   */
  protected AbstractNode() {
  }

  /**
   * Creates a new Node object.
   *
   * @param file     the file storing the RTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow) of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public AbstractNode(PageFile<AbstractNode> file, int capacity, boolean isLeaf) {
    initLogger();
    this.file = file;
    this.nodeID = null;
    this.parentID = null;
    this.index = -1;
    this.numEntries = 0;
    this.entries = new Entry[capacity];
    this.isLeaf = isLeaf;
  }

  /**
   * Returns the id of this node.
   *
   * @return the id of this node
   */
  public Integer getID() {
    return nodeID;
  }

  /**
   * Sets the unique id of this Page.
   *
   * @param id the id to be set
   */
  public void setID(int id) {
    this.nodeID = id;
  }

  /**
   * Sets the page file of this page.
   *
   * @param file the page file to be set
   */
  public void setFile(PageFile file) {
    //noinspection unchecked
    this.file = file;
  }

  /**
   * Returns the id of this node.
   *
   * @return the id of this node
   */
  public int getNodeID() {
    return nodeID;
  }

  /**
   * Returns the id of the parent node of this node.
   *
   * @return the id of the parent node of this node
   */
  public int getParentID() {
    return parentID;
  }

  /**
   * Returns the number of entries of this node.
   *
   * @return the number of entries of this node
   */
  public int getNumEntries() {
    return numEntries;
  }

  /**
   * Returns true if this node is a leaf node, false otherwise.
   *
   * @return true if this node is a leaf node, false otherwise
   */
  public boolean isLeaf() {
    return isLeaf;
  }

  /**
   * Returns an enumeration of the children of this node.
   *
   * @return an enumeration of the children of this node
   */
  public Enumeration<Entry> children() {
    return new Enumeration<Entry>() {
      int count = 0;

      public boolean hasMoreElements() {
        return count < numEntries;
      }

      public Entry nextElement() {
        synchronized (AbstractNode.this) {
          if (count < numEntries) {
            return entries[count++];
          }
        }
        throw new NoSuchElementException();
      }
    };
  }

  /**
   * Returns the entry at the specified index.
   *
   * @param index the index of the entry to be returned
   * @return the entry at the specified index
   */
  public Entry getEntry(int index) {
    return entries[index];
  }

  /**
   * @see Object#equals(Object)
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AbstractNode)) return false;

    final AbstractNode node = (AbstractNode) o;
    if (nodeID != node.nodeID) return false;

    if (parentID != node.parentID)
      throw new RuntimeException("Should never happen! parentID: " +
                                 parentID + " != " + node.parentID);

    if (index != node.index)
      throw new RuntimeException("Should never happen! index " +
                                 index + " != " + node.index);

    if (numEntries != node.numEntries)
      throw new RuntimeException("Should never happen! numEntries " +
                                 numEntries + " != " + node.numEntries);

    for (int i = 0; i < numEntries; i++) {
      Entry e1 = entries[i];
      Entry e2 = node.entries[i];
      if (!e1.equals(e2))
        throw new RuntimeException("Should never happen! entry " +
                                   e1 + " != " + e2);
    }

    return true;
  }

  /**
   * @see Object#hashCode()
   */
  public int hashCode() {
    return nodeID;
  }

  /**
   * Computes and returns the MBR of this node.
   *
   * @return the MBR of this node
   */
  public MBR mbr() {
    int dim = entries[0].getMBR().getDimensionality();
    double[] min = entries[0].getMBR().getMin();
    double[] max = entries[0].getMBR().getMax();

    for (int i = 1; i < numEntries; i++) {
      MBR mbr = entries[i].getMBR();
      for (int d = 1; d <= dim; d++) {
        if (min[d - 1] > mbr.getMin(d))
          min[d - 1] = mbr.getMin(d);
        if (max[d - 1] < mbr.getMax(d))
          max[d - 1] = mbr.getMax(d);
      }
    }
    return new MBR(min, max);
  }

  public int getDimensionality() {
    return entries[0].getMBR().getDimensionality();
  }

  /**
   * Returns a string representation of this node.
   *
   * @return a string representation of this node
   */
  public String toString() {
    if (isLeaf) return "LeafNode " + nodeID;
    else
      return "DirNode " + nodeID;
  }

  /**
   * The object implements the writeExternal method to save its contents
   * by calling the methods of DataOutput for its primitive values or
   * calling the writeObject method of ObjectOutput for objects, strings,
   * and arrays.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   * @serialData Overriding methods should use this tag to describe
   * the data layout of this Externalizable object.
   * List the sequence of element types and, if possible,
   * relate the element to a public/protected field and/or
   * method of this Externalizable class.
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(nodeID);
    out.writeInt(parentID);
    out.writeInt(index);
    out.writeInt(numEntries);
    out.writeObject(entries);
  }

  /**
   * The object implements the readExternal method to restore its
   * contents by calling the methods of DataInput for primitive
   * types and readObject for objects, strings and arrays.  The
   * readExternal method must read the values in the same sequence
   * and with the same types as were written by writeExternal.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.nodeID = in.readInt();
    this.parentID = in.readInt();
    this.index = in.readInt();
    this.numEntries = in.readInt();
    this.entries = (Entry[]) in.readObject();
  }

  /**
   * Adds a new entry to this node's children.
   *
   * @param obj the entry to be added
   */
  protected void addEntry(SpatialObject obj) {
    // leaf node
    if (isLeaf) {
      SpatialData data = (SpatialData) obj;
      entries[numEntries++] = new LeafEntry(data.getObjectID(), data.getValues());
    }

    // directory node
    else {
      AbstractNode node = (AbstractNode) obj;
      entries[numEntries++] = new DirectoryEntry(node.getID(), node.mbr());

      node.parentID = nodeID;
      node.index = numEntries - 1;
      file.writePage(node);
    }
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

    // directory node
    if (! isLeaf) {

      for (int i = 0; i < numEntries; i++) {
        AbstractNode help = file.readPage(entries[i].getID());
        help.index = i;
        file.writePage(help);
      }
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

    if (isLeaf) {
      for (int i = start; i < reInsertEntries.length; i++) {
        LeafEntry entry = (LeafEntry) reInsertEntries[i].getEntry();
        entries[numEntries++] = new LeafEntry(entry.getID(),
                                              entry.getValues());
      }
    }

    else {
      for (int i = start; i < reInsertEntries.length; i++) {
        ReinsertEntry reInsertEntry = reInsertEntries[i];
        AbstractNode node = file.readPage(reInsertEntry.getEntry().getID());
        addEntry(node);
      }
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
  protected AbstractNode splitEntries(Entry[] sorting, int splitPoint) {
    if (isLeaf) {
      AbstractNode newNode = createNewLeafNode(entries.length);
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

    else {
      AbstractNode newNode = createNewDirectoryNode(entries.length);
      file.writePage(newNode);

      this.entries = new Entry[entries.length];
      this.numEntries = 0;

      String msg = "\n";
      for (int i = 0; i < splitPoint; i++) {
        msg += "n_" + getID() + " " + sorting[i] + "\n";
        AbstractNode node = file.readPage(sorting[i].getID());
        addEntry(node);
      }

      for (int i = 0; i < sorting.length - splitPoint; i++) {
        msg += "n_" + newNode.getID() + " " + sorting[splitPoint + i] + "\n";
        AbstractNode node = file.readPage(sorting[splitPoint + i].getID());
        newNode.addEntry(node);
      }
      logger.fine(msg);
      return newNode;
    }
  }

  /**
   * Tests this node (for debugging purposes).
   */
  protected void test() {
    // leaf node
    if (isLeaf) {
      for (int i = 0; i < entries.length; i++) {
        Entry e = entries[i];
        if (i < numEntries && e == null)
          throw new RuntimeException("i < numEntries && entry == null");
        if (i >= numEntries && e != null)
          throw new RuntimeException("i >= numEntries && entry != null");
      }
    }

    // dir node
    else {
      AbstractNode tmp = file.readPage(entries[0].getID());
      boolean childIsLeaf = tmp.isLeaf();

      for (int i = 0; i < entries.length; i++) {
        Entry e = entries[i];

        if (i < numEntries && e == null)
          throw new RuntimeException("i < numEntries && entry == null");

        if (i >= numEntries && e != null)
          throw new RuntimeException("i >= numEntries && entry != null");

        if (e != null) {
          AbstractNode node = file.readPage(e.getID());

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

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected abstract AbstractNode createNewLeafNode(int capacity);

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected abstract AbstractNode createNewDirectoryNode(int capacity);


  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(getClass().toString());
    logger.setLevel(level);
  }

}
