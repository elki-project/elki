package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * SequentialDatabase is a simple implementation of a Database.
 * <p/>
 * It does not support any index structure and holds all objects in main memory
 * (as a Map).
 * 
 * @author Arthur Zimek
 * @param <O> the type of FeatureVector as element of the database
 */
@Description("Database using an in-memory hashtable and doing linear scans.")
public class SequentialDatabase<O extends DatabaseObject> extends AbstractDatabase<O> implements Parameterizable {
  /**
   * Provides a database for main memory holding all objects in a hashtable.
   */
  public SequentialDatabase() {
    super();
  }

  /**
   * Retrieves the k-nearest neighbors (kNN) for the query object performing a
   * sequential scan on this database. The kNN are determined by trying to add
   * each object to a {@link KNNHeap}.
   */
  public <D extends Distance<D>> List<DistanceResultPair<D>> kNNQueryForObject(O queryObject, int k, DistanceFunction<O, D> distanceFunction) {
    KNNHeap<D> heap = new KNNHeap<D>(k);
    for(DBID candidateID : this) {
      O candidate = get(candidateID);
      heap.add(new DistanceResultPair<D>(distanceFunction.distance(queryObject, candidate), candidateID));
    }
    return heap.toSortedArrayList();
  }

  /**
   * Retrieves the k-nearest neighbors (kNN) for the query objects performing
   * one sequential scan on this database. For each query id a {@link KNNHeap}
   * is assigned. The kNNs are determined by trying to add each object to all
   * KNNHeap.
   */
  @Override
  public <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkKNNQueryForID(ArrayDBIDs ids, int k, DistanceFunction<O, D> distanceFunction) {
    List<KNNHeap<D>> heaps = new ArrayList<KNNHeap<D>>(ids.size());
    for(int i = 0; i < ids.size(); i++) {
      heaps.add(new KNNHeap<D>(k));
    }

    for(DBID candidateID : this) {
      O candidate = get(candidateID);
      Integer index = -1;
      for(DBID id : ids) {
        index++;
        O object = get(id);
        KNNHeap<D> heap = heaps.get(index);
        heap.add(new DistanceResultPair<D>(distanceFunction.distance(object, candidate), candidateID));
      }
    }

    List<List<DistanceResultPair<D>>> result = new ArrayList<List<DistanceResultPair<D>>>(heaps.size());
    for(KNNHeap<D> heap : heaps) {
      result.add(heap.toSortedArrayList());
    }
    return result;
  }

  /**
   * Retrieves the epsilon-neighborhood of the query object performing a
   * sequential scan on this database.
   */
  public <D extends Distance<D>> List<DistanceResultPair<D>> rangeQuery(DBID id, D epsilon, DistanceFunction<O, D> distanceFunction) {
    List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
    for(DBID currentID : this) {
      D currentDistance = distanceFunction.distance(id, currentID);
      if(currentDistance.compareTo(epsilon) <= 0) {
        result.add(new DistanceResultPair<D>(currentDistance, currentID));
      }
    }
    Collections.sort(result);
    return result;
  }

  /**
   * Retrieves the reverse k-nearest neighbors (RkNN) for the query object by
   * performing a bulk knn query for all objects. If the query object is an
   * element of the kNN of an object o, o belongs to the query result.
   */
  public <D extends Distance<D>> List<DistanceResultPair<D>> reverseKNNQueryForID(DBID id, int k, DistanceFunction<O, D> distanceFunction) {
    return sequentialBulkReverseKNNQueryForID(id, k, distanceFunction).get(0);
  }

  /**
   * Retrieves the reverse k-nearest neighbors (RkNN) for the query object by
   * performing a bulk knn query for all objects. If a query object is an
   * element of the kNN of an object o, o belongs to the particular query
   * result.
   */
  public <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkReverseKNNQueryForID(ArrayDBIDs ids, int k, DistanceFunction<O, D> distanceFunction) {
    return sequentialBulkReverseKNNQueryForID(ids, k, distanceFunction);
  }

  @Override
  protected Collection<Pair<OptionID, Object>> getParameters() {
    return new java.util.Vector<Pair<OptionID, Object>>();
  }
}
