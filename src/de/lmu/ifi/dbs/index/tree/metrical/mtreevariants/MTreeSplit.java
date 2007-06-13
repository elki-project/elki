package de.lmu.ifi.dbs.index.tree.metrical.mtreevariants;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.util.Assignments;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates the required parameters for a split of a node in a M-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class MTreeSplit<O extends DatabaseObject, D extends Distance<D>, N extends AbstractMTreeNode<O,D,N, E>, E extends MTreeEntry<D>> {
  /**
   * Encapsulates the two promotion objects and their assignments.
   */
  Assignments<D,E> assignments;

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
  Assignments<D,E> balancedPartition(N node,
                                  Integer routingObject1, Integer routingObject2,
                                  DistanceFunction<O, D> distanceFunction) {

    HashSet<E> assigned1 = new HashSet<E>();
    HashSet<E> assigned2 = new HashSet<E>();

    D currentCR1 = distanceFunction.nullDistance();
    D currentCR2 = distanceFunction.nullDistance();

    // determine the nearest neighbors
    List<DistanceEntry<D,E>> list1 = new ArrayList<DistanceEntry<D,E>>();
    List<DistanceEntry<D,E>> list2 = new ArrayList<DistanceEntry<D,E>>();
    for (int i = 0; i < node.getNumEntries(); i++) {
      Integer id = node.getEntry(i).getRoutingObjectID();
      // determine the distance of o to o1 / o2
      D d1 = distanceFunction.distance(routingObject1, id);
      D d2 = distanceFunction.distance(routingObject2, id);

      list1.add(new DistanceEntry<D,E>(node.getEntry(i), d1, i));
      list2.add(new DistanceEntry<D,E>(node.getEntry(i), d2, i));
    }
    Collections.sort(list1);
    Collections.sort(list2);

    for (int i = 0; i < node.getNumEntries(); i++) {
      if (i % 2 == 0) {
        currentCR1 = assignNN(assigned1, assigned2, list1, currentCR1, node.isLeaf());
      }
      else {
        currentCR2 = assignNN(assigned2, assigned1, list2, currentCR2, node.isLeaf());
      }
    }
    return new Assignments<D,E>(routingObject1, routingObject2, currentCR1, currentCR2, assigned1, assigned2);
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
  private D assignNN(Set<E> assigned1,
                     Set<E> assigned2,
                     List<DistanceEntry<D,E>> list,
                     D currentCR,
                     boolean isLeaf) {


    DistanceEntry<D,E> distEntry = list.remove(0);
    //noinspection SuspiciousMethodCalls
    while (assigned2.contains(distEntry.getEntry())) {
      distEntry = list.remove(0);
    }
    assigned1.add(distEntry.getEntry());

    if (isLeaf) {
      return Util.max(currentCR, distEntry.getDistance());
    }
    else {
      //noinspection unchecked
      return Util.max(currentCR,
                      distEntry.getDistance().plus((distEntry.getEntry()).getCoveringRadius()));
    }
  }

  /**
   * Returns the assignments of this split.
   * @return the assignments of this split
   */
  public Assignments<D,E> getAssignments() {
    return assignments;
  }
}
