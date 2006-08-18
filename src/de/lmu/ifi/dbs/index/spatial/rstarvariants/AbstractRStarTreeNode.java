package de.lmu.ifi.dbs.index.spatial.rstarvariants;

import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.AbstractNode;
import de.lmu.ifi.dbs.index.DistanceEntry;
import de.lmu.ifi.dbs.index.Node;
import de.lmu.ifi.dbs.index.spatial.MBR;
import de.lmu.ifi.dbs.index.spatial.SpatialComparable;
import de.lmu.ifi.dbs.index.spatial.SpatialEntry;
import de.lmu.ifi.dbs.index.spatial.SpatialNode;
import de.lmu.ifi.dbs.persistent.PageFile;

import java.util.List;

/**
 * Abstract superclass for nodes in a R*-Tree.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractRStarTreeNode<N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> extends AbstractNode<N, E> implements SpatialNode<E> {

  /**
   * Empty constructor for Externalizable interface.
   */
  public AbstractRStarTreeNode() {
    super();
  }

  /**
   * Creates a new AbstractRStarTreeNode with the specified parameters.
   *
   * @param file     the file storing the R*-Tree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public AbstractRStarTreeNode(PageFile<N> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
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
   * @see de.lmu.ifi.dbs.index.spatial.SpatialNode#mbr()
   */
  public MBR mbr() {
    E firstEntry = getEntry(0);
    int dim = firstEntry.getMBR().getDimensionality();
    double[] min = firstEntry.getMBR().getMin();
    double[] max = firstEntry.getMBR().getMax();

    for (int i = 1; i < getNumEntries(); i++) {
      MBR mbr = getEntry(i).getMBR();
      for (int d = 1; d <= dim; d++) {
        if (min[d - 1] > mbr.getMin(d))
          min[d - 1] = mbr.getMin(d);
        if (max[d - 1] < mbr.getMax(d))
          max[d - 1] = mbr.getMax(d);
      }
    }
    return new MBR(min, max);
  }

  /**
   * @see de.lmu.ifi.dbs.index.spatial.SpatialNode#getDimensionality()
   */
  public int getDimensionality() {
    return getEntry(0).getMBR().getDimensionality();
  }

  /**
   * Adds a new leaf entry to this node's children
   * and returns the index of the entry in this node's children array.
   * An UnsupportedOperationException will be thrown if the entry is not a leaf entry or
   * this node is not a leaf node.
   *
   * @param entry the leaf entry to be added
   * @return the index of the entry in this node's children array
   * @throws UnsupportedOperationException if entry is not a leaf entry or
   *                                       this node is not a leaf node
   */
  protected int addLeafEntry(E entry) {
    // entry is not a leaf entry
    if (! entry.isLeafEntry()) {
      throw new UnsupportedOperationException("Entry is not a leaf entry!");
    }
    // this is a not a leaf node
    if (!isLeaf()) {
      throw new UnsupportedOperationException("Node is not a leaf node!");
    }

    // leaf node
    addEntry(entry);
    return getNumEntries() - 1;
  }

  /**
   * Adds a new directory entry to this node's children
   * and returns the index of the entry in this node's children array.
   * An UnsupportedOperationException will be thrown if the entry is not a directory entry or
   * this node is not a directory node.
   *
   * @param entry the directory entry to be added
   * @return the index of the entry in this node's children array
   * @throws UnsupportedOperationException if entry is not a directory entry or
   *                                       this node is not a directory node
   */
  public int addDirectoryEntry(E entry) {
    // entry is not a directory entry
    if (entry.isLeafEntry()) {
      throw new UnsupportedOperationException("Entry is not a directory entry!");
    }
    // this is a not a directory node
    if (isLeaf()) {
      throw new UnsupportedOperationException("Node is not a directory node!");
    }

    addEntry(entry);
    return getNumEntries() - 1;
  }

  /**
   * Initializes a reinsert operation. Deletes all entries in this node and
   * adds all entries from start index on to this node's children.
   *
   * @param start           the start index of the entries that will be reinserted
   * @param reInsertEntries the array of entries to be reinserted
   */
  protected <D extends Distance<D>> void initReInsert(int start, DistanceEntry<D, E>[] reInsertEntries) {
    deleteAllEntries();

    if (isLeaf()) {
      for (int i = start; i < reInsertEntries.length; i++) {
        addLeafEntry(reInsertEntries[i].getEntry());
      }
    }
    else {
      for (int i = start; i < reInsertEntries.length; i++) {
        addDirectoryEntry(reInsertEntries[i].getEntry());
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
  protected N splitEntries(List<E> sorting, int splitPoint) {
    StringBuffer msg = new StringBuffer("\n");

    if (isLeaf()) {
      N newNode = createNewLeafNode(getCapacity());
      getFile().writePage(newNode);

      deleteAllEntries();

      for (int i = 0; i < splitPoint; i++) {
        addLeafEntry(sorting.get(i));
        if (this.debug) {
          msg.append("n_").append(getID()).append(" ");
          msg.append(sorting.get(i)).append("\n");
        }
      }

      for (int i = 0; i < sorting.size() - splitPoint; i++) {
        newNode.addLeafEntry(sorting.get(splitPoint + i));
        if (this.debug) {
          msg.append("n_").append(newNode.getID()).append(" ");
          msg.append(sorting.get(splitPoint + i)).append("\n");
        }
      }
      if (this.debug) {
        debugFine(msg.toString());
      }
      return newNode;
    }

    else {
      N newNode = createNewDirectoryNode(getCapacity());
      getFile().writePage(newNode);

      deleteAllEntries();

      for (int i = 0; i < splitPoint; i++) {
        addDirectoryEntry(sorting.get(i));
        if (this.debug) {
          msg.append("n_").append(getID()).append(" ");
          msg.append(sorting.get(i)).append("\n");
        }
      }

      for (int i = 0; i < sorting.size() - splitPoint; i++) {
        newNode.addDirectoryEntry(sorting.get(splitPoint + i));
        if (this.debug) {
          msg.append("n_").append(newNode.getID()).append(" ");
          msg.append(sorting.get(splitPoint + i)).append("\n");
        }
      }
      if (this.debug) {
        debugFine(msg.toString());
      }
      return newNode;
    }
  }

  /**
   * Tests this node (for debugging purposes).
   */
  protected void test() {
    // leaf node
    if (isLeaf()) {
      for (int i = 0; i < getCapacity(); i++) {
        E e = getEntry(i);
        if (i < getNumEntries() && e == null)
          throw new RuntimeException(
              "i < numEntries && entry == null");
        if (i >= getNumEntries() && e != null)
          throw new RuntimeException(
              "i >= numEntries && entry != null");
      }
    }

    // dir node
    else {
      N tmp = getFile().readPage(getEntry(0).getID());
      boolean childIsLeaf = tmp.isLeaf();

      for (int i = 0; i < getCapacity(); i++) {
        E e = getEntry(i);

        if (i < getNumEntries() && e == null)
          throw new RuntimeException(
              "i < numEntries && entry == null");

        if (i >= getNumEntries() && e != null)
          throw new RuntimeException(
              "i >= numEntries && entry != null");

        if (e != null) {
          N node = getFile().readPage(e.getID());

          if (childIsLeaf && !node.isLeaf()) {
            for (int k = 0; k < getNumEntries(); k++) {
              SpatialEntry ee = getEntry(k);
              getFile().readPage(ee.getID());
            }

            throw new RuntimeException("Wrong Child in " + this
                                       + " at " + i);
          }

          if (!childIsLeaf && node.isLeaf())
            throw new RuntimeException(
                "Wrong Child: child id no leaf, but node is leaf!");

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

      if (this.debug) {
        debugFine("DirNode " + getID() + " ok!");
      }
    }
  }

  /**
   * Adjusts the parameters of the entry representing the specified node. Subclasses may need to
   * overwrite this method.
   *
   * @param node  the node
   * @param index the index of the entry representing the node in this node's entries array
   */
  protected void adjustEntry(N node, int index) {
    E entry = getEntry(index);
    MBR mbr = node.mbr();
    entry.setMBR(mbr);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  abstract protected N createNewLeafNode(int capacity);

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  abstract protected N createNewDirectoryNode(int capacity);
}
