package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.caching.Page;
import de.lmu.ifi.dbs.index.spatial.SpatialNode;
import de.lmu.ifi.dbs.index.spatial.SpatialObject;
import de.lmu.ifi.dbs.index.spatial.MBR;

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
  protected static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  protected static Level level = Level.OFF;

  /**
   * The PageFile storing the RTree.
   */
  protected final transient PageFile file;

  /**
   * The unique id if this node.
   */
  protected int pageID;

  /**
   * The id of the parent of this node.
   */
  protected int parentID;

  /**
   * The index of this node in its parent node.
   */
  protected int index;

  /**
   * The number of entries in this node.
   */
  protected int numEntries;

  /**
   * The entries (children) of this node.
   */
  protected Entry[] entries;

  /**
   * Creates a new Node object.
   *
   * @param pageFile the PageFile storing the RTree
   */
  public Node(PageFile pageFile) {
    initLogger();
    this.file = pageFile;
    this.pageID = -1;
    this.parentID = -1;
    this.index = -1;
    this.numEntries = 0;
    this.entries = new Entry[file.getCapacity()];
  }

  /**
   * Returns the id of this node.
   *
   * @return the id of this node
   */
  public int getPageID() {
    return pageID;
  }

  /**
   * Returns the parent id of this node.
   *
   * @return the parent id of this node
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
  public Enumeration<SpatialObject> children() {
    return new Enumeration<SpatialObject>() {
      int count = 0;

      public boolean hasMoreElements() {
        return count < numEntries;
      }

      public SpatialObject nextElement() {
        synchronized (Node.this) {
          if (count < numEntries) {
            Entry entry = entries[count++];
            if (isLeaf()) {
              return new Data(entry.getID(), entry.getMBR().getMinClone(), pageID);
            }
            else {
              return file.readNode(entry.getID());
            }
          }
        }
        throw new NoSuchElementException();
      }
    };
  }

  /**
   * @see Object#equals(Object)
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Node)) return false;

    final Node node = (Node) o;
    if (pageID != node.pageID) return false;

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
    return pageID;
  }

  /**
   * Computes and returns the MBR of this node.
   * @return the MBR of this node
   */
  public MBR mbr() {
    int dim = entries[0].getMBR().getDimensionality();
    double[] min = entries[0].getMBR().getMinClone();
    double[] max = entries[0].getMBR().getMaxClone();

    for (int i = 1; i < numEntries; i++) {
      MBR mbr = entries[i].getMBR();
      for (int d = 0; d < dim; d++) {
        if (min[d] > mbr.getMin(d))
          min[d] = mbr.getMin(d);
        if (max[d] < mbr.getMax(d))
          max[d] = mbr.getMax(d);
      }
    }
    return new MBR(min, max);
  }

  public int getDimensionality() {
    return file.getDimensionality();
  }
  
//
//  protected Entry[] getEntries() {
//    return entries;
//  }
//
//  protected Entry getEntry(int index) {
//    return entries[index];
//  }

//  protected void setEntry(int index, Entry entry) {
//    this.entries[index] = (Entry) entry;
//  }

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
    logger = Logger.getLogger(Node.class.toString());
    logger.setLevel(level);
  }

}
