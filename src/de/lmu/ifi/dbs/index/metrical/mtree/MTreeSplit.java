package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.metrical.mtree.util.Assignments;
import de.lmu.ifi.dbs.index.metrical.mtree.util.DistanceEntry;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.*;

/**
 * Encapsulates the required parameters for a split of a node in a M-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class MTreeSplit<O extends DatabaseObject, D extends Distance<D>> {
  /**
   * Encapsulates the two promotion objects and their assignments.
   */
  Assignments<D> assignments;

  /**
   * Creates a new split object.
   */
  public MTreeSplit() {
  }

  /**
   * Creates a balanced partition of the entries of the specified node.
   * @param node the node to be splitted
   * @param routingObject1 the id of the first routing object
   * @param routingObject2 the id of the second routing object
   * @param distanceFunction the distance function to compute the distances
   * @return an assignment that holds a balanced partition of the entries of the specified node
   */
  Assignments<D> balancedPartition(MTreeNode<O, D> node,
                                  Integer routingObject1, Integer routingObject2,
                                  DistanceFunction<O, D> distanceFunction) {

    HashSet<MTreeEntry<D>> assigned1 = new HashSet<MTreeEntry<D>>();
    HashSet<MTreeEntry<D>> assigned2 = new HashSet<MTreeEntry<D>>();

    D currentCR1 = distanceFunction.nullDistance();
    D currentCR2 = distanceFunction.nullDistance();

    // determine the nearest neighbors
    List<DistanceEntry<D>> list1 = new ArrayList<DistanceEntry<D>>();
    List<DistanceEntry<D>> list2 = new ArrayList<DistanceEntry<D>>();
    for (int i = 0; i < node.numEntries; i++) {
      Integer id = node.entries[i].getRoutingObjectID();
      // determine the distance of o to o1 / o2
      D d1 = distanceFunction.distance(routingObject1, id);
      D d2 = distanceFunction.distance(routingObject2, id);

      list1.add(new DistanceEntry<D>(node.entries[i], d1, i));
      list2.add(new DistanceEntry<D>(node.entries[i], d2, i));
    }
    Collections.sort(list1);
    Collections.sort(list2);

    for (int i = 0; i < node.numEntries; i++) {
      if (i % 2 == 0) {
        currentCR1 = assignNN(assigned1, assigned2, list1, currentCR1, node.isLeaf());
      }
      else {
        currentCR2 = assignNN(assigned2, assigned1, list2, currentCR2, node.isLeaf());
      }
    }
    return new Assignments<D>(routingObject1, routingObject2, currentCR1, currentCR2, assigned1, assigned2);
  }

  /**
   * Assigns the first object of the specified list to the first assignment that
   * it is not yet assigned to the second assignment.
   *
   * @param assigned1 the first assignment
   * @param assigned2 the second assignment
   * @param list      the list, the first object should be assigned
   * @param currentCR the current covering radius
   * @param isLeaf    true, if the node of the entries to be assigned is a leaf, false othwerwise
   * @return the new covering radius
   */
  private D assignNN(Set<MTreeEntry<D>> assigned1,
                     Set<MTreeEntry<D>> assigned2,
                     List<DistanceEntry<D>> list,
                     D currentCR,
                     boolean isLeaf) {


    DistanceEntry<D> distEntry = list.remove(0);
    while (assigned2.contains(distEntry.getEntry())) {
      distEntry = list.remove(0);
    }
    assigned1.add(distEntry.getEntry());

    if (isLeaf) {
      return Util.max(currentCR, distEntry.getDistance());
    }
    else {
      return Util.max(currentCR,
                      (D) distEntry.getDistance().plus(((MTreeDirectoryEntry<D>) distEntry.getEntry()).getCoveringRadius()));
    }
  }

  /**
   * Returns the assignments of this split.
   * @return the assignments of this split
   */
  public Assignments<D> getAssignments() {
    return assignments;
  }
}
