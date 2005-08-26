package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.index.spatial.*;
import de.lmu.ifi.dbs.persistent.Page;
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
abstract class Node implements SpatialNode, Page {
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
//  protected final transient RTreeFile file;
  PageFile<Node> file;

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
   * The entries (children) of this node.
   */
  Entry[] entries;

  /**
   * Empty constructor for Externalizable interface.
   */
  protected Node() {
  }

  /**
   * Creates a new Node object.
   *
   * @param file     the file storing the RTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow) of this node
   */
  public Node(PageFile<Node> file, int capacity) {
    initLogger();
    this.file = file;
    this.nodeID = null;
    this.parentID = null;
    this.index = -1;
    this.numEntries = 0;
    this.entries = new Entry[capacity];
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
        synchronized (Node.this) {
          if (count < numEntries) {
            return entries[count++];
//            if (isLeaf()) {
//              return new Data(entry.getID(), ((LeafEntry) entry).getValues(), nodeID);
//            }
//            else {
//              return file.readPage(entry.getID());
//            }
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
    if (!(o instanceof Node)) return false;

    final Node node = (Node) o;
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
  protected abstract void addEntry(SpatialObject obj);

  /**
   * Deletes an entry from this node's children.
   *
   * @param index the index of the entry to be deleted
   */
  protected abstract void deleteEntry(int index);

  /**
   * Initializes a reinsert operation. Deletes all entries in this
   * node and adds all entries from start index on to this node's children.
   *
   * @param start           the start index of the entries that will be reinserted
   * @param reInsertEntries the array of entries to be reinserted
   */
  protected abstract void initReInsert(int start, ReinsertEntry[] reInsertEntries);

  /**
   * Splits the entries of this node into a new node at the specified splitPoint
   * and returns the newly created node.
   *
   * @param sorting    the sorted entries of this node
   * @param splitPoint the split point of the entries
   * @return the newly created split node
   */
  protected abstract Node splitEntries(Entry[] sorting, int splitPoint);

  /**
   * Tests this node (for debugging purposes).
   */
  protected abstract void test();

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(getClass().toString());
    logger.setLevel(level);
  }

}
