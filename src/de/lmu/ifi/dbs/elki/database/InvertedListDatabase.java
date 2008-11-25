package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DimensionSelectingDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.pairs.SimplePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Database implemented by inverted lists that supports range queries on a specific dimension.
 *
 * @author Elke Achtert
 * @param <O> the type of FeatureVector as element of the database
 * @param <N> the type of the real vector space of the FeatureVector
 */
public class InvertedListDatabase<N extends Number, O extends FeatureVector<O, N>> extends SequentialDatabase<O> {
    /**
     * Map to hold the inverted lists for each dimension.
     */
    private Map<Integer, SortedMap<Double, List<Integer>>> invertedLists = new HashMap<Integer, SortedMap<Double, List<Integer>>>();

    /**
     * @throws de.lmu.ifi.dbs.elki.utilities.UnableToComplyException
     *          if database reached limit of storage capacity
     */
    @Override
    public Integer insert(SimplePair<O, Associations> objectAndAssociations) throws UnableToComplyException {
        Integer id = super.insert(objectAndAssociations);
        O object = objectAndAssociations.getFirst();

        for (int d = 1; d <= object.getDimensionality(); d++) {
            SortedMap<Double, List<Integer>> invertedList = invertedLists.get(d);
            if (invertedList == null) {
                invertedList = new TreeMap<Double, List<Integer>>();
                invertedLists.put(d, invertedList);
            }

            double value = object.getValue(d).doubleValue();
            List<Integer> idList = invertedList.get(value);
            if (idList == null) {
                idList = new ArrayList<Integer>();
                invertedList.put(value, idList);
            }
            idList.add(id);
        }

        return id;
    }

    @Override
    public O delete(Integer id) {
        O object = get(id);
        for (int d = 1; d <= object.getDimensionality(); d++) {
            SortedMap<Double, List<Integer>> invertedList = invertedLists.get(d);
            if (invertedList == null) break;

            double value = object.getValue(d).doubleValue();
            List<Integer> idList = invertedList.get(value);
            if (idList == null) break;
            idList.remove(object.getID());
        }

        return super.delete(id);
    }

    /**
     * Performs a range query for the given object ID with the given epsilon
     * range and the according distance function. The query result is in
     * ascending order to the distance to the query object.
     *
     * @param id               the ID of the query object
     * @param epsilon          the string representation of the query range
     * @param distanceFunction the distance function that computes the distances between the
     *                         objects
     * @return a List of the query results
     */
    @SuppressWarnings("unchecked")
    @Override
    public <D extends Distance<D>> List<QueryResult<D>> rangeQuery(Integer id, String epsilon, DistanceFunction<O, D> distanceFunction) {
        List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();

        if (distanceFunction instanceof DimensionSelectingDistanceFunction) {
            DimensionSelectingDistanceFunction df = (DimensionSelectingDistanceFunction) distanceFunction;
            double eps = df.valueOf(epsilon).getValue();
            int dim = df.getSelectedDimension();
            SortedMap<Double, List<Integer>> invertedList = invertedLists.get(dim);

            O object = get(id);
            double value = object.getValue(dim).doubleValue();
            double from = value - eps;
            double to = value + eps + Double.MIN_VALUE;

            SortedMap<Double, List<Integer>> epsMap = invertedList.subMap(from, to);
            for (Double key : epsMap.keySet()) {
                List<Integer> ids = epsMap.get(key);
                for (Integer currentID : ids) {
                    // noinspection unchecked
                    D currentDistance = (D) df.distance(currentID, id);
                    result.add(new QueryResult<D>(currentID, currentDistance));
                }
            }

            Collections.sort(result);
            return result;
        }
        else {
            return super.rangeQuery(id, epsilon, distanceFunction);
        }
    }

    /**
     * Performs a k-nearest neighbor query for the given object ID. The query
     * result is in ascending order to the distance to the query object.
     *
     * @param id               the ID of the query object
     * @param k                the number of nearest neighbors to be returned
     * @param distanceFunction the distance function that computes the distances between the
     *                         objects
     * @return a List of the query results
     */
    @Override
    public <D extends Distance<D>> List<QueryResult<D>> kNNQueryForID(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
        return super.kNNQueryForID(id, k, distanceFunction);
    }

    /**
     * Performs a k-nearest neighbor query for the given object. The query
     * result is in ascending order to the distance to the query object.
     *
     * @param queryObject      the query object
     * @param k                the number of nearest neighbors to be returned
     * @param distanceFunction the distance function that computes the distances between the
     *                         objects
     * @return a List of the query results
     */
    @Override
    public <D extends Distance<D>> List<QueryResult<D>> kNNQueryForObject(O queryObject, int k, DistanceFunction<O, D> distanceFunction) {
        return super.kNNQueryForObject(queryObject, k, distanceFunction);
    }

    /**
     * Performs k-nearest neighbor queries for the given object IDs. The query
     * result is in ascending order to the distance to the query object.
     *
     * @param ids              the IDs of the query objects
     * @param k                the number of nearest neighbors to be returned
     * @param distanceFunction the distance function that computes the distances between the
     *                         objects
     * @return a List of List of the query results
     */
    @Override
    public <D extends Distance<D>> List<List<QueryResult<D>>> bulkKNNQueryForID(List<Integer> ids, int k, DistanceFunction<O, D> distanceFunction) {
        return super.bulkKNNQueryForID(ids, k, distanceFunction);
    }

    /**
     * Performs a reverse k-nearest neighbor query for the given object ID. The
     * query result is in ascending order to the distance to the query object.
     *
     * @param id               the ID of the query object
     * @param k                the number of nearest neighbors to be returned
     * @param distanceFunction the distance function that computes the distances between the
     *                         objects
     * @return a List of the query results
     */
    @Override
    public <D extends Distance<D>> List<QueryResult<D>> reverseKNNQuery(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
        return super.reverseKNNQuery(id, k, distanceFunction);
    }
}
