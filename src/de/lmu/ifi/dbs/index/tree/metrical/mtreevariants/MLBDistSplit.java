package de.lmu.ifi.dbs.index.tree.metrical.mtreevariants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.index.tree.DistanceEntry;

/**
 * Encapsulates the required parameters for a split of a node in an M-Tree.
 * The routing objects are chosen according to the M_LB_DIST strategy.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MLBDistSplit<O extends DatabaseObject, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>> extends MTreeSplit<O, D, N, E> {

  /**
   * Creates a new split object.
   *
   * @param node             the node to be splitted
   * @param distanceFunction the distance function
   */
  public MLBDistSplit(N node, DistanceFunction<O, D> distanceFunction) {
    super();
    promote(node, distanceFunction);
  }

  /**
   * Selects the second object of the specified node to be promoted
   * and stored into the parent node and partitions the entries
   * according to the M_LB_DIST strategy.
   * <p/>
   * This strategy considers all possible pairs of objects and
   * chooses the pair of objects for which the distance is maximum.
   *
   * @param node             the node to be splitted
   * @param distanceFunction the distance function
   */
  private void promote(N node, DistanceFunction<O, D> distanceFunction) {
    Integer firstPromoted = null;
    Integer secondPromoted = null;

    // choose first and second routing object
    D currentMaxDist = distanceFunction.nullDistance();
    for (int i = 0; i < node.getNumEntries(); i++) {
      Integer id1 = node.getEntry(i).getRoutingObjectID();
      for (int j = i + 1; j < node.getNumEntries(); j++) {
        Integer id2 = node.getEntry(j).getRoutingObjectID();

        D distance = distanceFunction.distance(id1, id2);
        if (distance.compareTo(currentMaxDist) >= 0) {
          firstPromoted = id1;
          secondPromoted = id2;
          currentMaxDist = distance;
        }
      }
    }

    // partition the entries
    List<DistanceEntry<D, E>> list1 = new ArrayList<DistanceEntry<D, E>>();
    List<DistanceEntry<D, E>> list2 = new ArrayList<DistanceEntry<D, E>>();
    for (int i = 0; i < node.getNumEntries(); i++) {
      Integer id = node.getEntry(i).getRoutingObjectID();
      D d1 = distanceFunction.distance(firstPromoted, id);
      D d2 = distanceFunction.distance(secondPromoted, id);

      list1.add(new DistanceEntry<D, E>(node.getEntry(i), d1, i));
      list2.add(new DistanceEntry<D, E>(node.getEntry(i), d2, i));
    }
    Collections.sort(list1);
    Collections.sort(list2);

    assignments = balancedPartition(node, firstPromoted, secondPromoted, distanceFunction);
  }
}
