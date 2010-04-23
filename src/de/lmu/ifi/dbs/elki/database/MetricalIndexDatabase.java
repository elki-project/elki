package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalIndex;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * MetricalIndexDatabase is a database implementation which is supported by a
 * metrical index structure.
 * 
 * @author Elke Achtert
 * @param <O> the type of FeatureVector as element of the database
 * @param <D> Distance type
 * @param <N> Node type
 * @param <E> Entry type
 */
@Description("Database using a metrical index")
public class MetricalIndexDatabase<O extends DatabaseObject, D extends Distance<D>, N extends MetricalNode<N, E>, E extends MTreeEntry<D>> extends IndexDatabase<O> implements Parameterizable {
  /**
   * OptionID for {@link #INDEX_PARAM}
   */
  public static final OptionID INDEX_ID = OptionID.getOrCreateOptionID("metricalindexdb.index", "Metrical index class to use.");

  /**
   * Parameter to specify the metrical index to use.
   * <p>
   * Key: {@code -metricalindexdb.index}
   * </p>
   */
  private final ObjectParameter<MetricalIndex<O, D, N, E>> INDEX_PARAM = new ObjectParameter<MetricalIndex<O, D, N, E>>(INDEX_ID, MetricalIndex.class);

  /**
   * The metrical index storing the data.
   */
  MetricalIndex<O, D, N, E> index;

  /**
   * Store own parameters, for partitioning.
   */
  private Collection<Pair<OptionID, Object>> params;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public MetricalIndexDatabase(Parameterization config) {
    super();
    TrackParameters track = new TrackParameters(config);
    if(track.grab(INDEX_PARAM)) {
      index = INDEX_PARAM.instantiateClass(track);
      index.setDatabase(this);
    }
    params = track.getGivenParameters();
  }

  /**
   * Retrieves the epsilon-neighborhood for the query object by performing a
   * range query on the underlying index.
   * 
   * @see MetricalIndex#rangeQuery(DatabaseObject, Distance)
   */
  @SuppressWarnings("unchecked")
  public <T extends Distance<T>> List<DistanceResultPair<T>> rangeQuery(Integer id, T epsilon, DistanceFunction<O, T> distanceFunction) {
    checkDistanceFunction(distanceFunction);

    List rangeQuery = index.rangeQuery(get(id), (D) epsilon);
    return rangeQuery;
  }

  /**
   * Retrieves the k-nearest neighbors (kNN) for the query object by performing
   * a kNN query on the underlying index.
   * 
   * @see MetricalIndex#kNNQuery(DatabaseObject, int)
   */
  @SuppressWarnings("unchecked")
  public <T extends Distance<T>> List<DistanceResultPair<T>> kNNQueryForObject(O queryObject, int k, DistanceFunction<O, T> distanceFunction) {
    checkDistanceFunction(distanceFunction);

    List knnQuery = index.kNNQuery(queryObject, k);
    return knnQuery;
  }

  /**
   * Not yet supported.
   */
  public <T extends Distance<T>> List<List<DistanceResultPair<T>>> bulkKNNQueryForID(@SuppressWarnings("unused") List<Integer> ids, @SuppressWarnings("unused") int k, @SuppressWarnings("unused") DistanceFunction<O, T> distanceFunction) {
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  /**
   * Retrieves the reverse k-nearest neighbors (RkNN) for the query object by
   * performing a RkNN query on the underlying index. If the index does not
   * support RkNN queries, a sequential scan is performed.
   * 
   * @see MetricalIndex#reverseKNNQuery(DatabaseObject, int)
   */
  @SuppressWarnings("unchecked")
  public <T extends Distance<T>> List<DistanceResultPair<T>> reverseKNNQueryForID(Integer id, int k, DistanceFunction<O, T> distanceFunction) {
    checkDistanceFunction(distanceFunction);
    try {
      List rknnQuery = index.reverseKNNQuery(get(id), k);
      return rknnQuery;
    }
    catch(UnsupportedOperationException e) {
      logger.warning("Reverse KNN queries are not supported by the underlying index structure. Perform a sequential scan.");
      List<Integer> ids = new ArrayList<Integer>();
      ids.add(id);
      return sequentialBulkReverseKNNQueryForID(ids, k, distanceFunction).get(0);
    }
  }

  /**
   * Not yet supported.
   */
  public <T extends Distance<T>> List<List<DistanceResultPair<T>>> bulkReverseKNNQueryForID(@SuppressWarnings("unused") List<Integer> ids, @SuppressWarnings("unused") int k, @SuppressWarnings("unused") DistanceFunction<O, T> distanceFunction) {
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  /**
   * Returns the index of this database.
   * 
   * @return the index of this database
   */
  @Override
  public MetricalIndex<O, D, N, E> getIndex() {
    return index;
  }

  @Override
  protected Collection<Pair<OptionID, Object>> getParameters() {
    return new java.util.Vector<Pair<OptionID, Object>>(this.params);
  }

  /**
   * Throws an IllegalArgumentException if the specified distance function is
   * not an instance of the distance function used by the underlying index of
   * this database.
   * 
   * @throws IllegalArgumentException
   * @param <T> distance type
   * @param distanceFunction the distance function to be checked
   */
  private <T extends Distance<T>> void checkDistanceFunction(DistanceFunction<O, T> distanceFunction) {
    // todo: the same class does not necessarily indicate the same
    // distancefunction!!! (e.g.dim selecting df!)
    if(!distanceFunction.getClass().equals(index.getDistanceFunction().getClass())) {
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " + index.getDistanceFunction().getClass() + ", but is " + distanceFunction.getClass());
    }
  }
}