package de.lmu.ifi.dbs.index.metrical.mtreevariants;

import java.util.List;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.AbstractNode;
import de.lmu.ifi.dbs.persistent.PageFile;

/**
 * Represents a node in an M-Tree.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MTreeNode<O extends DatabaseObject, D extends Distance<D>, N extends MTreeNode<O, D, N, E>, E extends MTreeEntry<D>> extends AbstractNode<N, E> {

  /**
   * Empty constructor for Externalizable interface.
   */
  public MTreeNode() {
  }

   /**
   * Creates a new MTreeNode with the specified parameters.
   *
   * @param file     the file storing the M-Tree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public MTreeNode(PageFile<N> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * Adds a new leaf entry to this node's children. Note that this node must
   * be a leaf node.
   *
   * @param entry the leaf entry to be added
   */
  public void addLeafEntry(E entry) {
    // directory entry
    if (! entry.isLeafEntry()) {
      throw new UnsupportedOperationException("Entry is not a leaf entry!");
    }

    // directory node
    if (!isLeaf()) {
      throw new UnsupportedOperationException("Node is not a leaf node!");
    }

    super.addEntry(entry);
  }

  /**
   * Adds a new directory entry to this node's children. Note that this node
   * must be a directory node.
   *
   * @param entry the directory entry to be added
   */
  public void addDirectoryEntry(E entry) {
    // leaf entry
    if (entry.isLeafEntry()) {
      throw new UnsupportedOperationException("Entry is not a directory entry!");
    }

    // leaf node
    if (isLeaf()) {
      throw new UnsupportedOperationException("Node is a leaf node!");
    }

    super.addEntry(entry);
  }

  /**
   * Splits the entries of this node into a new node at the specified
   * splitPoint and returns the newly created node.
   *
   * @param assignmentsToFirst  the assignment to this node
   * @param assignmentsToSecond the assignment to the new node
   * @return the newly created split node
   */
  public N splitEntries(List<E> assignmentsToFirst,
                        List<E> assignmentsToSecond) {

    StringBuffer msg = new StringBuffer("\n");

    if (isLeaf()) {
      N newNode = createNewLeafNode(getCapacity());
      getFile().writePage(newNode);

      deleteAllEntries();

      // assignments to this node
      for (E entry : assignmentsToFirst) {
        if (this.debug) {
          msg.append("n_" + getID() + " " + entry + "\n");
        }
        addLeafEntry(entry);
      }

      // assignments to the new node
      for (E entry : assignmentsToSecond) {
        if (this.debug) {
          msg.append("n_" + newNode.getID() + " " + entry + "\n");
        }
        newNode.addLeafEntry(entry);
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

      for (E entry : assignmentsToFirst) {
        if (this.debug) {
          msg.append("n_" + getID() + " " + entry + "\n");
        }
        addDirectoryEntry(entry);
      }

      for (E entry : assignmentsToSecond) {
        if (this.debug) {
          msg.append("n_" + newNode.getID() + " " + entry + "\n");
        }
        newNode.addDirectoryEntry(entry);
      }
      if (this.debug) {
    	  debugFine(msg.toString());
      }
      return newNode;
    }
  }

  /**
   * Creates a new leaf node with the specified capacity.
   * Subclasses have to overwrite this method.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected N createNewLeafNode(int capacity) {
    //noinspection unchecked
    return (N) new MTreeNode<O, D, N, E>(getFile(), capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   * Subclasses have to overwrite this method.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected N createNewDirectoryNode(int capacity) {
    //noinspection unchecked
    return (N) new MTreeNode<O, D, N, E>(getFile(), capacity, false);
  }

  /**
   * Tests this node (for debugging purposes).
   */
  public void test() {
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
            throw new RuntimeException("Wrong Child in " + this
                                       + " at " + i
                                       + ": child id no leaf, but node is leaf!");
          }

          if (!childIsLeaf && node.isLeaf())
            throw new RuntimeException("Wrong Child in " + this
                                       + " at " + i
                                       + ": child id no leaf, but node is leaf!");
        }
      }
    }
  }

  /**
   * Tests, if the covering radii are correctly set.
   */
  public void testCoveringRadius(Integer objectID, D coveringRadius,
                                 DistanceFunction<O, D> distanceFunction) {
    for (int i = 0; i < getNumEntries(); i++) {
      D dist = distanceFunction.distance(getEntry(i).getRoutingObjectID(),
                                         objectID);
      if (dist.compareTo(coveringRadius) > 0) {
        String msg = "dist > cr \n" + dist + " > " + coveringRadius
                     + "\n" + "in " + this.toString() + " at entry "
                     + getEntry(i) + "\n" + "distance("
                     + getEntry(i).getRoutingObjectID() + " - " + objectID + ")"
                     + " >  cr(" + getEntry(i) + ")";

        // throw new RuntimeException(msg);
        if (dist instanceof NumberDistance) {
          double d1 = Double.parseDouble(dist.toString());
          double d2 = Double.parseDouble(coveringRadius.toString());
          if (Math.abs(d1 - d2) > 0.000000001)
            throw new RuntimeException(msg);
          // System.out.println("ALERT " + msg + "\n");
        }
        else
          throw new RuntimeException(msg);
        // System.out.println("ALERT " + msg + "\n");
      }
    }

  }

  /**
   * Tests, if the parent distances are correctly set.
   */
  public void testParentDistance(Integer objectID,
                                 DistanceFunction<O, D> distanceFunction) {
    for (int i = 0; i < getNumEntries(); i++) {
      if (objectID != null) {
        D dist = distanceFunction.distance(getEntry(i).getRoutingObjectID(),
                                           objectID);
        if (!getEntry(i).getParentDistance().equals(dist)) {
          throw new RuntimeException("entry.pd != dist: \n"
                                     + getEntry(i).getParentDistance() + " != " + dist
                                     + "\n" + "in " + this.toString() + " at entry "
                                     + getEntry(i) + "\n" + "distance("
                                     + getEntry(i).getRoutingObjectID() + " - " + objectID + ")");
        }
      }
      else {
        if (getEntry(i).getParentDistance() != null)
          throw new RuntimeException("entry.pd != null : \n"
                                     + getEntry(i).getParentDistance() + " != null \n"
                                     + "in " + this.toString() + " at entry "
                                     + getEntry(i) + "\n");
      }
    }
  }
}
