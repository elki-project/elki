package de.lmu.ifi.dbs.index.spatial.rstar;

import de.lmu.ifi.dbs.index.IndexPath;
import de.lmu.ifi.dbs.index.IndexPathComponent;
import de.lmu.ifi.dbs.index.spatial.*;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.persistent.PageFile;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 * Represents a node in a R*-Tree.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RTreeNode implements SpatialNode {
  /**
   * Holds the class specific debug status.
   */
  protected static boolean DEBUG = LoggingConfiguration.DEBUG;
//  protected static boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * The file storing the R*-Tree.
   */
  protected PageFile<RTreeNode> file;

  /**
   * The unique id if this node.
   */
  Integer nodeID;

  /**
   * The id of the parent of this node.
   */
  public Integer parentID;

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
  SpatialEntry[] entries;

  /**
   * The dirty flag of this node.
   */
  boolean dirty;

  /**
   * Empty constructor for Externalizable interface.
   */
  public RTreeNode() {
  }

  /**
   * Creates a new Node object.
   *
   * @param file     the file storing the RTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public RTreeNode(PageFile<RTreeNode> file, int capacity, boolean isLeaf) {
    this.file = file;
    this.nodeID = null;
    this.parentID = null;
    this.index = -1;
    this.numEntries = 0;
    this.entries = new SpatialEntry[capacity];
    this.isLeaf = isLeaf;
  }

  /**
   * @see SpatialComparable#getMin(int)
   */
  public double getMin(int dimension) {
    return mbr().getMin(dimension);
  }

  /**
   * @see SpatialComparable#getMax(int)
   */
  public double getMax(int dimension) {
    return mbr().getMax(dimension);
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
    // noinspection unchecked
    this.file = file;
  }

  /**
   * Returns true if this page is dirty, false otherwise.
   *
   * @return true if this page is dirty, false otherwise
   */
  public boolean isDirty() {
    return dirty;
  }

  /**
   * Sets the dirty flag of this page.
   *
   * @param dirty the dirty flag to be set
   */
  public void setDirty(boolean dirty) {
    this.dirty = dirty;
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
   * Returns an enumeration of the children paths of this node.
   *
   * @param parentPath the path to this node
   * @return an enumeration of the children paths of this node
   */
  public Enumeration<IndexPath> children(final IndexPath parentPath) {
    return new Enumeration<IndexPath>() {
      int count = 0;

      public boolean hasMoreElements() {
        return count < numEntries;
      }

      public IndexPath nextElement() {
        synchronized (RTreeNode.this) {
          if (count < numEntries) {
            return parentPath
            .pathByAddingChild(new IndexPathComponent(
            entries[count], count++));
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
  public SpatialEntry getEntry(int index) {
    return entries[index];
  }

  /**
   * @see Object#equals(Object)
   */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof RTreeNode))
      return false;

    final RTreeNode node = (RTreeNode) o;
    if (nodeID != node.nodeID)
      return false;

    if (parentID != node.parentID)
      throw new RuntimeException("Should never happen! parentID: "
                                 + parentID + " != " + node.parentID);

    if (index != node.index)
      throw new RuntimeException("Should never happen! index " + index
                                 + " != " + node.index);

    if (numEntries != node.numEntries)
      throw new RuntimeException("Should never happen! numEntries "
                                 + numEntries + " != " + node.numEntries);

    for (int i = 0; i < numEntries; i++) {
      SpatialEntry e1 = entries[i];
      SpatialEntry e2 = node.entries[i];
      if (!e1.equals(e2))
        throw new RuntimeException("Should never happen! entry " + e1
                                   + " != " + e2);
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
    if (isLeaf)
      return "LeafNode " + nodeID;
    else
      return "DirNode " + nodeID;
  }

  /**
   * The object implements the writeExternal method to save its contents by
   * calling the methods of DataOutput for its primitive values or calling the
   * writeObject method of ObjectOutput for objects, strings, and arrays.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   * @serialData Overriding methods should use this tag to describe the data
   * layout of this Externalizable object. List the sequence of
   * element types and, if possible, relate the element to a
   * public/protected field and/or method of this Externalizable
   * class.
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(nodeID);
    out.writeInt(parentID);
    out.writeInt(index);
    out.writeInt(numEntries);
    out.writeObject(entries);
  }

  /**
   * The object implements the readExternal method to restore its contents by
   * calling the methods of DataInput for primitive types and readObject for
   * objects, strings and arrays. The readExternal method must read the values
   * in the same sequence and with the same types as were written by
   * writeExternal.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException,
                                                  ClassNotFoundException {
    this.nodeID = in.readInt();
    this.parentID = in.readInt();
    this.index = in.readInt();
    this.numEntries = in.readInt();
    this.entries = (SpatialEntry[]) in.readObject();
  }

  /**
   * Returns the index of this node in its parent.
   *
   * @return the index of this node in its parent
   */
  public int getIndex() {
    return index;
  }

  /**
   * Adds a new leaf entry to this node's children. Note that this node must
   * be a leaf node.
   *
   * @param entry the entry to be added
   */
  protected void addLeafEntry(SpatialLeafEntry entry) {
    // directory node
    if (!isLeaf) {
      throw new UnsupportedOperationException("Node is not a leaf node!");
    }

    // leaf node
    entries[numEntries++] = entry;
  }

  /**
   * Adds a new node to this node's children. Note that this node must be a
   * directory node.
   *
   * @param node the node to be added
   */
  protected void addNode(RTreeNode node) {
    // leaf node
    if (isLeaf) {
      throw new UnsupportedOperationException("Node is a leaf node!");
    }

    // directory node
    entries[numEntries++] = createNewDirectoryEntry(node.getID(), node.mbr());

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
    // delete entry at index in entries
    System.arraycopy(entries, index + 1, entries, index, numEntries - index
                                                         - 1);
    entries[--numEntries] = null;

    // directory node
    if (!isLeaf) {

      for (int i = 0; i < numEntries; i++) {
        RTreeNode help = file.readPage(entries[i].getID());
        help.index = i;
        file.writePage(help);
      }
    }
  }

  /**
   * Initializes a reinsert operation. Deletes all entries in this node and
   * adds all entries from start index on to this node's children.
   *
   * @param start           the start index of the entries that will be reinserted
   * @param reInsertEntries the array of entries to be reinserted
   */
  protected void initReInsert(int start, ReinsertEntry[] reInsertEntries) {
    this.entries = new SpatialEntry[entries.length];
    this.numEntries = 0;

    if (isLeaf) {
      for (int i = start; i < reInsertEntries.length; i++) {
        SpatialLeafEntry entry = (SpatialLeafEntry) reInsertEntries[i].getEntry();
        entries[numEntries++] = createNewLeafEntry(entry.getID(), entry
        .getValues());
      }
    }

    else {
      for (int i = start; i < reInsertEntries.length; i++) {
        ReinsertEntry reInsertEntry = reInsertEntries[i];
        RTreeNode node = file
        .readPage(reInsertEntry.getEntry().getID());
        addNode(node);
      }
    }
  }

  /**
   * Splits the entries of this node into a new node at the specified
   * splitPoint and returns the newly created node.
   *
   * @param sorting    the sorted entries of this node
   * @param splitPoint the split point of the entries
   * @return the newly created split node
   */
  protected RTreeNode splitEntries(SpatialEntry[] sorting, int splitPoint) {
    if (isLeaf) {
      RTreeNode newNode = createNewLeafNode(entries.length);
      file.writePage(newNode);

      this.entries = new SpatialEntry[entries.length];
      this.numEntries = 0;

      StringBuffer msg = new StringBuffer("\n");
      for (int i = 0; i < splitPoint; i++) {
        if (DEBUG) {
          msg.append("n_").append(getID()).append(" ");
          msg.append(sorting[i]).append("\n");
        }
        entries[numEntries++] = sorting[i];
      }

      for (int i = 0; i < sorting.length - splitPoint; i++) {
        if (DEBUG) {
          msg.append("n_").append(newNode.getID()).append(" ");
          msg.append(sorting[splitPoint + i]).append("\n");
        }
        newNode.entries[newNode.numEntries++] = sorting[splitPoint + i];
      }

      if (DEBUG) {
        logger.fine(msg.toString());
      }
      return newNode;
    }

    else {
      RTreeNode newNode = createNewDirectoryNode(entries.length);
      file.writePage(newNode);

      this.entries = new SpatialEntry[entries.length];
      this.numEntries = 0;

      StringBuffer msg = new StringBuffer("\n");
      for (int i = 0; i < splitPoint; i++) {
        if (DEBUG) {
          msg.append("n_").append(getID()).append(" ");
          msg.append(sorting[i]).append("\n");
        }
        RTreeNode node = file.readPage(sorting[i].getID());
        addNode(node);
      }

      for (int i = 0; i < sorting.length - splitPoint; i++) {
        if (DEBUG) {
          msg.append("n_").append(newNode.getID()).append(" ");
          msg.append(sorting[splitPoint + i]).append("\n");
        }
        RTreeNode node = file.readPage(sorting[splitPoint + i].getID());
        newNode.addNode(node);
      }
      if (DEBUG) {
        logger.fine(msg.toString());
      }
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
        SpatialEntry e = entries[i];
        if (i < numEntries && e == null)
          throw new RuntimeException(
          "i < numEntries && entry == null");
        if (i >= numEntries && e != null)
          throw new RuntimeException(
          "i >= numEntries && entry != null");
      }
    }

    // dir node
    else {
      RTreeNode tmp = file.readPage(entries[0].getID());
      boolean childIsLeaf = tmp.isLeaf();

      for (int i = 0; i < entries.length; i++) {
        SpatialEntry e = entries[i];

        if (i < numEntries && e == null)
          throw new RuntimeException(
          "i < numEntries && entry == null");

        if (i >= numEntries && e != null)
          throw new RuntimeException(
          "i >= numEntries && entry != null");

        if (e != null) {
          RTreeNode node = file.readPage(e.getID());

          if (childIsLeaf && !node.isLeaf()) {
            for (int k = 0; k < getNumEntries(); k++) {
              SpatialEntry ee = entries[k];
              file.readPage(ee.getID());
            }

            throw new RuntimeException("Wrong Child in " + this
                                       + " at " + i);
          }

          if (!childIsLeaf && node.isLeaf())
            throw new RuntimeException(
            "Wrong Child: child id no leaf, but node is leaf!");

          if (node.parentID != nodeID)
            throw new RuntimeException("Wrong parent in node "
                                       + e.getID() + ": " + node.parentID + " != "
                                       + nodeID);

          if (node.index != i) {
            throw new RuntimeException("Wrong index in node "
                                       + node + ": ist " + node.index + " != " + i
                                       + " soll, parent is " + this);
          }

          MBR mbr = node.mbr();
          if (!e.getMBR().equals(mbr)) {
            String soll = node.mbr().toString();
            String ist = e.getMBR().toString();
            throw new RuntimeException("Wrong MBR in node "
                                       + getID() + " at index " + i + " (node "
                                       + e.getID() + ")" + "\nsoll: " + soll
                                       + ",\n ist: " + ist);
          }
          node.test();
        }
      }

      if (DEBUG) {
        logger.fine("DirNode " + getID() + " ok!");
      }
    }
  }

  /**
   * Creates a new leaf node with the specified capacity.
   * Subclasses may overwrite this method.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected RTreeNode createNewLeafNode(int capacity) {
    return new RTreeNode(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   * Subclasses may overwrite this method.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected RTreeNode createNewDirectoryNode(int capacity) {
    return new RTreeNode(file, capacity, false);
  }

  /**
   * Creates a new leaf entry with the specified parameters.
   * Subclasses may overwrite this method.
   *
   * @param id     the unique id of the underlying data object
   * @param values the values of the underlying data object
   */
  protected SpatialLeafEntry createNewLeafEntry(int id, double[] values) {
    return new SpatialLeafEntry(id, values);
  }

  /**
   * Creates a new leaf entry with the specified parameters.
   * Subclasses may overwrite this method.
   *
   * @param id  the unique id of the underlying spatial object
   * @param mbr the minmum bounding rectangle of the underlying spatial object
   */
  protected SpatialDirectoryEntry createNewDirectoryEntry(int id, MBR mbr) {
    return new SpatialDirectoryEntry(id, mbr);
  }

}
