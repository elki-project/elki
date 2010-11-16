package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * SpatialIndexDatabase is a database implementation which is supported by a
 * spatial index structure.
 * 
 * @author Elke Achtert
 * @param <O> the type of FeatureVector as element of the database
 * @param <N> the type of SpatialNode stored in the index
 * @param <E> the type of SpatialEntry stored in the index
 */
@Description("Database using a spatial index")
public class SpatialIndexDatabase<O extends NumberVector<?, ?>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends AbstractDatabase<O> implements Parameterizable {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SpatialIndexDatabase.class);
  
  /**
   * OptionID for {@link #INDEX_PARAM}
   */
  public static final OptionID INDEX_ID = OptionID.getOrCreateOptionID("spatialindexdb.index", "Spatial index class to use.");

  /**
   * Parameter to specify the spatial index to use.
   * <p>
   * Key: {@code -spatialindexdb.index}
   * </p>
   */
  private final ObjectParameter<SpatialIndex<O, N, E>> INDEX_PARAM = new ObjectParameter<SpatialIndex<O, N, E>>(INDEX_ID, SpatialIndex.class);

  /**
   * The index structure storing the data.
   */
  protected SpatialIndex<O, N, E> index;

  /**
   * Store own parameters, needed for partitioning.
   */
  private Collection<Pair<OptionID, Object>> params;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public SpatialIndexDatabase(Parameterization config) {
    super();
    config = config.descend(this);
    TrackParameters track = new TrackParameters(config);
    if(track.grab(INDEX_PARAM)) {
      index = INDEX_PARAM.instantiateClass(track);
      index.setDatabase(this);
      addIndex(index);
    }
    params = track.getGivenParameters();
  }

  /**
   * Retrieves the epsilon-neighborhood for the query object. If the specified
   * distance function is an instance of a {@link SpatialPrimitiveDistanceFunction} the
   * range query is delegated to the underlying index. Otherwise a sequential
   * scan is performed to retrieve the epsilon-neighborhood,
   * 
   * @see SpatialIndex#rangeQuery
   */
  @Override
  public <D extends Distance<D>> List<DistanceResultPair<D>> rangeQuery(DBID id, D epsilon, DistanceQuery<O, D> distanceQuery) {
    if(epsilon.isInfiniteDistance()) {
      final List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
      for(Iterator<DBID> it = iterator(); it.hasNext();) {
        DBID next = it.next();
        result.add(new DistanceResultPair<D>(distanceQuery.distance(id, next), next));
      }
      Collections.sort(result);
      return result;
    }

    SpatialPrimitiveDistanceFunction<O, D> distanceFunction = checkDistanceFunction(distanceQuery);
    if(distanceFunction == null) {
      return sequentialRangeQuery(id, epsilon, distanceQuery);
    }
    return index.rangeQuery(get(id), epsilon, distanceFunction);
  }

  /**
   * Retrieves the epsilon-neighborhood for the query object. If the specified
   * distance function is an instance of a {@link SpatialPrimitiveDistanceFunction} the
   * range query is delegated to the underlying index. Otherwise a sequential
   * scan is performed to retrieve the epsilon-neighborhood,
   * 
   * @see SpatialIndex#rangeQuery
   */
  @Override
  public <D extends Distance<D>> List<DistanceResultPair<D>> rangeQueryForObject(O obj, D epsilon, DistanceQuery<O, D> distanceQuery) {
    if(epsilon.isInfiniteDistance()) {
      final List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
      for(Iterator<DBID> it = iterator(); it.hasNext();) {
        DBID next = it.next();
        result.add(new DistanceResultPair<D>(distanceQuery.distance(next, obj), next));
      }
      Collections.sort(result);
      return result;
    }

    SpatialPrimitiveDistanceFunction<O, D> distanceFunction = checkDistanceFunction(distanceQuery);
    if(distanceFunction == null) {
      return sequentialRangeQueryForObject(obj, epsilon, distanceQuery);
    }
    return index.rangeQuery(obj, epsilon, distanceFunction);
  }

  /**
   * Retrieves the k-nearest neighbors (kNN) for the query objects by performing
   * a bulk kNN query on the underlying index.
   * 
   * @see SpatialIndex#bulkKNNQueryForIDs
   */
  @Override
  public <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkKNNQueryForID(ArrayDBIDs ids, int k, DistanceQuery<O, D> distanceQuery) {
    SpatialPrimitiveDistanceFunction<O, D> distanceFunction = checkDistanceFunction(distanceQuery);
    if(distanceFunction == null) {
      return sequentialBulkKNNQueryForID(ids, k, distanceQuery);
    }
    return index.bulkKNNQueryForIDs(ids, k, distanceFunction);
  }

  /**
   * Retrieves the reverse k-nearest neighbors (RkNN) for the query object by
   * performing a RkNN query on the underlying index. If the index does not
   * support RkNN queries, a sequential scan is performed.
   * 
   * @see SpatialIndex#reverseKNNQuery
   */
  @Override
  public <D extends Distance<D>> List<DistanceResultPair<D>> reverseKNNQueryForID(DBID id, int k, DistanceQuery<O, D> distanceQuery) {
    SpatialPrimitiveDistanceFunction<O, D> distanceFunction = checkDistanceFunction(distanceQuery);
    if(distanceFunction == null) {
      return sequentialBulkReverseKNNQueryForID(id, k, distanceQuery).get(0);
    }
    try {
      return index.reverseKNNQuery(get(id), k, distanceFunction);
    }
    catch(UnsupportedOperationException e) {
      logger.warning("Reverse KNN queries are not supported by the underlying index structure. Perform a sequential scan.");
      return sequentialBulkReverseKNNQueryForID(id, k, distanceQuery).get(0);
    }
  }

  /**
   * Retrieves the reverse k-nearest neighbors (RkNN) for the query objects by
   * performing a bulk RkNN query on the underlying index. If the index does not
   * support bulk RkNN queries, a sequential scan is performed.
   * 
   * @see SpatialIndex#bulkReverseKNNQueryForID
   */
  @Override
  public <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkReverseKNNQueryForID(ArrayDBIDs ids, int k, DistanceQuery<O, D> distanceQuery) {
    SpatialPrimitiveDistanceFunction<O, D> distanceFunction = checkDistanceFunction(distanceQuery);
    if(distanceFunction == null) {
      return sequentialBulkReverseKNNQueryForID(ids, k, distanceQuery);
    }
    try {
      return index.bulkReverseKNNQueryForID(ids, k, distanceFunction);
    }
    catch(UnsupportedOperationException e) {
      logger.warning("Bulk Reverse KNN queries are not supported by the underlying index structure. Perform single rnn queries.");
      try {
        List<List<DistanceResultPair<D>>> rNNList = new ArrayList<List<DistanceResultPair<D>>>(ids.size());
        for(DBID id : ids) {
          rNNList.add(index.reverseKNNQuery(get(id), k, distanceFunction));
        }
        return rNNList;
      }
      catch(UnsupportedOperationException ee) {
        logger.warning("Bulk Reverse KNN queries are not supported by the underlying index structure. Perform a sequential scan.");
        return sequentialBulkReverseKNNQueryForID(ids, k, distanceQuery);
      }
    }
  }

  /**
   * Returns a list of the leaf nodes of the underlying spatial index of this
   * database.
   * 
   * @return a list of the leaf nodes of the underlying spatial index of this
   *         database
   */
  public List<E> getLeaves() {
    return index.getLeaves();
  }

  /**
   * Returns the id of the root of the underlying index.
   * 
   * @return the id of the root of the underlying index
   */
  public E getRootEntry() {
    return index.getRootEntry();
  }

  /**
   * Returns the index of this database.
   * 
   * @return the index of this database
   */
  @Deprecated
  public SpatialIndex<O, N, E> getIndex() {
    return index;
  }

  @Override
  protected Collection<Pair<OptionID, Object>> getParameters() {
    return new java.util.Vector<Pair<OptionID, Object>>(this.params);
  }

  /**
   * Throws an IllegalArgumentException if the specified distance function is
   * not a SpatialDistanceFunction.
   * 
   * @throws IllegalArgumentException
   * @param <T> distance type
   * @param distanceQuery the distance query to be checked
   */
  @SuppressWarnings("unchecked")
  private <T extends Distance<T>> SpatialPrimitiveDistanceFunction<O, T> checkDistanceFunction(DistanceQuery<O, T> distanceQuery) {
    DistanceFunction<? super O, T> distanceFunction = distanceQuery.getDistanceFunction();
    if(distanceFunction instanceof SpatialPrimitiveDistanceFunction<?, ?>) {
      return (SpatialPrimitiveDistanceFunction<O, T>) distanceFunction;
    }
    else {
      logger.warning("Querying the database with an unsupported distance function, fallback to sequential scan. Got: "+distanceQuery.getClass());
      return null;
      // throw new
      // IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");
    }
  }
}