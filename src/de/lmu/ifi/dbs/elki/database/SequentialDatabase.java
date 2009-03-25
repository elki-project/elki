package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.KNNList;
import de.lmu.ifi.dbs.elki.utilities.pairs.ComparablePair;

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

    public <D extends Distance<D>> List<ComparablePair<D, Integer>> kNNQueryForObject(O queryObject,
                                                                          int k,
                                                                          DistanceFunction<O, D> distanceFunction) {
        KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());
        Iterator<Integer> iterator = iterator();
        while (iterator.hasNext()) {
            Integer candidateID = iterator.next();
            O candidate = get(candidateID);
            knnList.add(new ComparablePair<D, Integer>(distanceFunction.distance(queryObject, candidate), candidateID));
        }
        return knnList.toList();
    }

    public <D extends Distance<D>> List<ComparablePair<D, Integer>> kNNQueryForID(Integer id,
                                                                      int k,
                                                                      DistanceFunction<O, D> distanceFunction) {
        O object = get(id);
        KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());

        Iterator<Integer> iterator = iterator();
        while (iterator.hasNext()) {
            Integer candidateID = iterator.next();
            O candidate = get(candidateID);
            knnList.add(new ComparablePair<D, Integer>(distanceFunction.distance(object, candidate), candidateID));
        }
        return knnList.toList();
    }

    public <D extends Distance<D>> List<List<ComparablePair<D, Integer>>> bulkKNNQueryForID(List<Integer> ids, int k, DistanceFunction<O, D> distanceFunction) {
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
                knnList.add(new ComparablePair<D, Integer>(distanceFunction.distance(object, candidate), candidateID));
            }
        }

        List<List<ComparablePair<D, Integer>>> result = new ArrayList<List<ComparablePair<D, Integer>>>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            result.add(knnLists.get(i).toList());
        }
        return result;
    }

    public <D extends Distance<D>> List<ComparablePair<D, Integer>> rangeQuery(Integer id,
                                                                   String epsilon,
                                                                   DistanceFunction<O, D> distanceFunction) {
        List<ComparablePair<D, Integer>> result = new ArrayList<ComparablePair<D, Integer>>();
        D distance = distanceFunction.valueOf(epsilon);
        Iterator<Integer> iterator = iterator();
        while (iterator.hasNext()) {
            Integer currentID = iterator.next();
            D currentDistance = distanceFunction.distance(id, currentID);

            if (currentDistance.compareTo(distance) <= 0) {
                result.add(new ComparablePair<D, Integer>(currentDistance, currentID));
            }
        }
        Collections.sort(result);
        return result;
    }

    public <D extends Distance<D>> List<ComparablePair<D, Integer>> reverseKNNQuery(Integer id,
                                                                        int k,
                                                                        DistanceFunction<O, D> distanceFunction) {
        List<ComparablePair<D, Integer>> result = new ArrayList<ComparablePair<D, Integer>>();
        for (Iterator<Integer> iter = iterator(); iter.hasNext();) {
            Integer candidateID = iter.next();
            List<ComparablePair<D, Integer>> knns = this.kNNQueryForID(candidateID, k, distanceFunction);
            for (ComparablePair<D, Integer> knn : knns) {
                if (knn.getSecond() == id) {
                    result.add(new ComparablePair<D, Integer>(knn.getFirst(), candidateID));
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
