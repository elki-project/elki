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
public class MLBDistSplit<O extends MetricalObject, D extends Distance<D>> extends Split<O, D> {

  /**
   * Creates a new split object.
   *
   * @param node             the node to be splitted
   * @param distanceFunction the distance function
   */
  public MLBDistSplit(MTreeNode<O, D> node, DistanceFunction<O, D> distanceFunction) {
    super();
    promote(node, distanceFunction);
  }

  /**
   * Selects the second object of the specified node to be promoted
   * and stored into the parent node and partitions the entries
   * according to the M_LB_DIST strategy.
   *
   * This strategy considers all possible pairs of objects and
   * chooses the pair of objects for which the distance is maximum.
   * @param node             the node to be splitted
   * @param distanceFunction the distance function
   */
  private void promote(MTreeNode<O, D> node, DistanceFunction<O, D> distanceFunction) {
    Integer firstPromoted = null;
    Integer secondPromoted = null;

    // choose first and second routing object
      D currentMaxDist = distanceFunction.nullDistance();
      for (int i = 0; i < node.numEntries; i++) {
        Integer id1 = node.entries[i].getObjectID();
        for (int j = i + 1; j < node.numEntries; j++) {
          Integer id2 = node.entries[j].getObjectID();

          D distance = distanceFunction.distance(id1, id2);
          if (distance.compareTo(currentMaxDist) > 0) {
            firstPromoted = id1;
            secondPromoted = id2;
            currentMaxDist = distance;
          }
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

    assignments = balancedPartition(node, firstPromoted, secondPromoted, distanceFunction);
  }
}
