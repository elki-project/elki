package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;

/**
 * Instance of this query for a particular database.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has DistanceQuery
 */
public class LinearScanKNNQuery<O, D extends Distance<D>> extends AbstractDistanceKNNQuery<O, D> implements LinearScanQuery {
  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  public LinearScanKNNQuery(DistanceQuery<O, D> distanceQuery) {
    super(distanceQuery);
  }

  /**
   * Linear batch knn for arbitrary distance functions.
   * 
   * @param ids DBIDs to process
   * @param heaps Heaps to store the results in
   */
  private void linearScanBatchKNN(ArrayDBIDs ids, List<KNNHeap<D>> heaps) {
    // The distance is computed on database IDs
    for(DBID candidateID : relation.iterDBIDs()) {
      Integer index = -1;
      for(DBID id : ids) {
        index++;
        KNNHeap<D> heap = heaps.get(index);
        heap.add(distanceQuery.distance(id, candidateID), candidateID);
      }
    }
  }

  @Override
  public List<DistanceResultPair<D>> getKNNForDBID(DBID id, int k) {
    KNNHeap<D> heap = new KNNHeap<D>(k);
    if(PrimitiveDistanceQuery.class.isInstance(distanceQuery)) {
      O obj = relation.get(id);
      for(DBID candidateID : relation.iterDBIDs()) {
        heap.add(distanceQuery.distance(obj, relation.get(candidateID)), candidateID);
      }
    }
    else {
      for(DBID candidateID : relation.iterDBIDs()) {
        heap.add(distanceQuery.distance(id, candidateID), candidateID);
      }
    }
    return heap.toSortedArrayList();
  }

  @Override
  public List<List<DistanceResultPair<D>>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    final int size = ids.size();
    final List<KNNHeap<D>> heaps = new ArrayList<KNNHeap<D>>(size);
    for(int i = 0; i < size; i++) {
      heaps.add(new KNNHeap<D>(k));
    }
    linearScanBatchKNN(ids, heaps);
    // Serialize heaps
    List<List<DistanceResultPair<D>>> result = new ArrayList<List<DistanceResultPair<D>>>(size);
    for(KNNHeap<D> heap : heaps) {
      result.add(heap.toSortedArrayList());
    }
    return result;
  }

  @Override
  public void getKNNForBulkHeaps(Map<DBID, KNNHeap<D>> heaps) {
    final int size = heaps.size();
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(size);
    List<KNNHeap<D>> kheaps = new ArrayList<KNNHeap<D>>(size);
    for(Entry<DBID, KNNHeap<D>> ent : heaps.entrySet()) {
      ids.add(ent.getKey());
      kheaps.add(ent.getValue());
    }
    linearScanBatchKNN(ids, kheaps);
  }

  @Override
  public List<DistanceResultPair<D>> getKNNForObject(O obj, int k) {
    KNNHeap<D> heap = new KNNHeap<D>(k);
    for(DBID candidateID : relation.iterDBIDs()) {
      O candidate = relation.get(candidateID);
      heap.add(distanceQuery.distance(obj, candidate), candidateID);
    }
    return heap.toSortedArrayList();
  }
}