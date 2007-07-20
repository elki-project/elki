package de.lmu.ifi.dbs.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;

/**
 * SequentialDatabase is a simple implementation of a Database. <p/> It does not
 * support any index structure and holds all objects in main memory (as a Map).
 *
 * @author Arthur Zimek 
 */
public class SequentialDatabase<O extends DatabaseObject> extends AbstractDatabase<O> {
  /**
   * Provides a database for main memory holding all objects in a hashtable.
   */
  public SequentialDatabase() {
    super();
  }

  /**
   * @see Database#kNNQueryForObject(DatabaseObject, int, de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction)
   */
  public <D extends Distance<D>> List<QueryResult<D>> kNNQueryForObject(O queryObject,
                                                                        int k,
                                                                        DistanceFunction<O, D> distanceFunction) {
    KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());
    Iterator<Integer> iterator = iterator();
    while (iterator.hasNext()) {
      Integer candidateID = iterator.next();
      O candidate = get(candidateID);
      knnList.add(new QueryResult<D>(candidateID, distanceFunction.distance(queryObject, candidate)));
    }
    return knnList.toList();
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#kNNQueryForID(java.lang.Integer,
   *      int, de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction)
   */
  public <D extends Distance<D>> List<QueryResult<D>> kNNQueryForID(Integer id,
                                                                    int k,
                                                                    DistanceFunction<O, D> distanceFunction) {
    O object = get(id);
    KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());

    Iterator<Integer> iterator = iterator();
    while (iterator.hasNext()) {
      Integer candidateID = iterator.next();
      O candidate = get(candidateID);
      knnList.add(new QueryResult<D>(candidateID, distanceFunction.distance(object, candidate)));
    }
    return knnList.toList();
  }

  /**
   * @see Database#bulkKNNQueryForID(java.util.List, int, de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction)
   */
  public <D extends Distance<D>>List<List<QueryResult<D>>> bulkKNNQueryForID(List<Integer> ids, int k, DistanceFunction<O, D> distanceFunction) {
    List<KNNList<D>> knnLists = new ArrayList<KNNList<D>>(ids.size());
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < ids.size(); i++) {
      knnLists.add(new KNNList<D>(k, distanceFunction.infiniteDistance()));
    }

    Iterator<Integer> iterator = iterator();
    while (iterator.hasNext()) {
      Integer candidateID = iterator.next();
      O candidate = get(candidateID);
      for (int i = 0; i < ids.size(); i++) {
        Integer id = ids.get(i);
        O object = get(id);
        KNNList<D> knnList = knnLists.get(i);
        knnList.add(new QueryResult<D>(candidateID, distanceFunction.distance(object, candidate)));
      }
    }

    List<List<QueryResult<D>>> result = new ArrayList<List<QueryResult<D>>>(ids.size());
    for (int i = 0; i < ids.size(); i++) {
      result.add(knnLists.get(i).toList());
    }
    return result;
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#rangeQuery(java.lang.Integer,
   *      java.lang.String, de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction)
   */
  public <D extends Distance<D>> List<QueryResult<D>> rangeQuery(Integer id,
                                                                 String epsilon,
                                                                 DistanceFunction<O, D> distanceFunction) {
    List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
    D distance = distanceFunction.valueOf(epsilon);
    Iterator<Integer> iterator = iterator();
    while (iterator.hasNext()) {
      Integer currentID = iterator.next();
      D currentDistance = distanceFunction.distance(id, currentID);

      if (currentDistance.compareTo(distance) <= 0) {
        result.add(new QueryResult<D>(currentID, currentDistance));
      }
    }
    Collections.sort(result);
    return result;
  }

  /**
   * @see Database#reverseKNNQuery(Integer, int, de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction)
   */
  public <D extends Distance<D>> List<QueryResult<D>> reverseKNNQuery(Integer id,
                                                                   int k,
                                                                   DistanceFunction<O, D> distanceFunction) {
    List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
    for (Iterator<Integer> iter = iterator(); iter.hasNext();) {
      Integer candidateID = iter.next();
      List<QueryResult<D>> knns = this.kNNQueryForID(candidateID, k, distanceFunction);
      for (QueryResult<D> knn : knns) {
        if (knn.getID() == id) {
          result.add(new QueryResult<D>(candidateID, knn.getDistance()));
        }
      }
    }
    Collections.sort(result);
    return result;
  }

  /**
   * Provides a description for SequentialDatabase.
   *
   * @see Database#description()
   */
  @Override
public String description() {
    StringBuffer description = new StringBuffer();
    description.append(SequentialDatabase.class.getName());
    description.append(" holds all the data in main memory backed by a Hashtable.");
    return description.toString();
  }

}
