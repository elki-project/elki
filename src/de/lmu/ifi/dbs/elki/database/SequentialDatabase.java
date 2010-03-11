package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.KNNList;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;
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
   * Retrieves the k nearest neighbors for the query object.
   * 
   * The result contains always exactly k objects, including the query object if
   * it is an element of the database.
   * 
   * Ties in case of equal distances are resolved by the underlying
   * {@link KNNList}, see {@link KNNList#add(DistanceResultPair)}.
   * 
   * @see Database#kNNQueryForObject(DatabaseObject, int, DistanceFunction)
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
   * Retrieves the k nearest neighbors for the query object.
   * 
   * The result contains always exactly k objects.
   * 
   * Ties in case of equal distances are resolved by the underlying
   * {@link KNNList}, see {@link KNNList#add(DistanceResultPair)}.
   * 
   * @see Database#kNNQueryForObject(DatabaseObject, int, DistanceFunction)
   */
  public <D extends Distance<D>> List<DistanceResultPair<D>> kNNQueryForID(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
    O object = get(id);
    KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());

    for(Integer candidateID : this) {
      O candidate = get(candidateID);
      knnList.add(new DistanceResultPair<D>(distanceFunction.distance(object, candidate), candidateID));
    }
    return knnList.toList();
  }

  /**
   * Retrieves the k nearest neighbors for the query objects.
   * 
   * The result contains always exactly k objects.
   * 
   * Ties in case of equal distances are resolved by the underlying
   * {@link KNNList}, see {@link KNNList#add(DistanceResultPair)}.
   * 
   * @see Database#kNNQueryForObject(DatabaseObject, int, DistanceFunction)
   */
  public <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkKNNQueryForID(List<Integer> ids, int k, DistanceFunction<O, D> distanceFunction) {
    List<KNNList<D>> knnLists = new ArrayList<KNNList<D>>(ids.size());
    for(@SuppressWarnings("unused")
    Integer i : this) {
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

    List<List<DistanceResultPair<D>>> result = new ArrayList<List<DistanceResultPair<D>>>(ids.size());
    for(Integer i : this) {
      result.add(knnLists.get(i).toList());
    }
    return result;
  }

  public <D extends Distance<D>> List<DistanceResultPair<D>> rangeQuery(Integer id, String epsilon, DistanceFunction<O, D> distanceFunction) {
    List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
    D distance = distanceFunction.valueOf(epsilon);
    for(Integer currentID : this) {
      D currentDistance = distanceFunction.distance(id, currentID);
      if(currentDistance.compareTo(distance) <= 0) {
        result.add(new DistanceResultPair<D>(currentDistance, currentID));
      }
    }
    Collections.sort(result);
    return result;
  }

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
     * 
     */
  public <D extends Distance<D>> List<DistanceResultPair<D>> reverseKNNQuery(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
    List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
    for(Integer candidateID : this) {
      List<DistanceResultPair<D>> knns = this.kNNQueryForID(candidateID, k, distanceFunction);
      for(DistanceResultPair<D> knn : knns) {
        if(knn.getID() == id) {
          result.add(new DistanceResultPair<D>(knn.getDistance(), candidateID));
        }
      }
    }
    Collections.sort(result);
    return result;
  }

  @Override
  protected Collection<Pair<OptionID, Object>> getParameters() {
    return new java.util.Vector<Pair<OptionID, Object>>();
  }
}
