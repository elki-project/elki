package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.KNNList;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
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
   * each object to a {@link KNNList}.
   * 
   * @see KNNList#add(DistanceResultPair)
   */
  public <D extends Distance<D>> List<DistanceResultPair<D>> kNNQueryForObject(O queryObject, int k, DistanceFunction<O, D> distanceFunction) {
    KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());
    for(Integer candidateID : this) {
      O candidate = get(candidateID);
      knnList.add(new DistanceResultPair<D>(distanceFunction.distance(queryObject, candidate), candidateID));
    }
    return knnList.toList();
  }

  /**
   * Retrieves the k-nearest neighbors (kNN) for the query objects performing
   * one sequential scan on this database. For each query id a {@link KNNList}
   * is assigned. The kNNs are determined by trying to add each object to all
   * KNNLists.
   * 
   * @see KNNList#add(DistanceResultPair)
   */
  @Override
  public <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkKNNQueryForID(List<Integer> ids, int k, DistanceFunction<O, D> distanceFunction) {
    List<KNNList<D>> knnLists = new ArrayList<KNNList<D>>(ids.size());
    for(@SuppressWarnings("unused")
    Integer i : ids) {
      knnLists.add(new KNNList<D>(k, distanceFunction.infiniteDistance()));
    }

    for(Integer candidateID : this) {
      O candidate = get(candidateID);
      Integer index = -1;
      for(Integer id : ids) {
        index++;
        O object = get(id);
        KNNList<D> knnList = knnLists.get(index);
        knnList.add(new DistanceResultPair<D>(distanceFunction.distance(object, candidate), candidateID));
      }
    }

    List<List<DistanceResultPair<D>>> result = new ArrayList<List<DistanceResultPair<D>>>(knnLists.size());
    for(KNNList<D> knnList : knnLists) {
      result.add(knnList.toList());
    }
    return result;
  }

  /**
   * Retrieves the epsilon-neighborhood of the query object performing a
   * sequential scan on this database.
   */
  public <D extends Distance<D>> List<DistanceResultPair<D>> rangeQuery(Integer id, D epsilon, DistanceFunction<O, D> distanceFunction) {
    List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
    for(Integer currentID : this) {
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
  public <D extends Distance<D>> List<DistanceResultPair<D>> reverseKNNQueryForID(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
    List<Integer> ids = new ArrayList<Integer>();
    ids.add(id);
    return sequentialBulkReverseKNNQueryForID(ids, k, distanceFunction).get(0);
  }

  /**
   * Retrieves the reverse k-nearest neighbors (RkNN) for the query object by
   * performing a bulk knn query for all objects. If a query object is an
   * element of the kNN of an object o, o belongs to the particular query
   * result.
   */
  public <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkReverseKNNQueryForID(List<Integer> ids, int k, DistanceFunction<O, D> distanceFunction) {
    return sequentialBulkReverseKNNQueryForID(ids, k, distanceFunction);
  }

  @Override
  protected Collection<Pair<OptionID, Object>> getParameters() {
    return new java.util.Vector<Pair<OptionID, Object>>();
  }
}
