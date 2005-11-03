package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates the required parameters for a split of a node in a M-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MLBDistSplit<O extends MetricalObject, D extends Distance> extends Split<D> {

  /**
   * Creates a new split object.
   *
   * @param node             the node to be splitted
   * @param routingObjectID  the od of the routing object belonging to the specified node
   * @param distanceFunction the distance function
   */
  public MLBDistSplit(MTreeNode<O, D> node, Integer routingObjectID, DistanceFunction<O, D> distanceFunction) {
    super();
    if (routingObjectID != null)
      promote(node, routingObjectID, distanceFunction);
    else
      promote(node, distanceFunction);
  }

  /**
   * Selects two objects of the specified node to be promoted
   * and stored into the parent node.
   * The m-RAD strategy considers all possible pairs of objects and,
   * after partitioning the set of entries, promotes
   * the pair of objects for which the sum of covering radii is minimum.
   *
   * @param node             the node to be splitted
   * @param routingObjectID  the od of the routing object belonging to the specified node
   * @param distanceFunction the distance function
   */
  private void promote(MTreeNode<O, D> node, Integer routingObjectID, DistanceFunction<O, D> distanceFunction) {
    firstPromoted = routingObjectID;

    // choose second promoted
    D currentMaxDist = distanceFunction.nullDistance();
    for (int i = 0; i < node.numEntries; i++) {
      Integer id = node.entries[i].getObjectID();
      D distance = node.entries[i].getParentDistance();

      if (currentMaxDist.compareTo(distance) < 0) {
        secondPromoted = id;
        currentMaxDist = distance;
      }
    }

    // partition the entries
    List<DistanceEntry> list1 = new ArrayList<DistanceEntry>();
    List<DistanceEntry> list2 = new ArrayList<DistanceEntry>();
    for (int i = 0; i < node.numEntries; i++) {
      Integer id = node.entries[i].getObjectID();
      D d1 = node.entries[i].getParentDistance();
      D d2 = distanceFunction.distance(secondPromoted, id);

      list1.add(new DistanceEntry(node.entries[i], d1));
      list2.add(new DistanceEntry(node.entries[i], d2));
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

  /**
   * Selects two objects of the specified node to be promoted
   * and stored into the parent node.
   * The m-RAD strategy considers all possible pairs of objects and,
   * after partitioning the set of entries, promotes
   * the pair of objects for which the sum of covering radii is minimum.
   *
   * @param node             the node to be splitted
   * @param distanceFunction the distance function
   */
  private void promote(MTreeNode<O, D> node, DistanceFunction<O, D> distanceFunction) {
    firstPromoted = node.entries[0].getObjectID();

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
    List<DistanceEntry> list1 = new ArrayList<DistanceEntry>();
    List<DistanceEntry> list2 = new ArrayList<DistanceEntry>();
    for (int i = 0; i < node.numEntries; i++) {
      Integer id = node.entries[i].getObjectID();
      D d1 = distanceFunction.distance(firstPromoted, id);
      D d2 = distanceFunction.distance(secondPromoted, id);

      list1.add(new DistanceEntry(node.entries[i], d1));
      list2.add(new DistanceEntry(node.entries[i], d2));
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


  private D assignNN(List<Entry<D>> assignment, List<DistanceEntry> list,
                     List<DistanceEntry> other, D currentCR,
                     boolean isLeaf) {

    DistanceEntry de = list.remove(0);
    Integer id = de.entry.getObjectID();

    remove(id, other);
    assignment.add(de.entry);

    if (isLeaf)
      return Util.max(currentCR, de.distance);
    else {
      return Util.max(currentCR, de.distance.plus(((DirectoryEntry<D>) de.entry).getCoveringRadius()));
    }
  }

  private void remove(Integer id, List<DistanceEntry> list) {
    for (Iterator<DistanceEntry> iterator = list.iterator(); iterator.hasNext();) {
      DistanceEntry de = iterator.next();
      if (de.entry.getObjectID() == id) iterator.remove();
    }
  }

}
