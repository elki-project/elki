package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;

/**
 * Instance of this query for a particular database.
 * 
 * This is a subtle optimization: for primitive queries, it is clearly faster to
 * retrieve the query object from the relation only once!
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses PrimitiveDistanceQuery
 */
public class LinearScanPrimitiveDistanceKNNQuery<O, D extends Distance<D>> extends LinearScanKNNQuery<O, D> {
  /**
   * Constructor.
   * 
   * @param relation Data to query
   * @param distanceQuery Distance function to use
   */
  public LinearScanPrimitiveDistanceKNNQuery(Relation<? extends O> relation, PrimitiveDistanceQuery<O, D> distanceQuery) {
    super(relation, distanceQuery);
  }

  @Override
  public List<DistanceResultPair<D>> getKNNForDBID(DBID id, int k) {
    return getKNNForObject(relation.get(id), k);
  }

  @Override
  public List<List<DistanceResultPair<D>>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    final List<KNNHeap<D>> heaps = new ArrayList<KNNHeap<D>>(ids.size());
    // TODO: for large bulk queries, this array may also become large - the full database!
    List<O> objs = new ArrayList<O>(ids.size());
    for(DBID id : ids) {
      heaps.add(new KNNHeap<D>(k));
      objs.add(relation.get(id));
    }
    // The distance is computed on arbitrary vectors, we can reduce object
    // loading by working on the actual vectors.
    for(DBID candidateID : relation.iterDBIDs()) {
      O candidate = relation.get(candidateID);
      for(int index = 0; index < ids.size(); index++) {
        O object = objs.get(index);
        KNNHeap<D> heap = heaps.get(index);
        heap.add(distanceQuery.distance(object, candidate), candidateID);
      }
    }

    List<List<DistanceResultPair<D>>> result = new ArrayList<List<DistanceResultPair<D>>>(heaps.size());
    for(KNNHeap<D> heap : heaps) {
      result.add(heap.toSortedArrayList());
    }
    return result;
  }
}