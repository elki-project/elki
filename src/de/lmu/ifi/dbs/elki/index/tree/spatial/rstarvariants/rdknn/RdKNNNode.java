package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.Util;

/**
 * Represents a node in a RDkNN-Tree.
 *
 * @author Elke Achtert 
 */
public class RdKNNNode<D extends NumberDistance<D,N>, N extends Number> extends AbstractRStarTreeNode<RdKNNNode<D,N>, RdKNNEntry<D,N>> {
  private static final long serialVersionUID = 1;

  /**
   * Empty constructor for Externalizable interface.
   */
  public RdKNNNode() {
	  // empty constructor
  }

  /**
   * Creates a new RdKNNNode object.
   *
   * @param file     the file storing the RdKNN-Tree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public RdKNNNode(PageFile<RdKNNNode<D,N>> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * Computes and returns the aggregated knn distance of this node
   *
   * @return the aggregated knn distance of this node
   */
  protected D kNNDistance() {
    D result = getEntry(0).getKnnDistance();
    for (int i = 1; i < getNumEntries(); i++) {
      D knnDistance = getEntry(i).getKnnDistance();
      result = Util.max(result, knnDistance);
    }
    return result;
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected RdKNNNode<D,N> createNewLeafNode(int capacity) {
    return new RdKNNNode<D,N>(getFile(), capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected RdKNNNode<D,N> createNewDirectoryNode(int capacity) {
    return new RdKNNNode<D,N>(getFile(), capacity, false);
  }

  public void adjustEntry(RdKNNEntry<D,N> entry) {
    super.adjustEntry(entry);
    entry.setKnnDistance(kNNDistance());
  }

  /**
   * Tests, if the parameters of the entry representinmg this node, are correctly set.
   * Subclasses may need to overwrite this method.
   *
   * @param parent the parent holding the entry representing this node
   * @param index  the index of the entry in the parents child arry
   */
  protected void testEntry(RdKNNNode<D,N> parent, int index) {
    super.testEntry(parent, index);
    // test if knn distance is correctly set
    RdKNNEntry<D,N> entry = parent.getEntry(index);
    D knnDistance = kNNDistance();
    if (!entry.getKnnDistance().equals(knnDistance)) {
      String soll = knnDistance.toString();
      String ist = entry.getKnnDistance().toString();
      throw new RuntimeException("Wrong knnDistance in node "
                                 + parent.getID() + " at index " + index + " (child "
                                 + entry + ")" + "\nsoll: " + soll
                                 + ",\n ist: " + ist);
    }
  }

}
