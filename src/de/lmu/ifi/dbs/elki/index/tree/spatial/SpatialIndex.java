package de.lmu.ifi.dbs.elki.index.tree.spatial;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndex;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.EqualStringConstraint;

/**
 * Abstract super class for all spatial index classes.
 * 
 * @author Elke Achtert
 */
public abstract class SpatialIndex<O extends NumberVector<O, ?>, N extends SpatialNode<N, E>, E extends SpatialEntry> extends TreeIndex<O, N, E> {
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
  private final PatternParameter BULK_LOAD_STRATEGY_PARAM = new PatternParameter(BULK_LOAD_STRATEGY_ID, new EqualStringConstraint(new String[] { BulkSplit.Strategy.MAX_EXTENSION.toString(), BulkSplit.Strategy.ZCURVE.toString() }), BulkSplit.Strategy.ZCURVE.toString());

  /**
   * If true, a bulk load will be performed.
   */
  protected boolean bulk;

  /**
   * The strategy for bulk load.
   */
  protected BulkSplit.Strategy bulkLoadStrategy;

  public SpatialIndex() {
    super();
    addOption(BULK_LOAD_FLAG);
    addOption(BULK_LOAD_STRATEGY_PARAM);
  }

  /**
   * todo
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    bulk = BULK_LOAD_FLAG.isSet();

    if(bulk) {
      String strategy = BULK_LOAD_STRATEGY_PARAM.getValue();

      if(strategy.equals(BulkSplit.Strategy.MAX_EXTENSION.toString())) {
        bulkLoadStrategy = BulkSplit.Strategy.MAX_EXTENSION;
      }
      else if(strategy.equals(BulkSplit.Strategy.ZCURVE.toString())) {
        bulkLoadStrategy = BulkSplit.Strategy.ZCURVE;
      }
      else
        throw new WrongParameterValueException(BULK_LOAD_STRATEGY_PARAM, strategy, null);
    }

    return remainingParameters;
  }

  /**
   * Sets the database in the distance function of this index (if existing).
   * Subclasses may need to overwrite this method.
   * 
   * @param database the database
   */
  public void setDatabase(Database<O> database) {
    // do nothing
  }

  /**
   * Performs a range query for the given object with the given epsilon range
   * and the according distance function. The query result is in ascending order
   * to the distance to the query object.
   * 
   * @param obj the query object
   * @param epsilon the string representation of the query range
   * @param distanceFunction the distance function that computes the distances
   *        between the objects
   * @return a List of the query results
   */
  public abstract <D extends Distance<D>> List<QueryResult<D>> rangeQuery(final O obj, final String epsilon, final SpatialDistanceFunction<O, D> distanceFunction);

  /**
   * Performs a k-nearest neighbor query for the given object with the given
   * parameter k and the according distance function. The query result is in
   * ascending order to the distance to the query object.
   * 
   * @param obj the query object
   * @param k the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances
   *        between the objects
   * @return a List of the query results
   */
  public abstract <D extends Distance<D>> List<QueryResult<D>> kNNQuery(final O obj, final int k, final SpatialDistanceFunction<O, D> distanceFunction);

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   * 
   * @param object the query object
   * @param k the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances
   *        between the objects
   * @return a List of the query results
   */
  public abstract <D extends Distance<D>> List<QueryResult<D>> reverseKNNQuery(final O object, final int k, final SpatialDistanceFunction<O, D> distanceFunction);

  /**
   * Performs a bulk k-nearest neighbor query for the given object IDs. The
   * query result is in ascending order to the distance to the query objects.
   * 
   * @param ids the query objects
   * @param k the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances
   *        between the objects
   * @return a List of the query results
   */
  public abstract <D extends Distance<D>> List<List<QueryResult<D>>> bulkKNNQueryForIDs(List<Integer> ids, final int k, final SpatialDistanceFunction<O, D> distanceFunction);

  /**
   * Returns a list of entries pointing to the leaf nodes of this spatial index.
   * 
   * @return a list of entries pointing to the leaf nodes of this spatial index
   */
  public abstract List<E> getLeaves();
}
