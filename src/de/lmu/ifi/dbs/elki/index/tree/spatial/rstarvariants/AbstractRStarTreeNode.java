package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.AbstractNode;
import de.lmu.ifi.dbs.elki.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Abstract superclass for nodes in a R*-Tree.
 * 
 * @author Elke Achtert
 * @param <N> Node type
 * @param <E> Entry type
 */
public abstract class AbstractRStarTreeNode<N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> extends AbstractNode<E> implements SpatialNode<N, E> {
  /**
   * Empty constructor for Externalizable interface.
   */
  public AbstractRStarTreeNode() {
    super();
  }

  /**
   * Creates a new AbstractRStarTreeNode with the specified parameters.
   * 
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   * @param eclass Entry class, to initialize array storage
   */
  public AbstractRStarTreeNode(int capacity, boolean isLeaf, Class<? super E> eclass) {
    super(capacity, isLeaf, eclass);
  }

  @Override
  public double getMin(int dimension) {
    double min = getEntry(0).getMin(dimension);
    for(int i = 1; i < getNumEntries(); i++) {
      min = Math.min(min, getEntry(i).getMin(dimension));
    }
    return min;
  }

  @Override
  public double getMax(int dimension) {
    double max = getEntry(0).getMax(dimension);
    for(int i = 1; i < getNumEntries(); i++) {
      max = Math.min(max, getEntry(i).getMax(dimension));
    }
    return max;
  }

  /**
   * Recomputing the MBR is rather expensive.
   * 
   * @return MBR
   */
  public HyperBoundingBox computeMBR() {
    E firstEntry = getEntry(0);
    if(firstEntry == null) {
      return null;
    }
    int dim = firstEntry.getDimensionality();
    // Note: we deliberately get a cloned copy here, since we will modify it.
    double[] min = SpatialUtil.getMin(firstEntry);
    double[] max = SpatialUtil.getMax(firstEntry);

    for(int i = 1; i < getNumEntries(); i++) {
      SpatialComparable mbr = getEntry(i);
      for(int d = 1; d <= dim; d++) {
        if(min[d - 1] > mbr.getMin(d)) {
          min[d - 1] = mbr.getMin(d);
        }
        if(max[d - 1] < mbr.getMax(d)) {
          max[d - 1] = mbr.getMax(d);
        }
      }
    }
    return new HyperBoundingBox(min, max);
  }

  @Override
  public int getDimensionality() {
    return getEntry(0).getDimensionality();
  }

  /**
   * Adjusts the parameters of the entry representing this node.
   * 
   * @param entry the entry representing this node
   */
  public void adjustEntry(E entry) {
    ((SpatialDirectoryEntry)entry).setMBR(computeMBR());
  }

  /**
   * Adjusts the parameters of the entry representing this node. Only applicable
   * if one object was inserted or the size of an existing node increased.
   * 
   * @param entry the entry representing this node
   * @param responsibleMBR the MBR of the object or node which is responsible
   *        for the call of the method
   * @return the MBR of the new Node
   */
  public E adjustEntryIncremental(E entry, SpatialComparable responsibleMBR) {
    ((SpatialDirectoryEntry)entry).setMBR(SpatialUtil.union(entry, responsibleMBR));
    return entry;
  }

  /**
   * * Initializes a reinsert operation. Deletes all entries in this node and
   * adds all entries from start index on to this node's children.
   * 
   * @param start the start index of the entries that will be reinserted
   * @param reInsertEntries the array of entries to be reinserted
   */
  protected <D extends Distance<D>> void initReInsert(int start, DistanceEntry<D, E>[] reInsertEntries) {
    deleteAllEntries();

    if(isLeaf()) {
      for(int i = start; i < reInsertEntries.length; i++) {
        addLeafEntry(reInsertEntries[i].getEntry());
      }
    }
    else {
      for(int i = start; i < reInsertEntries.length; i++) {
        addDirectoryEntry(reInsertEntries[i].getEntry());
      }
    }
  }

