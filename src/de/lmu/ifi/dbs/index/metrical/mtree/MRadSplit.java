package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.metrical.mtree.util.Assignments;

/**
 * Encapsulates the required parameters for a split of a node in an M-Tree.
 * The routing objects are chosen according to the M_rad strategy.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MRadSplit<O extends DatabaseObject, D extends Distance<D>> extends Split<O,D> {

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
    D miSumCR = distanceFunction.infiniteDistance();

    for (int i = 0; i < node.numEntries; i++) {
      Integer id1 = node.entries[i].getObjectID();

      for (int j = i + 1; j < node.numEntries; j++) {
        Integer id2 = node.entries[j].getObjectID();
        // ... for each pair do testPartition...
        Assignments<D> currentAssignments = balancedPartition(node, id1, id2, distanceFunction);

        D sumCR = currentAssignments.getFirstCoveringRadius().plus(currentAssignments.getSecondCoveringRadius());
        if (sumCR.compareTo(miSumCR) < 0) {
          miSumCR = sumCR;
          assignments = currentAssignments;
        }
      }
    }
  }
}
