package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.split;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.split.Assignments;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Encapsulates the required methods for a split of a node in an M-Tree. The
 * routing objects are chosen according to the M_rad strategy.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to be stored in the M-Tree
 * @param <D> the type of Distance used in the M-Tree
 * @param <N> the type of AbstractMTreeNode used in the M-Tree
 * @param <E> the type of MetricalEntry used in the M-Tree
 */
public class MRadSplit<O extends DatabaseObject, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>> extends MTreeSplit<O, D, N, E> {

  /**
   * Creates a new split object.
   * 
   * @param node the node to be split
   * @param distanceFunction the distance function
   */
  public MRadSplit(N node, DistanceFunction<O, D> distanceFunction) {
    super();
    promote(node, distanceFunction);
  }

  /**
   * Selects two objects of the specified node to be promoted and stored into
   * the parent node. The m-RAD strategy considers all possible pairs of objects
   * and, after partitioning the set of entries, promotes the pair of objects
   * for which the sum of covering radiuses is minimum.
   * 
   * @param node the node to be split
   * @param distanceFunction the distance function
   */
  private void promote(N node, DistanceFunction<O, D> distanceFunction) {
    D miSumCR = distanceFunction.infiniteDistance();

    for(int i = 0; i < node.getNumEntries(); i++) {
      Integer id1 = node.getEntry(i).getRoutingObjectID();

      for(int j = i + 1; j < node.getNumEntries(); j++) {
        Integer id2 = node.getEntry(i).getRoutingObjectID();
        // ... for each pair do testPartition...
        Assignments<D, E> currentAssignments = balancedPartition(node, id1, id2, distanceFunction);

        D sumCR = currentAssignments.getFirstCoveringRadius().plus(currentAssignments.getSecondCoveringRadius());
        if(sumCR.compareTo(miSumCR) < 0) {
          miSumCR = sumCR;
          assignments = currentAssignments;
        }
      }
    }
  }
}
