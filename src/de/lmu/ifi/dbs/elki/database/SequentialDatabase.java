package de.lmu.ifi.dbs.elki.database;

import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
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
  @Override
  public <D extends Distance<D>> List<DistanceResultPair<D>> kNNQueryForID(DBID id, int k, DistanceQuery<O, D> distanceFunction) {
    return sequentialkNNQueryForID(id, k, distanceFunction);
  }

  /**
   * Retrieves the k-nearest neighbors (kNN) for the query object performing a
   * sequential scan on this database. The kNN are determined by trying to add
   * each object to a {@link KNNHeap}.
   */
  @Override
  public <D extends Distance<D>> List<DistanceResultPair<D>> kNNQueryForObject(O queryObject, int k, DistanceQuery<O, D> distanceFunction) {
    return sequentialkNNQueryForObject(queryObject, k, distanceFunction);
  }

  /**
   * Retrieves the k-nearest neighbors (kNN) for the query objects performing
   * one sequential scan on this database. For each query id a {@link KNNHeap}
   * is assigned. The kNNs are determined by trying to add each object to all
   * KNNHeap.
   */
  @Override
  public <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkKNNQueryForID(ArrayDBIDs ids, int k, DistanceQuery<O, D> distanceFunction) {
    return sequentialBulkKNNQueryForID(ids, k, distanceFunction);
  }

  /**
   * Retrieves the epsilon-neighborhood of the query object performing a
   * sequential scan on this database.
   */
  @Override
  public <D extends Distance<D>> List<DistanceResultPair<D>> rangeQuery(DBID id, D epsilon, DistanceQuery<O, D> distanceFunction) {
    return sequentialRangeQuery(id, epsilon, distanceFunction);
  }

  /**
   * Retrieves the epsilon-neighborhood of the query object performing a
   * sequential scan on this database.
   */
  @Override
  public <D extends Distance<D>> List<DistanceResultPair<D>> rangeQueryForObject(O obj, D epsilon, DistanceQuery<O, D> distanceFunction) {
    return sequentialRangeQueryForObject(obj, epsilon, distanceFunction);
  }

  /**
   * Retrieves the reverse k-nearest neighbors (RkNN) for the query object by
   * performing a bulk knn query for all objects. If the query object is an
   * element of the kNN of an object o, o belongs to the query result.
   */
  @Override
  public <D extends Distance<D>> List<DistanceResultPair<D>> reverseKNNQueryForID(DBID id, int k, DistanceQuery<O, D> distanceFunction) {
    return sequentialBulkReverseKNNQueryForID(id, k, distanceFunction).get(0);
  }

  /**
   * Retrieves the reverse k-nearest neighbors (RkNN) for the query object by
   * performing a bulk knn query for all objects. If a query object is an
   * element of the kNN of an object o, o belongs to the particular query
   * result.
   */
  @Override
  public <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkReverseKNNQueryForID(ArrayDBIDs ids, int k, DistanceQuery<O, D> distanceFunction) {
    return sequentialBulkReverseKNNQueryForID(ids, k, distanceFunction);
  }

  @Override
  protected Collection<Pair<OptionID, Object>> getParameters() {
    return new java.util.Vector<Pair<OptionID, Object>>();
  }
}
