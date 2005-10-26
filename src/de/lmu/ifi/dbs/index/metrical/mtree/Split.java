package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the required parameters for a split of a node in a M-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Split<O extends MetricalObject, D extends Distance> {
  /**
   * The id of the first promotion object.
   */
  Integer firstPromoted;

  /**
   * The id of the second promotion object.
   */
  Integer secondPromoted;

  /**
   * The first covering radius.
   */
  D firstCoveringRadius;

  /**
   * The second covering radius.
   */
  D secondCoveringRadius;

  /**
   * Entries assigned to first promotion object
   */
  List<Entry<D>> assignmentsToFirst;

  /**
   * Entries assigned to second promotion object
   */
  List<Entry<D>> assignmentsToSecond;

  /**
   * Creates a new split object.
   *
   * @param node             the node to be splitted
   * @param distanceFunction the distance function
   */
  public Split(MTreeNode<O, D> node, DistanceFunction<O, D> distanceFunction) {
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
      for (int j = 0; j < node.numEntries; j++) {
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
   * @param node         the node to be split
   * @param id1          the id of the first promotion object
   * @param id2          the od of the second promotion object
   * @param currentMinCR current minimum covering radius
   * @return the maximum covering radius maxCovRad of the two given objects,
   *         if maxCovRad < currentMinCovRad,
   *         or a value >= currentMinCOvRad, otherwise
   */
  private Assignment testPartition(MTreeNode<O, D> node, Integer id1, Integer id2, D currentMinCR,
                                   DistanceFunction<O, D> distanceFunction) {

    D currentCR = distanceFunction.nullDistance();

    D firstCR = distanceFunction.nullDistance();
    D secondCR = distanceFunction.nullDistance();
    List<Entry<D>> firstAssignment = new ArrayList<Entry<D>>();
    List<Entry<D>> secondAssignment = new ArrayList<Entry<D>>();

    for (int i = 0; i < node.numEntries; i++) {
      Integer id = node.entries[i].getObjectID();
      // determine the distance of o to o1 / o2
      D d1 = distanceFunction.distance(id1, id);
      D d2 = distanceFunction.distance(id2, id);

      // save the current maximum of all distances and
      // break if current maximum > current minimum covering radius
      D minDistance = Util.min(d1, d2);
      currentCR = Util.max(currentCR, minDistance);
      if (currentCR.compareTo(currentMinCR) >= 0) {
        return null;
      }

      // assign o to to o1 or o2 and update the covering radii
      if (d1.compareTo(d2) <= 0) {
        firstAssignment.add(node.entries[i]);
        firstCR = Util.max(firstCR, d1);
      }
      else {
        secondAssignment.add(node.entries[i]);
        secondCR = Util.max(secondCR, d2);
      }
    }
    return new Assignment(firstCR, secondCR, firstAssignment, secondAssignment);
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
