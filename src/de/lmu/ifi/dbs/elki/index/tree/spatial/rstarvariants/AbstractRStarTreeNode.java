package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants;

import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.index.tree.AbstractNode;
import de.lmu.ifi.dbs.elki.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;

import java.util.List;

/**
 * Abstract superclass for nodes in a R*-Tree.
 *
 * @author Elke Achtert 
 */
public abstract class AbstractRStarTreeNode<N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> extends AbstractNode<N, E> implements SpatialNode<N,E> {

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
   * @see de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode#mbr()
   */
  public HyperBoundingBox mbr() {
    E firstEntry = getEntry(0);
    if (firstEntry == null) return null;
    int dim = firstEntry.getMBR().getDimensionality();
    double[] min = firstEntry.getMBR().getMin();
    double[] max = firstEntry.getMBR().getMax();

    for (int i = 1; i < getNumEntries(); i++) {
      HyperBoundingBox mbr = getEntry(i).getMBR();
      for (int d = 1; d <= dim; d++) {
        if (min[d - 1] > mbr.getMin(d))
          min[d - 1] = mbr.getMin(d);
        if (max[d - 1] < mbr.getMax(d))
          max[d - 1] = mbr.getMax(d);
      }
    }
    return new HyperBoundingBox(min, max);
  }

  /**
   * @see de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode#getDimensionality()
   */
  public int getDimensionality() {
    return getEntry(0).getMBR().getDimensionality();
  }

  /**
   * Adjusts the parameters of the entry representing this node.
   *
   * @param entry  the entry representing this node
   */
  public void adjustEntry(E entry) {
    entry.setMBR(mbr());
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
  public final void test() {
    // leaf node
    if (isLeaf()) {
      for (int i = 0; i < getCapacity(); i++) {
        E e = getEntry(i);
        if (i < getNumEntries() && e == null)
          throw new RuntimeException("i < numEntries && entry == null");
        if (i >= getNumEntries() && e != null)
          throw new RuntimeException("i >= numEntries && entry != null");
      }
    }

    // dir node
    else {
      N tmp = getFile().readPage(getEntry(0).getID());
      boolean childIsLeaf = tmp.isLeaf();

      for (int i = 0; i < getCapacity(); i++) {
        E e = getEntry(i);

        if (i < getNumEntries() && e == null)
          throw new RuntimeException("i < numEntries && entry == null");

        if (i >= getNumEntries() && e != null)
          throw new RuntimeException("i >= numEntries && entry != null");

        if (e != null) {
          N node = getFile().readPage(e.getID());

          if (childIsLeaf && !node.isLeaf()) {
            for (int k = 0; k < getNumEntries(); k++) {
              getFile().readPage(getEntry(k).getID());
            }

            throw new RuntimeException("Wrong Child in " + this + " at " + i);
          }

          if (!childIsLeaf && node.isLeaf())
            throw new RuntimeException("Wrong Child: child id no leaf, but node is leaf!");

          //noinspection unchecked
          node.testEntry((N) this, i);
          node.test();
        }
      }

      if (this.debug) {
        debugFine("DirNode " + getID() + " ok!");
      }
    }
  }

  /**
   * Tests, if the parameters of the entry representing this node, are correctly set.
   * Subclasses may need to overwrite this method.
   *
   * @param parent the parent holding the entry representing this node
   * @param index  the index of the entry in the parents child arry
   */
  protected void testEntry(N parent, int index) {
    // test if mbr is correctly set
    E entry = parent.getEntry(index);
    HyperBoundingBox mbr = mbr();

    if (entry.getMBR() == null && mbr == null) return;
    if (!entry.getMBR().equals(mbr)) {
      String soll = mbr.toString();
      String ist = entry.getMBR().toString();
      throw new RuntimeException("Wrong MBR in node "
                                 + parent.getID() + " at index " + index + " (child "
                                 + entry + ")" + "\nsoll: " + soll
                                 + ",\n ist: " + ist);
    }
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
