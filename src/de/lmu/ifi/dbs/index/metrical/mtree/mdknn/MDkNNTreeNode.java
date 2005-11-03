package de.lmu.ifi.dbs.index.metrical.mtree.mdknn;

import de.lmu.ifi.dbs.index.metrical.mtree.MTreeNode;
import de.lmu.ifi.dbs.index.metrical.mtree.LeafEntry;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.persistent.PageFile;

/**
 * Represents a node in a MDkNN-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MDkNNTreeNode<O extends MetricalObject, D extends Distance> extends MTreeNode<O,D> {
  /**
   * Empty constructor for Externalizable interface.
   */
  public MDkNNTreeNode() {
  }

  /**
   * Creates a new Node object.
   *
   * @param file     the file storing the RTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow) of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public MDkNNTreeNode(PageFile<MTreeNode<O, D>> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * Adds a new leaf entry to this node's children.
   * Note that this node must be a leaf node.
   *
   * @param objectID       the id of the object to be added
   * @param parentDistance the distance of the object to be added to its parent
   */
  protected LeafEntry<D> addDataObject(Integer objectID, D parentDistance) {
    // directory node
    if (! isLeaf) {
      throw new UnsupportedOperationException("Node is not a leaf node!");
    }

    // leaf node
    // todo
    MDkNNLeafEntry<D> newEntry = new MDkNNLeafEntry<D>(objectID, parentDistance, null);
    entries[numEntries++] = newEntry;
    return newEntry;
  }

  /**
   * Adds a new node to this node's children.
   * Note that this node must be a directory node.
   *
   * @param node            the node to be added
   * @param routingObjectID the id of the routing object of the entry
   * @param parentDistance  the parent distance of the entry
   * @param coveringRadius  the covering radius of the entry
   */
  protected void addNode(MTreeNode<O, D> node, Integer routingObjectID, D parentDistance, D coveringRadius) {
    // leaf node
    if (isLeaf) {
      throw new UnsupportedOperationException("Node is a leaf node!");
    }

    // directory node
    // todo
    entries[numEntries++] = new MDkNNDirectoryEntry<D>(routingObjectID, parentDistance, null,
                                                       node.getID(), coveringRadius);

    MDkNNTreeNode<O,D> n = (MDkNNTreeNode<O,D>) node;
    n.parentID = nodeID;
    n.index = numEntries - 1;
    file.writePage(node);
  }
}
