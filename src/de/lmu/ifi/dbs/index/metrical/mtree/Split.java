package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.List;
import java.util.ArrayList;

/**
 * Encapsulates the required parameters for a split of a node in a M-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Split<O extends MetricalObject, D extends Distance> {
  /**
   * The first promotion object.
   */
  O firstPromoted;

  /**
   * The second promotion object.
   */
  O secondPromoted;

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
  List<Entry<O,D>> assignmentsToFirst;

  /**
   * Entries assigned to second promotion object
   */
  List<Entry<O,D>> assignmentsToSecond;

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
      O o1 = node.entries[i].getObject();
      for (int j = 0; j < node.numEntries; j++) {
        O o2 = node.entries[j].getObject();
        // ... for each pair do testPartition...
        Assignment assignment = testPartition(node, o1, o2, maxCR, distanceFunction);
        if (assignment == null) continue;

        D ass_maxCR = Util.max(assignment.firstCoveringRadius, assignment.secondCoveringRadius);
        if (ass_maxCR.compareTo(maxCR) < 0) {
          maxCR = ass_maxCR;

          firstPromoted = o1;
          secondPromoted = o2;
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
   * @param o1           first promotion object
   * @param o2           second promotion object
   * @param currentMinCR current minimum covering radius
   * @return the maximum covering radius maxCovRad of the two given objects,
   *         if maxCovRad < currentMinCovRad,
   *         or a value >= currentMinCOvRad, otherwise
   */
  private Assignment testPartition(MTreeNode<O, D> node, O o1, O o2, D currentMinCR,
                                   DistanceFunction<O, D> distanceFunction) {

    D currentCR = distanceFunction.nullDistance();

    D firstCR = distanceFunction.nullDistance();
    D secondCR = distanceFunction.nullDistance();
    List<Entry<O,D>> firstAssignment = new ArrayList<Entry<O,D>>();
    List<Entry<O,D>> secondAssignment = new ArrayList<Entry<O,D>>();

    for (int i = 0; i < node.numEntries; i++) {
      O o = node.entries[i].getObject();
      // determine the distance of o to o1 / o2
      D d1 = distanceFunction.distance(o1, o);
      D d2 = distanceFunction.distance(o2, o);

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
    List<Entry<O,D>> firstAssignment;
    List<Entry<O,D>> secondAssignment;

    public Assignment(D firstCoveringRadius, D secondCoveringRadius,
                      List<Entry<O,D>> firstAssignment, List<Entry<O,D>> secondAssignment) {
      this.firstCoveringRadius = firstCoveringRadius;
      this.secondCoveringRadius = secondCoveringRadius;
      this.firstAssignment = firstAssignment;
      this.secondAssignment = secondAssignment;
    }
  }

}
