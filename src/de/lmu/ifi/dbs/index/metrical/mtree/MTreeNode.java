package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.Node;
import de.lmu.ifi.dbs.index.TreePath;
import de.lmu.ifi.dbs.index.TreePathComponent;
import de.lmu.ifi.dbs.persistent.PageFile;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a node in an M-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MTreeNode<O extends MetricalObject, D extends Distance<D>> implements Node {
  // todo: logger mit debug flag
  /**
   * Logger object for logging messages.
   */
  protected static Logger logger;

  /**
   * The level for logging messages.
   */
  private static Level level = Level.OFF;

  /**
   * The file storing the RTree.
   */
  protected PageFile<MTreeNode<O, D>> file;

  /**
   * The unique id if this node.
   */
  protected Integer nodeID;

  /**
   * The number of entries in this node.
   */
  protected int numEntries;

  /**
   * Indicates wether this node is a leaf node.
   */
  protected boolean isLeaf;

  /**
   * The entries (children) of this node.
   */
  protected MTreeEntry<D>[] entries;

  /**
   * The dirty flag of this page.
   */
  boolean dirty;

  /**
   * Empty constructor for Externalizable interface.
   */
  public MTreeNode() {
  }

  /**
   * Creates a new Node object.
   *
   * @param file     the file storing the RTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow) of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public MTreeNode(PageFile<MTreeNode<O, D>> file, int capacity, boolean isLeaf) {
    initLogger();
    this.file = file;
    this.nodeID = null;
    this.numEntries = 0;
    //noinspection unchecked
    this.entries = new MTreeEntry[capacity];
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
   * Returns the id of this node.
   *
   * @return the id of this node
   */
  public int getNodeID() {
    return nodeID;
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
  public Enumeration<TreePath> children(final TreePath parentPath) {
    return new Enumeration<TreePath>() {
      int count = 0;

      public boolean hasMoreElements() {
        return count < numEntries;
      }

      public TreePath nextElement() {
        synchronized (MTreeNode.this) {
          if (count < numEntries) {
            return parentPath.pathByAddingChild(new TreePathComponent(entries[count], count++));
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
  public MTreeEntry<D> getEntry(int index) {
    return entries[index];
  }

  /**
   * @see Object#equals(Object)
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MTreeNode)) return false;

    final MTreeNode<O, D> node = (MTreeNode<O, D>) o;
    if (nodeID != node.nodeID) return false;

    if (numEntries != node.numEntries)
      throw new RuntimeException("Should never happen! numEntries " +
                                 numEntries + " != " + node.numEntries);

    for (int i = 0; i < numEntries; i++) {
      MTreeEntry<D> e1 = entries[i];
      MTreeEntry<D> e2 = node.entries[i];
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
    this.numEntries = in.readInt();
    this.entries = (MTreeEntry<D>[]) in.readObject();
  }

  /**
   * Adds a new leaf entry to this node's children.
   * Note that this node must be a leaf node.
   *
   * @param newEntry the leaf entry to be added
   */
  public void addLeafEntry(MTreeLeafEntry<D> newEntry) {
    // directory node
    if (! isLeaf) {
      throw new UnsupportedOperationException("Node is not a leaf node!");
    }

    // leaf node
    entries[numEntries++] = newEntry;
  }

  /**
   * Adds a new directory entry to this node's children.
   * Note that this node must be a directory node.
   *
   * @param newEntry the directory entry to be added
   */
  public void addDirectoryEntry(MTreeDirectoryEntry<D> newEntry) {
    // leaf node
    if (isLeaf) {
      throw new UnsupportedOperationException("Node is a leaf node!");
    }

    // directory node
    entries[numEntries++] = newEntry;

    MTreeNode<O, D> node = file.readPage(newEntry.getNodeID());
    file.writePage(node);
  }

  /**
   * Splits the entries of this node into a new node at the specified splitPoint
   * and returns the newly created node.
   *
   * @param assignmentsToFirst  the assignment to this node
   * @param assignmentsToSecond the assignment to the new node
   * @return the newly created split node
   */
  public MTreeNode<O, D> splitEntries(List<MTreeEntry<D>> assignmentsToFirst, List<MTreeEntry<D>> assignmentsToSecond) {
    if (isLeaf) {
      MTreeNode<O, D> newNode = createNewLeafNode(entries.length);
      file.writePage(newNode);

      //noinspection unchecked
      this.entries = new MTreeEntry[entries.length];
      this.numEntries = 0;

      // assignments to this node
      String msg = "\n";
      for (MTreeEntry<D> entry : assignmentsToFirst) {
        msg += "n_" + getID() + " " + entry + "\n";
        entries[numEntries++] = entry;
      }

      // assignments to the new node
      for (MTreeEntry<D> entry : assignmentsToSecond) {
        msg += "n_" + newNode.getID() + " " + entry + "\n";
        newNode.entries[newNode.numEntries++] = entry;
      }
      logger.fine(msg);
      return newNode;
    }

    else {
      MTreeNode<O, D> newNode = createNewDirectoryNode(entries.length);
      file.writePage(newNode);

      //noinspection unchecked
      this.entries = new MTreeEntry[entries.length];
      this.numEntries = 0;

      String msg = "\n";
      for (MTreeEntry<D> e : assignmentsToFirst) {
        MTreeDirectoryEntry<D> entry = (MTreeDirectoryEntry<D>) e;
        msg += "n_" + getID() + " " + entry + "\n";
        addDirectoryEntry(entry);
      }

      for (MTreeEntry<D> e : assignmentsToSecond) {
        MTreeDirectoryEntry<D> entry = (MTreeDirectoryEntry<D>) e;
        msg += "n_" + newNode.getID() + " " + entry + "\n";
        newNode.addDirectoryEntry(entry);
      }
      logger.fine(msg);
      return newNode;
    }
  }

  /**
   * Tests this node (for debugging purposes).
   */
  public void test() {
    // leaf node
    if (isLeaf) {
      for (int i = 0; i < entries.length; i++) {
        MTreeEntry e = entries[i];
        if (i < numEntries && e == null)
          throw new RuntimeException("i < numEntries && entry == null");
        if (i >= numEntries && e != null)
          throw new RuntimeException("i >= numEntries && entry != null");
      }
    }

    // dir node
    else {
      MTreeNode<O, D> tmp = file.readPage(((MTreeDirectoryEntry<D>) entries[0]).getNodeID());
      boolean childIsLeaf = tmp.isLeaf();

      for (int i = 0; i < entries.length; i++) {
        MTreeDirectoryEntry<D> e = (MTreeDirectoryEntry<D>) entries[i];

        if (i < numEntries && e == null)
          throw new RuntimeException("i < numEntries && entry == null");

        if (i >= numEntries && e != null)
          throw new RuntimeException("i >= numEntries && entry != null");

        if (e != null) {
          MTreeNode<O, D> node = file.readPage(e.getNodeID());

          if (childIsLeaf && !node.isLeaf()) {
            throw new RuntimeException("Wrong Child in " + this + " at " + i +
                                       ": child id no leaf, but node is leaf!");
          }

          if (!childIsLeaf && node.isLeaf())
            throw new RuntimeException("Wrong Child in " + this + " at " + i +
                                       ": child id no leaf, but node is leaf!");
        }
      }
    }
  }

  /**
   * Tests, if the covering radii are correctly set.
   */
  public void testCoveringRadius(Integer objectID, D coveringRadius, DistanceFunction<O, D> distanceFunction) {
    for (int i = 0; i < numEntries; i++) {
      D dist = distanceFunction.distance(entries[i].getObjectID(), objectID);
      if (dist.compareTo(coveringRadius) > 0) {
        String msg = "dist > cr \n" +
                     dist + " > " + coveringRadius + "\n" +
                     "in " + this.toString() + " at entry " + entries[i] + "\n" +
                     "distance(" + entries[i].getObjectID() + " - " + objectID + ")" +
                     " >  cr(" + entries[i] + ")";

//        throw new RuntimeException(msg);
        if (dist instanceof NumberDistance) {
          double d1 = Double.parseDouble(dist.toString());
          double d2 = Double.parseDouble(coveringRadius.toString());
          if (Math.abs(d1 - d2) > 0.000000001)
            throw new RuntimeException(msg);
//            System.out.println("ALERT " + msg + "\n");
        }
        else
          throw new RuntimeException(msg);
//        System.out.println("ALERT " + msg + "\n");
      }
    }

  }

  /**
   * Tests, if the parent distances are correctly set.
   */
  public void testParentDistance(Integer objectID, DistanceFunction<O, D> distanceFunction) {
    for (int i = 0; i < numEntries; i++) {
      if (objectID != null) {
        D dist = distanceFunction.distance(entries[i].getObjectID(), objectID);
        if (! entries[i].getParentDistance().equals(dist)) {
          throw new RuntimeException("entry.pd != dist: \n" +
                                     entries[i].getParentDistance() + " != " + dist + "\n" +
                                     "in " + this.toString() + " at entry " + entries[i] + "\n" +
                                     "distance(" + entries[i].getObjectID() + " - " + objectID + ")");
        }
      }
      else {
        if (entries[i].getParentDistance() != null)
          throw new RuntimeException("entry.pd != null : \n" +
                                     entries[i].getParentDistance() + " != null \n" +
                                     "in " + this.toString() + " at entry " + entries[i] + "\n");
      }
    }
  }

  /**
   * Creates a new leaf node with the specified capacity.
   * Each subclass must override thois method.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected MTreeNode<O, D> createNewLeafNode(int capacity) {
    return new MTreeNode<O, D>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   * Each subclass must override thois method.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected MTreeNode<O, D> createNewDirectoryNode(int capacity) {
    return new MTreeNode<O, D>(file, capacity, false);
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(getClass().toString());
    logger.setLevel(level);
  }


}
