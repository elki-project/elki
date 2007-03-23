package de.lmu.ifi.dbs.index.tree.metrical.mtreevariants;

import java.util.List;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.tree.AbstractNode;
import de.lmu.ifi.dbs.index.tree.metrical.MetricalNode;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * Represents a node in an AbstractM-Tree.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractMTreeNode<O extends DatabaseObject, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>> extends AbstractNode<N, E> implements MetricalNode<N,E> {

  /**
   * Empty constructor for Externalizable interface.
   */
  public AbstractMTreeNode() {
  }

  /**
   * Creates a new MTreeNode with the specified parameters.
   *
   * @param file     the file storing the M-Tree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public AbstractMTreeNode(PageFile<N> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
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
   * Adjusts the parameters of the entry representing this node.
   *
   * @param entry           the entry representing this node
   * @param routingObjectID the id of the (new) routing object of this node
   * @param parentDistance  the distance from the routing object of this node
   *                        to the routing object of the parent node
   * @param mTree           the M-Tree object holding this node
   */
  public void adjustEntry(E entry, Integer routingObjectID, D parentDistance, AbstractMTree<O, D, N, E> mTree) {
    entry.setRoutingObjectID(routingObjectID);
    entry.setParentDistance(parentDistance);
    entry.setCoveringRadius(coveringRadius(entry.getRoutingObjectID(), mTree));

    for (int i = 0; i < getNumEntries(); i++) {
      E childEntry = getEntry(i);
      D dist = mTree.distance(routingObjectID, childEntry.getRoutingObjectID());
      childEntry.setParentDistance(dist);
    }
  }

  /**
   * Determines and returns the covering radius of this node.
   *
   * @param routingObjectID the object id of the routing object of this node
   * @param mTree           the M-Tree
   * @return he covering radius of this node
   */
  public D coveringRadius(Integer routingObjectID, AbstractMTree<O, D, N, E> mTree) {
    D coveringRadius = mTree.getDistanceFunction().nullDistance();
    for (int i = 0; i < getNumEntries(); i++) {
      E entry = getEntry(i);
      D distance = mTree.distance(entry.getRoutingObjectID(), routingObjectID);
      coveringRadius = Util.max(coveringRadius, distance);
    }
    return coveringRadius;
  }

  /**
   * Tests this node (for debugging purposes).
   *
   * @param mTree the M-Tree holding this node
   * @param entry the entry representing this node
   */
  public final void test(AbstractMTree<O, D, N, E> mTree, E entry) {
    // leaf node
    if (isLeaf()) {
      for (int i = 0; i < getCapacity(); i++) {
        E e = getEntry(i);
        if (i < getNumEntries() && e == null) {
          throw new RuntimeException("i < numEntries && entry == null");
        }
        if (i >= getNumEntries() && e != null) {
          throw new RuntimeException("i >= numEntries && entry != null");
        }
      }
    }

    // dir node
    else {
      N tmp = getFile().readPage(getEntry(0).getID());
      boolean childIsLeaf = tmp.isLeaf();

      for (int i = 0; i < getCapacity(); i++) {
        E e = getEntry(i);

        if (i < getNumEntries() && e == null) {
          throw new RuntimeException("i < numEntries && entry == null");
        }

        if (i >= getNumEntries() && e != null) {
          throw new RuntimeException("i >= numEntries && entry != null");
        }

        if (e != null) {
          N node = getFile().readPage(e.getID());

          if (childIsLeaf && !node.isLeaf()) {
            for (int k = 0; k < getNumEntries(); k++) {
              getFile().readPage(getEntry(k).getID());
            }

            throw new RuntimeException("Wrong Child in " + this + " at " + i);
          }

          if (!childIsLeaf && node.isLeaf()) {
            throw new RuntimeException("Wrong Child: child id no leaf, but node is leaf!");
          }

          //noinspection unchecked
          node.test(entry, (N) this, i, mTree);
          node.test(mTree, e);
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
   * @param parentEntry the entry representing the parent
   * @param parent the parent holding the entry representing this node
   * @param index  the index of the entry in the parents child arry
   * @param mTree  the M-Tree holding this node
   */
  protected void test(E parentEntry, N parent, int index, AbstractMTree<O, D, N, E> mTree) {
    // test if parent distance is correctly set
    E entry = parent.getEntry(index);
    D parentDistance = mTree.distance(entry.getRoutingObjectID(), parentEntry.getRoutingObjectID());
    if (!entry.getParentDistance().equals(parentDistance)) {
      String soll = parentDistance.toString();
      String ist = entry.getParentDistance().toString();
      throw new RuntimeException("Wrong parent distance in node "
                                 + parent.getID() + " at index " + index
                                 + " (child " + entry.getID() + ")"
                                 + "\nsoll: " + soll
                                 + ",\n ist: " + ist);
    }

    // test if covering radius is correctly set
    //noinspection unchecked
    if (entry.getCoveringRadius().compareTo(parentDistance) < 0) {
      String msg = "cr < pd \n"
                   + entry.getCoveringRadius() + " < " + parentDistance
                   + "in node " + parent.getID() + " at index " + index
                   + " (child " + entry.getID() + "):\n"
                   + "dist(" + entry.getRoutingObjectID() + " - " + parentEntry.getRoutingObjectID() + ")"
                   + " >  cr(" + entry + ")";

      // throw new RuntimeException(msg);
      if (parentDistance instanceof NumberDistance) {
        double d1 = Double.parseDouble(parentDistance.toString());
        double d2 = Double.parseDouble(entry.getCoveringRadius().toString());
        if (Math.abs(d1 - d2) > 0.000000001) {
          throw new RuntimeException(msg);
        }
      }
      else {
        throw new RuntimeException(msg);
      }
    }
  }
}
