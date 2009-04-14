package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.KNNList;

/**
 * SequentialDatabase is a simple implementation of a Database. <p/> It does not
 * support any index structure and holds all objects in main memory (as a Map).
 *
 * @author Arthur Zimek
 * @param <O> the type of FeatureVector as element of the database
 */
public class SequentialDatabase<O extends DatabaseObject> extends AbstractDatabase<O> {
    /**
     * Provides a database for main memory holding all objects in a hashtable.
     */
    public SequentialDatabase() {
        super();
    }

    public <D extends Distance<D>> List<DistanceResultPair<D>> kNNQueryForObject(O queryObject,
                                                                          int k,
                                                                          DistanceFunction<O, D> distanceFunction) {
        KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());
        for(Integer candidateID : this){
            O candidate = get(candidateID);
            knnList.add(new DistanceResultPair<D>(distanceFunction.distance(queryObject, candidate), candidateID));
        }
        return knnList.toList();
    }

    public <D extends Distance<D>> List<DistanceResultPair<D>> kNNQueryForID(Integer id,
                                                                      int k,
                                                                      DistanceFunction<O, D> distanceFunction) {
        O object = get(id);
        KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());

        Iterator<Integer> iterator = iterator();
        while (iterator.hasNext()) {
            Integer candidateID = iterator.next();
            O candidate = get(candidateID);
            knnList.add(new DistanceResultPair<D>(distanceFunction.distance(object, candidate), candidateID));
        }
        return knnList.toList();
    }

    public <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkKNNQueryForID(List<Integer> ids, int k, DistanceFunction<O, D> distanceFunction) {
      List<KNNList<D>> knnLists = new ArrayList<KNNList<D>>(ids.size());
      for(@SuppressWarnings("unused") Integer i : this) {
        knnLists.add(new KNNList<D>(k, distanceFunction.infiniteDistance()));
      }

      for(Integer candidateID : this) {
        O candidate = get(candidateID);
        for (int i = 0; i < ids.size(); i++) {
          Integer id = ids.get(i);
          O object = get(id);
          KNNList<D> knnList = knnLists.get(i);
          knnList.add(new DistanceResultPair<D>(distanceFunction.distance(object, candidate), candidateID));
        }
      }

      List<List<DistanceResultPair<D>>> result = new ArrayList<List<DistanceResultPair<D>>>(ids.size());
      for (Integer i : this) {
        result.add(knnLists.get(i).toList());
      }
      return result;
    }

    public <D extends Distance<D>> List<DistanceResultPair<D>> rangeQuery(Integer id,
                                                                   String epsilon,
                                                                   DistanceFunction<O, D> distanceFunction) {
      List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
      D distance = distanceFunction.valueOf(epsilon);
      for(Integer currentID : this){
        D currentDistance = distanceFunction.distance(id, currentID);
        if (currentDistance.compareTo(distance) <= 0) {
          result.add(new DistanceResultPair<D>(currentDistance, currentID));
        }
      }
      Collections.sort(result);
      return result;
    }

    public <D extends Distance<D>> List<DistanceResultPair<D>> reverseKNNQuery(Integer id,
                                                                        int k,
                                                                        DistanceFunction<O, D> distanceFunction) {
      List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
      for (Integer candidateID : this){
        List<DistanceResultPair<D>> knns = this.kNNQueryForID(candidateID, k, distanceFunction);
        for (DistanceResultPair<D> knn : knns) {
          if (knn.getID() == id) {
            result.add(new DistanceResultPair<D>(knn.getDistance(), candidateID));
          }
        }
      }
      Collections.sort(result);
      return result;
    }

    /**
     * Provides a description for SequentialDatabase.
     */
    @Override
    public String parameterDescription() {
        StringBuffer description = new StringBuffer();
        description.append(SequentialDatabase.class.getName());
        description.append(" holds all the data in main memory backed by a Hashtable.");
        return description.toString();
    }

}