  /**
   * Tests this node (for debugging purposes).
   */
  @SuppressWarnings("unchecked")
  public final void integrityCheck(PageFile<N> pagefile) {
    // leaf node
    if(isLeaf()) {
      for(int i = 0; i < getCapacity(); i++) {
        E e = getEntry(i);
        if(i < getNumEntries() && e == null) {
          throw new RuntimeException("i < numEntries && entry == null");
        }
        if(i >= getNumEntries() && e != null) {
          throw new RuntimeException("i >= numEntries && entry != null");
        }
      }
    }

    // dir node
    else {
      N tmp = pagefile.readPage(getEntry(0).getEntryID());
      boolean childIsLeaf = tmp.isLeaf();

      for(int i = 0; i < getCapacity(); i++) {
        E e = getEntry(i);

        if(i < getNumEntries() && e == null) {
          throw new RuntimeException("i < numEntries && entry == null");
        }

        if(i >= getNumEntries() && e != null) {
          throw new RuntimeException("i >= numEntries && entry != null");
        }

        if(e != null) {
          N node = pagefile.readPage(e.getEntryID());

          if(childIsLeaf && !node.isLeaf()) {
            for(int k = 0; k < getNumEntries(); k++) {
              pagefile.readPage(getEntry(k).getEntryID());
            }

            throw new RuntimeException("Wrong Child in " + this + " at " + i);
          }

          if(!childIsLeaf && node.isLeaf()) {
            throw new RuntimeException("Wrong Child: child id no leaf, but node is leaf!");
          }

          node.integrityCheckParameters((N) this, i);
          node.integrityCheck(pagefile);
        }
      }

      if(LoggingConfiguration.DEBUG) {
        Logger.getLogger(this.getClass().getName()).fine("DirNode " + getPageID() + " ok!");
      }
    }
  }

  /**
   * Tests, if the parameters of the entry representing this node, are correctly
   * set. Subclasses may need to overwrite this method.
   * 
   * @param parent the parent holding the entry representing this node
   * @param index the index of the entry in the parents child array
   */
  protected void integrityCheckParameters(N parent, int index) {
    // test if mbr is correctly set
    E entry = parent.getEntry(index);
    HyperBoundingBox mbr = computeMBR();

    if(/* entry.getMBR() == null && */mbr == null) {
      return;
    }
    if(!SpatialUtil.equals(entry, mbr)) {
      String soll = mbr.toString();
      String ist = new HyperBoundingBox(entry).toString();
      throw new RuntimeException("Wrong MBR in node " + parent.getPageID() + " at index " + index + " (child " + entry + ")" + "\nsoll: " + soll + ",\n ist: " + ist);
    }
  }

  /**
   * Calls the super method and writes the id of this node, the numEntries and
   * the entries array to the specified stream.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    // TODO: do we need to write/read the capacity?
    out.writeInt(entries.length);
    for(E entry : entries) {
      if(entry == null) {
        break;
      }
      entry.writeExternal(out);
    }
  }

  /**
   * Reads the id of this node, the numEntries and the entries array from the
   * specified stream.
   * 
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  @Override
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);

    // TODO: do we need to write/read the capacity?
    final int capacity = in.readInt();
    if(isLeaf) {
      entries = (E[]) new SpatialPointLeafEntry[capacity];
      for(int i = 0; i < numEntries; i++) {
        SpatialPointLeafEntry s = new SpatialPointLeafEntry();
        s.readExternal(in);
        entries[i] = (E) s;
      }
    }
    else {
      entries = (E[]) new SpatialDirectoryEntry[capacity];
      for(int i = 0; i < numEntries; i++) {
        SpatialDirectoryEntry s = new SpatialDirectoryEntry();
        s.readExternal(in);
        entries[i] = (E) s;
      }
    }
  }
}