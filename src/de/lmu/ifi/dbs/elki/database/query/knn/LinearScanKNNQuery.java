package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.KNNHeap;

/**
 * Instance of this query for a particular database.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has DistanceQuery
 */
public class LinearScanKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceKNNQuery<O, D> implements LinearScanQuery {
  /**
   * Constructor.
   * 
   * @param database Database to query
   * @param distanceQuery Distance function to use
   */
  public LinearScanKNNQuery(Database<O> database, DistanceQuery<O, D> distanceQuery) {
    super(database, distanceQuery);
  }

  @Override
  public List<DistanceResultPair<D>> getKNNForDBID(DBID id, int k) {
    KNNHeap<D> heap = new KNNHeap<D>(k);
    for(DBID candidateID : database) {
      heap.add(new DistanceResultPair<D>(distanceQuery.distance(id, candidateID), candidateID));
    }
    return heap.toSortedArrayList();
  }

  @Override
  public List<List<DistanceResultPair<D>>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    List<KNNHeap<D>> heaps = new ArrayList<KNNHeap<D>>(ids.size());
    for(int i = 0; i < ids.size(); i++) {
      heaps.add(new KNNHeap<D>(k));
    }

    if(PrimitiveDistanceQuery.class.isAssignableFrom(distanceQuery.getClass())) {
      // The distance is computed on arbitrary vectors, we can reduce object
      // loading by working on the actual vectors.
      for(DBID candidateID : database) {
        O candidate = database.get(candidateID);
        Integer index = -1;
        for(DBID id : ids) {
          index++;
          O object = database.get(id);
          KNNHeap<D> heap = heaps.get(index);
          heap.add(new DistanceResultPair<D>(distanceQuery.distance(object, candidate), candidateID));
        }
      }
    }
    else {
      // The distance is computed on database IDs
      for(DBID candidateID : database) {
        Integer index = -1;
        for(DBID id : ids) {
          index++;
          KNNHeap<D> heap = heaps.get(index);
          heap.add(new DistanceResultPair<D>(distanceQuery.distance(id, candidateID), candidateID));
        }
      }
    }

    List<List<DistanceResultPair<D>>> result = new ArrayList<List<DistanceResultPair<D>>>(heaps.size());
    for(KNNHeap<D> heap : heaps) {
      result.add(heap.toSortedArrayList());
    }
    return result;
  }

  @Override
  public List<DistanceResultPair<D>> getKNNForObject(O obj, int k) {
    KNNHeap<D> heap = new KNNHeap<D>(k);
    for(DBID candidateID : database) {
      O candidate = database.get(candidateID);
      heap.add(new DistanceResultPair<D>(distanceQuery.distance(obj, candidate), candidateID));
    }
    return heap.toSortedArrayList();
  }
}