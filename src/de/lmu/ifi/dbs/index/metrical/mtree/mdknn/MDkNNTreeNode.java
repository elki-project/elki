package de.lmu.ifi.dbs.index.metrical.mtree.mdknn;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.index.metrical.mtree.Entry;
import de.lmu.ifi.dbs.index.metrical.mtree.MTreeNode;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.List;

/**
 * Represents a node in a MDkNN-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MDkNNTreeNode<O extends MetricalObject, D extends Distance> extends MTreeNode<O, D> {
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
   * @param newEntry the leaf entry to be added
   */
  protected void addLeafEntry(MDkNNLeafEntry<D> newEntry) {
    // directory node
    if (! isLeaf) {
      throw new UnsupportedOperationException("Node is not a leaf node!");
    }

    // leaf node
    entries[numEntries++] = newEntry;
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
  protected void addNode(MTreeNode<O, D> node, Integer routingObjectID,
                         D parentDistance, D coveringRadius, D knnDistance) {
    // leaf node
    if (isLeaf) {
      throw new UnsupportedOperationException("Node is a leaf node!");
    }

    // directory node
    entries[numEntries++] = new MDkNNDirectoryEntry<D>(routingObjectID,
                                                       parentDistance,
                                                       node.getID(),
                                                       coveringRadius,
                                                       knnDistance);

    MDkNNTreeNode<O, D> n = (MDkNNTreeNode<O, D>) node;
    n.parentID = nodeID;
    n.index = numEntries - 1;
    file.writePage(node);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected MTreeNode<O, D> createNewLeafNode(int capacity) {
    return new MDkNNTreeNode<O, D>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected MTreeNode<O, D> createNewDirectoryNode(int capacity) {
    return new MDkNNTreeNode<O, D>(file, capacity, false);
  }

  /**
   * Tests, if the covering radii are correctly set.
   */
  protected void testKNNDistance(D knnDist,
                                 DistanceFunction<O, D> distanceFunction) {

    for (int i = 0; i < numEntries; i++) {
      D dist;
      MDkNNEntry<D> e = (MDkNNEntry<D>) entries[i];
      dist = e.getKnnDistance();
      if (dist.compareTo(knnDist) > 0) {
        String msg = "dist > knnDist \n" +
                     dist + " > " + knnDist + "\n" +
                     "in " + this.toString() + " at entry " + entries[i] + "\n" +
                     "distance(" + entries[i].getObjectID() + ")" +
                     " >  knnDist(" + this.getNodeID() + ")";

//        throw new RuntimeException(msg);
        if (dist instanceof DoubleDistance) {
          double d1 = Double.parseDouble(dist.toString());
          double d2 = Double.parseDouble(knnDist.toString());
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

  protected D kNNDistance(DistanceFunction<O, D> distanceFunction) {
    D knnDist = distanceFunction.nullDistance();
    for (int i = 0; i < numEntries; i++) {
      MDkNNEntry<D> entry = (MDkNNEntry<D>) entries[i];
//      System.out.println("entry " + entry + ", kDist = " + entry.getKnnDistance());
      knnDist = Util.max(knnDist, entry.getKnnDistance());
    }
    return knnDist;
  }

  /**
   * Splits the entries of this node into a new node at the specified splitPoint
   * and returns the newly created node.
   *
   * @param assignmentsToFirst  the assignment to this node
   * @param assignmentsToSecond the assignment to the new node
   * @return the newly created split node
   */
  protected MDkNNTreeNode<O, D> splitEntries(List<Entry<D>> assignmentsToFirst,
                                             List<Entry<D>> assignmentsToSecond) {
    if (isLeaf) {
      MDkNNTreeNode<O, D> newNode = (MDkNNTreeNode<O, D>) createNewLeafNode(entries.length);
      file.writePage(newNode);

      //noinspection unchecked
      this.entries = new Entry[entries.length];
      this.numEntries = 0;

      // assignments to this node
      String msg = "\n";
      for (Entry<D> entry : assignmentsToFirst) {
        msg += "n_" + getID() + " " + entry + "\n";
        entries[numEntries++] = entry;
      }

      // assignments to the new node
      for (Entry<D> entry : assignmentsToSecond) {
        msg += "n_" + newNode.getID() + " " + entry + "\n";
        newNode.entries[newNode.numEntries++] = entry;
      }
      logger.fine(msg);
      return newNode;
    }

    else {
      MDkNNTreeNode<O, D> newNode = (MDkNNTreeNode<O, D>) createNewDirectoryNode(entries.length);
      file.writePage(newNode);

      //noinspection unchecked
      this.entries = new Entry[entries.length];
      this.numEntries = 0;

      String msg = "\n";
      for (Entry<D> e : assignmentsToFirst) {
        MDkNNDirectoryEntry<D> entry = (MDkNNDirectoryEntry<D>) e;
        msg += "n_" + getID() + " " + entry + "\n";
        MTreeNode<O, D> node = file.readPage(entry.getNodeID());
        addNode(node, entry.getObjectID(), entry.getParentDistance(),
                entry.getCoveringRadius(), entry.getKnnDistance());
      }

      for (Entry<D> e : assignmentsToSecond) {
        MDkNNDirectoryEntry<D> entry = (MDkNNDirectoryEntry<D>) e;
        msg += "n_" + newNode.getID() + " " + entry + "\n";
        MTreeNode<O, D> node = file.readPage(entry.getNodeID());
        newNode.addNode(node, entry.getObjectID(), entry.getParentDistance(),
                        entry.getCoveringRadius(), entry.getKnnDistance());
      }
      logger.fine(msg);
      return newNode;
    }
  }

}
