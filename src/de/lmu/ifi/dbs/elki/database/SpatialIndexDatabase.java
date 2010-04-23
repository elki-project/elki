package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialNode;
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
public class SpatialIndexDatabase<O extends NumberVector<O, ?>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends IndexDatabase<O> implements Parameterizable {
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
    TrackParameters track = new TrackParameters(config);
    if(track.grab(INDEX_PARAM)) {
      index = INDEX_PARAM.instantiateClass(track);
      index.setDatabase(this);
    }
    params = track.getGivenParameters();
  }

  /**
   * Retrieves the epsilon-neighborhood for the query object. If the specified
   * distance function is an instance of a {@link SpatialDistanceFunction} the
   * range query is delegated to the underlying index. Otherwise a sequential
   * scan is performed to retrieve the epsilon-neighborhood,
   * 
   * @see SpatialIndex#rangeQuery(NumberVector, Distance,
   *      SpatialDistanceFunction)
   */
  public <D extends Distance<D>> List<DistanceResultPair<D>> rangeQuery(Integer id, D epsilon, DistanceFunction<O, D> distanceFunction) {
    if(epsilon.isInfiniteDistance()) {
      final List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
      for(Iterator<Integer> it = iterator(); it.hasNext();) {
        Integer next = it.next();
        result.add(new DistanceResultPair<D>(distanceFunction.distance(id, next), next));
      }
      Collections.sort(result);
      return result;
    }

    if(!(distanceFunction instanceof SpatialDistanceFunction<?, ?>)) {
      // TODO: why is this emulated here, but not for other queries.
      List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
      for(Iterator<Integer> it = iterator(); it.hasNext();) {
        Integer next = it.next();
        D currentDistance = distanceFunction.distance(id, next);
        if(currentDistance.compareTo(epsilon) <= 0) {
          result.add(new DistanceResultPair<D>(currentDistance, next));
        }
      }
      Collections.sort(result);
      return result;
    }
    else {
      return index.rangeQuery(get(id), epsilon, (SpatialDistanceFunction<O, D>) distanceFunction);
    }
  }

  /**
   * Retrieves the k-nearest neighbors (kNN) for the query object by performing
   * a kNN query on the underlying index.
   * 
   * @see SpatialIndex#kNNQuery(NumberVector, int, SpatialDistanceFunction)
   */
  public <D extends Distance<D>> List<DistanceResultPair<D>> kNNQueryForObject(O queryObject, int k, DistanceFunction<O, D> distanceFunction) {
    checkDistanceFunction(distanceFunction);
    return index.kNNQuery(queryObject, k, (SpatialDistanceFunction<O, D>) distanceFunction);
  }

  /**
   * Retrieves the k-nearest neighbors (kNN) for the query objects by performing
   * a bulk kNN query on the underlying index.
   * 
   * @see SpatialIndex#bulkKNNQueryForIDs(List, int, SpatialDistanceFunction)
   */
  public <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkKNNQueryForID(List<Integer> ids, int k, DistanceFunction<O, D> distanceFunction) {
    checkDistanceFunction(distanceFunction);
    return index.bulkKNNQueryForIDs(ids, k, (SpatialDistanceFunction<O, D>) distanceFunction);
  }

  /**
   * Retrieves the reverse k-nearest neighbors (RkNN) for the query object by
   * performing a RkNN query on the underlying index. If the index does not
   * support RkNN queries, a sequential scan is performed.
   * 
   * @see SpatialIndex#reverseKNNQuery(DatabaseObject, int)
   */
  public <D extends Distance<D>> List<DistanceResultPair<D>> reverseKNNQueryForID(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
    checkDistanceFunction(distanceFunction);
    try {
      return index.reverseKNNQuery(get(id), k, (SpatialDistanceFunction<O, D>) distanceFunction);
    }
    catch(UnsupportedOperationException e) {
      logger.warning("Reverse KNN queries are not supported by the underlying index structure. Perform a sequential scan.");
      List<Integer> ids = new ArrayList<Integer>();
      ids.add(id);
      return sequentialBulkReverseKNNQueryForID(ids, k, distanceFunction).get(0);
    }
  }

  /**
   * Retrieves the reverse k-nearest neighbors (RkNN) for the query objects by
   * performing a bulk RkNN query on the underlying index. If the index does not
   * support bulk RkNN queries, a sequential scan is performed.
   * 
   * @see SpatialIndex#bulkReverseKNNQueryForID(List, int,
   *      SpatialDistanceFunction)
   */
  public <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkReverseKNNQueryForID(List<Integer> ids, int k, DistanceFunction<O, D> distanceFunction) {
    checkDistanceFunction(distanceFunction);
    try {
      return index.bulkReverseKNNQueryForID(ids, k, (SpatialDistanceFunction<O, D>) distanceFunction);
    }
    catch(UnsupportedOperationException e) {
      logger.warning("Bulk Reverse KNN queries are not supported by the underlying index structure. Perform single rnn queries.");
      try {
        List<List<DistanceResultPair<D>>> rNNList = new ArrayList<List<DistanceResultPair<D>>>(ids.size());
        for(Integer id : ids) {
          rNNList.add(index.reverseKNNQuery(get(id), k, (SpatialDistanceFunction<O, D>) distanceFunction));
        }
        return rNNList;
      }
      catch(UnsupportedOperationException ee) {
        logger.warning("Bulk Reverse KNN queries are not supported by the underlying index structure. Perform a sequential scan.");
        return sequentialBulkReverseKNNQueryForID(ids, k, distanceFunction);
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
  @Override
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
   * @param distanceFunction the distance function to be checked
   */
  private <T extends Distance<T>> void checkDistanceFunction(DistanceFunction<O, T> distanceFunction) {
    if(!(distanceFunction instanceof SpatialDistanceFunction<?, ?>)) {
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");
    }
  }
}