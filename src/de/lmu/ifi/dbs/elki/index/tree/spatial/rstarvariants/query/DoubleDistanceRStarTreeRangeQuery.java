package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.DirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.query.DoubleDistanceSearchCandidate;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.UpdatableHeap;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Instance of a range query for a particular spatial index.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses AbstractRStarTree
 * @apiviz.uses SpatialPrimitiveNumberDistanceFunction
 */
public class DoubleDistanceRStarTreeRangeQuery<O extends SpatialComparable> extends AbstractDistanceRangeQuery<O, DoubleDistance> {
  /**
   * The index to use
   */
  protected final AbstractRStarTree<O, ?, ?> index;

  /**
   * Spatial primitive distance function
   */
  protected final SpatialPrimitiveDoubleDistanceFunction<? super O> distanceFunction;

  /**
   * Constructor.
   * 
   * @param relation Relation to use
   * @param index Index to use
   * @param distanceQuery Distance query to use
   * @param distanceFunction Distance function
   */
  public DoubleDistanceRStarTreeRangeQuery(Relation<? extends O> relation, AbstractRStarTree<O, ?, ?> index, DistanceQuery<O, DoubleDistance> distanceQuery, SpatialPrimitiveDoubleDistanceFunction<? super O> distanceFunction) {
    super(relation, distanceQuery);
    this.index = index;
    this.distanceFunction = distanceFunction;
  }

  /**
   * Perform the actual query process.
   * 
   * @param object Query object
   * @param epsilon Query range
   * @return Objects contained in query range.
   */
  protected List<DistanceResultPair<DoubleDistance>> doRangeQuery(O object, double epsilon) {
    final List<DistanceResultPair<DoubleDistance>> result = new ArrayList<DistanceResultPair<DoubleDistance>>();
    final Heap<DoubleDistanceSearchCandidate> pq = new UpdatableHeap<DoubleDistanceSearchCandidate>();

    // push root
    pq.add(new DoubleDistanceSearchCandidate(0.0, index.getRootEntry().getEntryID()));

    // search in tree
    while(!pq.isEmpty()) {
      DoubleDistanceSearchCandidate pqNode = pq.poll();
      if(pqNode.mindist > epsilon) {
        break;
      }

      AbstractRStarTreeNode<?, ?> node = index.getNode(pqNode.nodeID);
      final int numEntries = node.getNumEntries();

      for(int i = 0; i < numEntries; i++) {
        double distance = distanceFunction.doubleMinDist(node.getEntry(i), object);
        if(distance <= epsilon) {
          if(node.isLeaf()) {
            LeafEntry entry = (LeafEntry) node.getEntry(i);
            result.add(new DoubleDistanceResultPair(distance, entry.getDBID()));
          }
          else {
            DirectoryEntry entry = (DirectoryEntry) node.getEntry(i);
            pq.add(new DoubleDistanceSearchCandidate(distance, entry.getEntryID()));
          }
        }
      }
    }

    // sort the result according to the distances
    Collections.sort(result);
    return result;
  }

  @Override
  public List<DistanceResultPair<DoubleDistance>> getRangeForObject(O obj, DoubleDistance range) {
    return doRangeQuery(obj, range.doubleValue());
  }

  @Override
  public List<DistanceResultPair<DoubleDistance>> getRangeForDBID(DBID id, DoubleDistance range) {
    return getRangeForObject(relation.get(id), range);
  }

  @SuppressWarnings("unused")
  @Override
  public List<List<DistanceResultPair<DoubleDistance>>> getRangeForBulkDBIDs(ArrayDBIDs ids, DoubleDistance range) {
    // TODO: implement
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }
}