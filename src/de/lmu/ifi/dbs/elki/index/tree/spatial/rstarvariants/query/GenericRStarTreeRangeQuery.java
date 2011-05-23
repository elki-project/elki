package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.DirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.query.GenericDistanceSearchCandidate;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Instance of a range query for a particular spatial index.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses AbstractRStarTree
 * @apiviz.uses SpatialPrimitiveDistanceFunction
 */
public class GenericRStarTreeRangeQuery<O extends SpatialComparable, D extends Distance<D>> extends AbstractDistanceRangeQuery<O, D> {
  /**
   * The index to use
   */
  protected final AbstractRStarTree<O, ?, ?> index;

  /**
   * Spatial primitive distance function
   */
  protected final SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction;

  /**
   * Constructor.
   * 
   * @param relation Relation to use
   * @param index Index to use
   * @param distanceQuery Distance query to use
   * @param distanceFunction Distance function
   */
  public GenericRStarTreeRangeQuery(Relation<? extends O> relation, AbstractRStarTree<O, ?, ?> index, DistanceQuery<O, D> distanceQuery, SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction) {
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
  protected List<DistanceResultPair<D>> doRangeQuery(O object, D epsilon) {
    final List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
    final Heap<GenericDistanceSearchCandidate<D>> pq = new Heap<GenericDistanceSearchCandidate<D>>();

    // push root
    pq.add(new GenericDistanceSearchCandidate<D>(distanceFunction.getDistanceFactory().nullDistance(), index.getRootEntry().getEntryID()));

    // search in tree
    while(!pq.isEmpty()) {
      GenericDistanceSearchCandidate<D> pqNode = pq.poll();
      if(pqNode.mindist.compareTo(epsilon) > 0) {
        break;
      }

      AbstractRStarTreeNode<?, ?> node = index.getNode(pqNode.nodeID);
      final int numEntries = node.getNumEntries();

      for(int i = 0; i < numEntries; i++) {
        D distance = distanceFunction.minDist(node.getEntry(i), object);
        if(distance.compareTo(epsilon) <= 0) {
          if(node.isLeaf()) {
            LeafEntry entry = (LeafEntry) node.getEntry(i);
            result.add(new GenericDistanceResultPair<D>(distance, entry.getDBID()));
          }
          else {
            DirectoryEntry entry = (DirectoryEntry) node.getEntry(i);
            pq.add(new GenericDistanceSearchCandidate<D>(distance, entry.getEntryID()));
          }
        }
      }
    }

    // sort the result according to the distances
    Collections.sort(result);
    return result;
  }

  @Override
  public List<DistanceResultPair<D>> getRangeForObject(O obj, D range) {
    return doRangeQuery(obj, range);
  }

  @Override
  public List<DistanceResultPair<D>> getRangeForDBID(DBID id, D range) {
    return getRangeForObject(relation.get(id), range);
  }

  @SuppressWarnings("unused")
  @Override
  public List<List<DistanceResultPair<D>>> getRangeForBulkDBIDs(ArrayDBIDs ids, D range) {
    // TODO: implement
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }
}