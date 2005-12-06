package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.metrical.mtree.util.DistanceEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the required parameters for a split of a node in an M-Tree.
 * The routing objects are chosen according to the M_LB_DIST strategy.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MLBDistSplit<O extends MetricalObject, D extends Distance<D>> extends Split<D> {

  /**
   * Creates a new split object.
   *
   * @param node             the node to be splitted
   * @param routingObjectID  the od of the routing object belonging to the specified node
   * @param distanceFunction the distance function
   */
  public MLBDistSplit(MTreeNode<O, D> node, Integer routingObjectID,
                      DistanceFunction<O, D> distanceFunction) {
    super();
    if (routingObjectID != null)
      promote(node, routingObjectID, distanceFunction);
    else
      promote(node, node.entries[0].getObjectID(), distanceFunction);
  }

  /**
   * Selects the second object of the specified node to be promoted
   * and stored into the parent node and partitions the entries
   * according to the M_LB_DIST strategy.
   * The second promoted object will be the farthest object from the first promoted object.
   *
   * @param node             the node to be splitted
   * @param firstPromotedID  the id of the first promoted object
   * @param distanceFunction the distance function
   */
  private void promote(MTreeNode<O, D> node, Integer firstPromotedID,
                       DistanceFunction<O, D> distanceFunction) {

    firstPromoted = firstPromotedID;

    // choose second promoted
    D currentMaxDist = distanceFunction.nullDistance();
    for (int i = 0; i < node.numEntries; i++) {
      Integer id = node.entries[i].getObjectID();
      D distance = distanceFunction.distance(firstPromoted, id);

      if (currentMaxDist.compareTo(distance) < 0) {
        secondPromoted = id;
        currentMaxDist = distance;
      }
    }

    // partition the entries
    List<DistanceEntry<D>> list1 = new ArrayList<DistanceEntry<D>>();
    List<DistanceEntry<D>> list2 = new ArrayList<DistanceEntry<D>>();
    for (int i = 0; i < node.numEntries; i++) {
      Integer id = node.entries[i].getObjectID();
      D d1 = distanceFunction.distance(firstPromoted, id);
      D d2 = distanceFunction.distance(secondPromoted, id);

      list1.add(new DistanceEntry<D>(node.entries[i], d1, i));
      list2.add(new DistanceEntry<D>(node.entries[i], d2, i));
    }
    Collections.sort(list1);
    Collections.sort(list2);

    firstCoveringRadius = distanceFunction.nullDistance();
    secondCoveringRadius = distanceFunction.nullDistance();
    assignmentsToFirst = new ArrayList<Entry<D>>();
    assignmentsToSecond = new ArrayList<Entry<D>>();
    for (int i = 0; i < node.numEntries; i++) {
      if (i % 2 == 0) {
        firstCoveringRadius = assignNN(assignmentsToFirst, list1, list2, firstCoveringRadius, node.isLeaf());
      }
      else {
        secondCoveringRadius = assignNN(assignmentsToSecond, list2, list1, secondCoveringRadius, node.isLeaf());
      }
    }
  }
}
