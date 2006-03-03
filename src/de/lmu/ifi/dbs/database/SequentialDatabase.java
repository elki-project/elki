package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * SequentialDatabase is a simple implementation of a Database. <p/> It does not
 * support any index structure and holds all objects in main memory (as a Map).
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class SequentialDatabase<O extends DatabaseObject> extends AbstractDatabase<O> {
  /**
   * Provides a database for main memory holding all objects in a hashtable.
   */
  public SequentialDatabase() {
    super();
  }

  /**
   * @see Database#kNNQueryForObject(DatabaseObject, int, de.lmu.ifi.dbs.distance.DistanceFunction)
   */
  public <D extends Distance<D>> List<QueryResult<D>> kNNQueryForObject(O queryObject,
                                                                        int k,
                                                                        DistanceFunction<O, D> distanceFunction) {
    // needed for cached distances:
    distanceFunction.setDatabase(this, false);

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
   *      int, de.lmu.ifi.dbs.distance.DistanceFunction)
   */
  public <D extends Distance<D>> List<QueryResult<D>> kNNQueryForID(Integer id,
                                                                    int k,
                                                                    DistanceFunction<O, D> distanceFunction) {
    distanceFunction.setDatabase(this, false);

    KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());

    Iterator<Integer> iterator = iterator();
    while (iterator.hasNext()) {
      Integer candidateID = iterator.next();
      knnList.add(new QueryResult<D>(candidateID, distanceFunction.distance(id, candidateID)));
    }
    return knnList.toList();
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#rangeQuery(java.lang.Integer,
   *      java.lang.String, de.lmu.ifi.dbs.distance.DistanceFunction)
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
   * @see Database#reverseKNNQuery(Integer, int, DistanceFunction)
   */
  public <D extends Distance> List<QueryResult<D>> reverseKNNQuery(Integer id,
                                                                   int k,
                                                                   DistanceFunction<O, D> distanceFunction) {
    distanceFunction.setDatabase(this, false);

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
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(SequentialDatabase.class.getName());
    description.append(" holds all the data in main memory backed by a Hashtable.");
    return description.toString();
  }

}
