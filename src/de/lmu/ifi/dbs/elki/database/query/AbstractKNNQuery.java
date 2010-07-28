package de.lmu.ifi.dbs.elki.database.query;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for KNN queries
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public abstract class AbstractKNNQuery<O extends DatabaseObject, D extends Distance<D>> implements KNNQuery<O, D> {
  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * materialized. must be an integer greater than 1.
   * <p>
   * Key: {@code -materialize.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(1));

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  protected int k = 2;

  /**
   * Parameter to indicate the distance function to be used to ascertain the
   * nearest neighbors.
   * <p/>
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code materialize.distance}
   * </p>
   */
  public final ObjectParameter<DistanceFunction<? super O, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<? super O, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);

  /**
   * Hold the distance function to be used.
   */
  protected DistanceFunction<? super O, D> distanceFunction;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public AbstractKNNQuery(Parameterization config) {
    super();
    // number of neighbors
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }

    // distance function
    if(config.grab(DISTANCE_FUNCTION_PARAM)) {
      distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
  }

  @Override
  public DistanceFunction<? super O, D> getDistanceFunction() {
    return distanceFunction;
  }

  @Override
  public D getDistanceFactory() {
    return distanceFunction.getDistanceFactory();
  }

  @Override
  abstract public <T extends O> Instance<T, D> instantiate(Database<T> database);

  @Override
  abstract public <T extends O> Instance<T, D> instantiate(Database<T> database, DistanceQuery<T, D> distanceQuery);

  /**
   * Instance for the query on a particular database.
   * 
   * @author Erich Schubert
   */
  public abstract static class Instance<O extends DatabaseObject, D extends Distance<D>> implements KNNQuery.Instance<O, D> {
    /**
     * Hold the distance function to be used.
     */
    protected DistanceQuery<O, D> distanceQuery;

    /**
     * The database we operate on.
     */
    protected Database<O> database;

    /**
     * Constructor.
     * 
     * @param database Database
     */
    public Instance(Database<O> database, DistanceQuery<O, D> distanceQuery) {
      super();
      this.database = database;
      this.distanceQuery = distanceQuery;
    }

    @Override
    abstract public List<DistanceResultPair<D>> get(DBID id);

    @Override
    public DistanceQuery<O, D> getDistanceQuery() {
      return distanceQuery;
    }
  }
}