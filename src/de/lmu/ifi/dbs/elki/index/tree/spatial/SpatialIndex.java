package de.lmu.ifi.dbs.elki.index.tree.spatial;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndex;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.EqualStringConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Abstract super class for all spatial index classes.
 * 
 * @author Elke Achtert
 * @param <O> Vector type
 * @param <N> Node type
 * @param <E> Entry type
 */
public abstract class SpatialIndex<O extends NumberVector<?, ?>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends TreeIndex<O, N, E> {
  /**
   * OptionID for {@link #BULK_LOAD_FLAG}
   */
  public static final OptionID BULK_LOAD_ID = OptionID.getOrCreateOptionID("spatial.bulk", "flag to specify bulk load (default is no bulk load)");

  /**
   * Parameter for bulk loading
   */
  private final Flag BULK_LOAD_FLAG = new Flag(BULK_LOAD_ID);

  /**
   * OptionID for {@link #BULK_LOAD_STRATEGY_PARAM}
   */
  public static final OptionID BULK_LOAD_STRATEGY_ID = OptionID.getOrCreateOptionID("spatial.bulkstrategy", "the strategy for bulk load, available strategies are: [" + BulkSplit.Strategy.MAX_EXTENSION + "| " + BulkSplit.Strategy.ZCURVE + "]" + "(default is " + BulkSplit.Strategy.ZCURVE + ")");

  /**
   * Parameter for bulk strategy
   */
  private final StringParameter BULK_LOAD_STRATEGY_PARAM = new StringParameter(BULK_LOAD_STRATEGY_ID, new EqualStringConstraint(new String[] { BulkSplit.Strategy.MAX_EXTENSION.toString(), BulkSplit.Strategy.ZCURVE.toString() }), BulkSplit.Strategy.ZCURVE.toString());

  /**
   * If true, a bulk load will be performed.
   */
  protected boolean bulk;

  /**
   * The strategy for bulk load.
   */
  protected BulkSplit.Strategy bulkLoadStrategy;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public SpatialIndex(Parameterization config) {
    super(config);
    config = config.descend(this);
    if(config.grab(BULK_LOAD_FLAG)) {
      bulk = BULK_LOAD_FLAG.getValue();
    }
    config.grab(BULK_LOAD_STRATEGY_PARAM);
    if(bulk) {
      String strategy = BULK_LOAD_STRATEGY_PARAM.getValue();

      if(strategy.equals(BulkSplit.Strategy.MAX_EXTENSION.toString())) {
        bulkLoadStrategy = BulkSplit.Strategy.MAX_EXTENSION;
      }
      else if(strategy.equals(BulkSplit.Strategy.ZCURVE.toString())) {
        bulkLoadStrategy = BulkSplit.Strategy.ZCURVE;
      }
      else {
        config.reportError(new WrongParameterValueException(BULK_LOAD_STRATEGY_PARAM, strategy));
      }
    }
    // TODO: specify constraint?
  }

  /**
   * Sets the database in the distance function of this index (if existing).
   * Subclasses may need to overwrite this method.
   * 
   * @param database the database
   */
  @Override
  public void setDatabase(Database<O> database) {
    // do nothing
  }

  /**
   * Performs a range query for the given object with the given epsilon range
   * and the according distance function. The query result is in ascending order
   * to the distance to the query object.
   * 
   * @param <D> distance type
   * @param obj the query object
   * @param epsilon the string representation of the query range
   * @param distanceFunction the distance function that computes the distances
   *        between the objects
   * @return a List of the query results
   */
  public abstract <D extends Distance<D>> List<DistanceResultPair<D>> rangeQuery(final O obj, final D epsilon, final SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction);

  /**
   * Performs a k-nearest neighbor query for the given object with the given
   * parameter k and the according distance function. The query result is in
   * ascending order to the distance to the query object.
   * 
   * @param <D> distance type
   * @param obj the query object
   * @param k the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances
   *        between the objects
   * @return a List of the query results
   */
  public abstract <D extends Distance<D>> List<DistanceResultPair<D>> kNNQuery(final O obj, final int k, final SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction);

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   * 
   * @param <D> distance type
   * @param object the query object
   * @param k the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances
   *        between the objects
   * @return a List of the query results
   */
  public abstract <D extends Distance<D>> List<DistanceResultPair<D>> reverseKNNQuery(final O object, final int k, final SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction);

  /**
   * Performs a bulk k-nearest neighbor query for the given object IDs. Each
   * query result is in ascending order to the distance to the query objects.
   * 
   * @param <D> distance type
   * @param ids the query objects
   * @param k the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances
   *        between the objects
   * @return a List of List the query results
   */
  public abstract <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkKNNQueryForIDs(DBIDs ids, final int k, final SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction);

  /**
   * Performs a bulk reverse k-nearest neighbor queries for the given object
   * IDs. Each query result is sorted in ascending order w.r.t. the distance to
   * the query object.
   * 
   * @param <D> distance type
   * @param ids the IDs of the query objects
   * @param k the size of k-nearest neighborhood of any object <code>o</code> to
   *        contain an object in order to include <code>o</code> in the result
   *        list
   * @param distanceFunction the distance function that computes the distances
   *        between the objects
   * @return a List of List of the query results
   */
  public abstract <D extends Distance<D>> List<List<DistanceResultPair<D>>> bulkReverseKNNQueryForID(DBIDs ids, int k, SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction);

  /**
   * Returns a list of entries pointing to the leaf nodes of this spatial index.
   * 
   * @return a list of entries pointing to the leaf nodes of this spatial index
   */
  public abstract List<E> getLeaves();

  /**
   * Get a KNNQuery object using the index.
   * 
   * @param <D> Distance type
   * @param k Maximum value of k
   * @param distanceQuery Distance query to use.
   * @return KNN query object, if supported.
   */
  @SuppressWarnings("unchecked")
  public <D extends Distance<D>> SpatialIndexKNNQuery<O, D> getKNNQuery(final int k, DistanceQuery<O, D> distanceQuery) {
    // FIXME: Check that this distance function is supported by the index!
    SpatialPrimitiveDistanceFunction<O, D> sdf = (SpatialPrimitiveDistanceFunction<O, D>) distanceQuery.getDistanceFunction();
    return new SpatialIndexKNNQuery<O, D>(distanceQuery, sdf, k);
  }

  /**
   * Instance of a KNN query for a particular spatial index.
   * 
   * @author Erich Schubert
   */
  public static class SpatialIndexKNNQuery<O extends NumberVector<?, ?>, D extends Distance<D>> implements KNNQuery.Instance<O, D> {
    /**
     * The index to use
     */
    SpatialIndex<O, ?, ?> index;

    /**
     * Spatial primitive distance function
     */
    SpatialPrimitiveDistanceFunction<? super O, D> df;

    /**
     * The query k
     */
    final int k;

    /**
     * Distance query
     */
    private DistanceQuery<O, D> distanceQuery;

    /**
     * Constructor.
     * 
     * @param distanceQuery Distance function to use
     * @param df Distance function
     * @param k maximum k value
     */
    public SpatialIndexKNNQuery(DistanceQuery<O, D> distanceQuery, SpatialPrimitiveDistanceFunction<? super O, D> df, int k) {
      this.distanceQuery = distanceQuery;
      this.df = df;
      this.k = k;
    }

    @Override
    public List<DistanceResultPair<D>> get(DBID id) {
      List<List<DistanceResultPair<D>>> res = index.bulkKNNQueryForIDs(id, k, df);
      return res.get(0);
    }

    @Override
    public DistanceQuery<O, D> getDistanceQuery() {
      return distanceQuery;
    }
  }
}