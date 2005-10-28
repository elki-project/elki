package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;

/**
 * Encapsulates the required parameters for a split of a node in a M-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MRadSplit<O extends MetricalObject, D extends Distance> extends Split<D> {

  /**
   * Creates a new split object.
   *
   * @param node             the node to be splitted
   * @param distanceFunction the distance function
   */
  public MRadSplit(MTreeNode<O, D> node, DistanceFunction<O, D> distanceFunction) {
    super();
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
   * @param distanceFunction the distance function
   */
  private void promote(MTreeNode<O, D> node, DistanceFunction<O, D> distanceFunction) {
    D maxCR = distanceFunction.infiniteDistance();

    for (int i = 0; i < node.numEntries; i++) {
      Integer id1 = node.entries[i].getObjectID();

      for (int j = i+1; j < node.numEntries; j++) {
        Integer id2 = node.entries[j].getObjectID();
        // ... for each pair do testPartition...
        Assignment assignment = testPartition(node, id1, id2, maxCR, distanceFunction);
        if (assignment == null) continue;

        D ass_maxCR = Util.max(assignment.firstCoveringRadius, assignment.secondCoveringRadius);
        if (ass_maxCR.compareTo(maxCR) < 0) {
          maxCR = ass_maxCR;
          firstPromoted = id1;
          secondPromoted = id2;
          firstCoveringRadius = assignment.firstCoveringRadius;
          secondCoveringRadius = assignment.secondCoveringRadius;
          assignmentsToFirst = assignment.firstAssignment;
          assignmentsToSecond = assignment.secondAssignment;
        }
      }
    }
  }

  /**
   * Determines the maximum covering radius of the specified objects o1 and o2.
   * If the current maximum covering radius (during determination) gets greater
   * than the given currentMinCovRad, the method aborts the current computation and
   * returns a value >= currentMinCovRad.
   *
   * @param node            the node to be split
   * @param id1             the id of the first promotion object
   * @param id2             the id of the second promotion object
   * @param currentMinCR    current minimum covering radius
   * @return the maximum covering radius maxCovRad of the two given objects,
   *         if maxCovRad < currentMinCovRad,
   *         or a value >= currentMinCOvRad, otherwise
   */
  private Assignment testPartition(MTreeNode<O, D> node,
                                   Integer id1, Integer id2,
                                   D currentMinCR,
                                   DistanceFunction<O, D> distanceFunction) {

    D currentCR1 = distanceFunction.nullDistance();
    D currentCR2 = distanceFunction.nullDistance();

    List<Entry<D>> firstAssignment = new ArrayList<Entry<D>>();
    List<Entry<D>> secondAssignment = new ArrayList<Entry<D>>();

    List<DistanceEntry> list1 = new ArrayList<DistanceEntry>();
    List<DistanceEntry> list2 = new ArrayList<DistanceEntry>();

    for (int i = 0; i < node.numEntries; i++) {
      Integer id = node.entries[i].getObjectID();
      // determine the distance of o to o1 / o2
      D d1 = distanceFunction.distance(id1, id);
      D d2 = distanceFunction.distance(id2, id);

      list1.add(new DistanceEntry(node.entries[i], d1));
      list2.add(new DistanceEntry(node.entries[i], d2));
    }
    Collections.sort(list1);
    Collections.sort(list2);

    for (int i = 0; i < node.numEntries; i++) {
      if (i % 2 == 0) {
        currentCR1 = assignNN(firstAssignment, list1, list2, currentCR1, node.isLeaf());
      }
      else {
        currentCR2 = assignNN(secondAssignment, list2, list1, currentCR2, node.isLeaf());
      }

      // break if the sum of currentCRs > current minimum covering radius
      D sumCurrentCR = currentCR1.plus(currentCR2);
      if (sumCurrentCR.compareTo(currentMinCR) >= 0) {
        return null;
      }
    }
    return new Assignment(currentCR1, currentCR2, firstAssignment, secondAssignment);
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
      return Util.max(currentCR, de.distance.plus(((DirectoryEntry<D>)de.entry).getCoveringRadius()));
    }
  }

  private void remove(Integer id, List<DistanceEntry> list) {
    for (Iterator<DistanceEntry> iterator = list.iterator(); iterator.hasNext();) {
      DistanceEntry de = iterator.next();
      if (de.entry.getObjectID() == id) iterator.remove();
    }
  }

  private class Assignment {
    D firstCoveringRadius;
    D secondCoveringRadius;
    List<Entry<D>> firstAssignment;
    List<Entry<D>> secondAssignment;

    public Assignment(D firstCoveringRadius, D secondCoveringRadius,
                      List<Entry<D>> firstAssignment, List<Entry<D>> secondAssignment) {
      this.firstCoveringRadius = firstCoveringRadius;
      this.secondCoveringRadius = secondCoveringRadius;
      this.firstAssignment = firstAssignment;
      this.secondAssignment = secondAssignment;
    }
  }

}
