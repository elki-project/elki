package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.AbstractDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.DirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.query.GenericDistanceSearchCandidate;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.UpdatableHeap;

/**
 * Instance of a KNN query for a particular spatial index.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses AbstractRStarTree
 * @apiviz.uses SpatialPrimitiveDistanceFunction
 */
public class GenericRStarTreeKNNQuery<O extends SpatialComparable, D extends Distance<D>> extends AbstractDistanceKNNQuery<O, D> {
  /**
   * The index to use
   */
  protected final AbstractRStarTree<?, ?> tree;

  /**
   * Spatial primitive distance function
   */
  protected final SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction;

  /**
   * Constructor.
   * 
   * @param tree Index to use
   * @param distanceQuery Distance query to use
   */
  public GenericRStarTreeKNNQuery(AbstractRStarTree<?, ?> tree, SpatialDistanceQuery<O, D> distanceQuery) {
    super(distanceQuery);
    this.tree = tree;
    this.distanceFunction = distanceQuery.getDistanceFunction();
  }

  /**
   * Performs a k-nearest neighbor query for the given NumberVector with the
   * given parameter k and the according distance function. The query result is
   * in ascending order to the distance to the query object.
   * 
   * @param object the query object
   * @param knnList the knn list containing the result
   */
  protected void doKNNQuery(O object, KNNHeap<D> knnList) {
    final Heap<GenericDistanceSearchCandidate<D>> pq = new UpdatableHeap<GenericDistanceSearchCandidate<D>>();

    // push root
    pq.add(new GenericDistanceSearchCandidate<D>(distanceFunction.getDistanceFactory().nullDistance(), tree.getRootID()));
    D maxDist = distanceFunction.getDistanceFactory().infiniteDistance();

    // search in tree
    while(!pq.isEmpty()) {
      GenericDistanceSearchCandidate<D> pqNode = pq.poll();

      if(pqNode.mindist.compareTo(maxDist) > 0) {
        return;
      }

      AbstractRStarTreeNode<?, ?> node = tree.getNode(pqNode.nodeID);
      // data node
      if(node.isLeaf()) {
        for(int i = 0; i < node.getNumEntries(); i++) {
          SpatialEntry entry = node.getEntry(i);
          D distance = distanceFunction.minDist(entry, object);
          tree.distanceCalcs++;
          if(distance.compareTo(maxDist) <= 0) {
            knnList.add(distance, ((LeafEntry) entry).getDBID());
            maxDist = knnList.getKNNDistance();
          }
        }
      }
      // directory node
      else {
        for(int i = 0; i < node.getNumEntries(); i++) {
          SpatialEntry entry = node.getEntry(i);
          D distance = distanceFunction.minDist(entry, object);
          tree.distanceCalcs++;
          if(distance.compareTo(maxDist) <= 0) {
            pq.add(new GenericDistanceSearchCandidate<D>(distance, ((DirectoryEntry)entry).getPageID()));
          }
        }
      }
    }
  }

  /**
   * Performs a batch knn query.
   * 
   * @param node the node for which the query should be performed
   * @param knnLists a map containing the knn lists for each query objects
   */
  protected void batchNN(AbstractRStarTreeNode<?, ?> node, Map<DBID, KNNHeap<D>> knnLists) {
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        SpatialEntry p = node.getEntry(i);
        for(Entry<DBID, KNNHeap<D>> ent : knnLists.entrySet()) {
          final DBID q = ent.getKey();
          final KNNHeap<D> knns_q = ent.getValue();
          D knn_q_maxDist = knns_q.getKNNDistance();

          DBID pid = ((LeafEntry) p).getDBID();
          // FIXME: objects are NOT accessible by DBID in a plain rtree context!
          D dist_pq = distanceQuery.distance(pid, q);
          if(dist_pq.compareTo(knn_q_maxDist) <= 0) {
            knns_q.add(dist_pq, pid);
          }
        }
      }
    }
    else {
      ModifiableDBIDs ids = DBIDUtil.newArray(knnLists.size());
      ids.addAll(knnLists.keySet());
      List<DistanceEntry<D, SpatialEntry>> entries = getSortedEntries(node, ids);
      for(DistanceEntry<D, SpatialEntry> distEntry : entries) {
        D minDist = distEntry.getDistance();
        for(Entry<DBID, KNNHeap<D>> ent : knnLists.entrySet()) {
          final KNNHeap<D> knns_q = ent.getValue();
          D knn_q_maxDist = knns_q.getKNNDistance();

          if(minDist.compareTo(knn_q_maxDist) <= 0) {
            SpatialEntry entry = distEntry.getEntry();
            AbstractRStarTreeNode<?, ?> child = tree.getNode(((DirectoryEntry)entry).getPageID());
            batchNN(child, knnLists);
            break;
          }
        }
      }
    }
  }

  @Override
  public void getKNNForBulkHeaps(Map<DBID, KNNHeap<D>> heaps) {
    AbstractRStarTreeNode<?, ?> root = tree.getRoot();
    batchNN(root, heaps);
  }

  /**
   * Sorts the entries of the specified node according to their minimum distance
   * to the specified objects.
   * 
   * @param node the node
   * @param ids the id of the objects
   * @return a list of the sorted entries
   */
  protected List<DistanceEntry<D, SpatialEntry>> getSortedEntries(AbstractRStarTreeNode<?, ?> node, DBIDs ids) {
    List<DistanceEntry<D, SpatialEntry>> result = new ArrayList<DistanceEntry<D, SpatialEntry>>();

    for(int i = 0; i < node.getNumEntries(); i++) {
      SpatialEntry entry = node.getEntry(i);
      D minMinDist = distanceQuery.getDistanceFactory().infiniteDistance();
      for(DBID id : ids) {
        D minDist = distanceFunction.minDist(entry, relation.get(id));
        minMinDist = DistanceUtil.min(minDist, minMinDist);
      }
      result.add(new DistanceEntry<D, SpatialEntry>(entry, minMinDist, i));
    }

    Collections.sort(result);
    return result;
  }

  @Override
  public List<DistanceResultPair<D>> getKNNForObject(O obj, int k) {
    if(k < 1) {
      throw new IllegalArgumentException("At least one enumeration has to be requested!");
    }

    final KNNHeap<D> knnList = new KNNHeap<D>(k, distanceFunction.getDistanceFactory().infiniteDistance());
    doKNNQuery(obj, knnList);
    return knnList.toSortedArrayList();
  }

  @Override
  public List<DistanceResultPair<D>> getKNNForDBID(DBID id, int k) {
    return getKNNForObject(relation.get(id), k);
  }

  @Override
  public List<List<DistanceResultPair<D>>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    if(k < 1) {
      throw new IllegalArgumentException("At least one enumeration has to be requested!");
    }
    // While this works, it seems to be slow at least for large sets!
    final Map<DBID, KNNHeap<D>> knnLists = new HashMap<DBID, KNNHeap<D>>(ids.size());
    for(DBID id : ids) {
      knnLists.put(id, new KNNHeap<D>(k, distanceFunction.getDistanceFactory().infiniteDistance()));
    }

    batchNN(tree.getRoot(), knnLists);

    List<List<DistanceResultPair<D>>> result = new ArrayList<List<DistanceResultPair<D>>>();
    for(DBID id : ids) {
      result.add(knnLists.get(id).toSortedArrayList());
    }
    return result;
  }

  @Override
  public D getDistanceFactory() {
    return distanceQuery.getDistanceFactory();
  }
}