package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for KNN queries that use a distance query in their
 * instance
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public abstract class AbstractDistanceKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractKNNQuery<O, D> {
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
  public AbstractDistanceKNNQuery(Parameterization config) {
    super(config);
    config = config.descend(this);
    // distance function
    if(config.grab(DISTANCE_FUNCTION_PARAM)) {
      distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
  }

  @Override
  abstract public <T extends O> KNNQuery.Instance<T, D> instantiate(Database<T> database);

  @SuppressWarnings("deprecation")
  @Override
  public DistanceFunction<? super O, D> getDistanceFunction() {
    return distanceFunction;
  }

  @Override
  public D getDistanceFactory() {
    return distanceFunction.getDistanceFactory();
  }

  /**
   * Instance for the query on a particular database.
   * 
   * @author Erich Schubert
   */
  public abstract static class Instance<O extends DatabaseObject, D extends Distance<D>> extends AbstractKNNQuery.Instance<O, D> {
    /**
     * Hold the distance function to be used.
     */
    protected DistanceQuery<O, D> distanceQuery;

    /**
     * Constructor.
     * 
     * @param database Database
     */
    public Instance(Database<O> database, DistanceQuery<O, D> distanceQuery) {
      super(database);
      this.distanceQuery = distanceQuery;
    }

    @Override
    abstract public List<DistanceResultPair<D>> getKNNForDBID(DBID id, int k);

    @Override
    abstract public List<DistanceResultPair<D>> getKNNForObject(O obj, int k);

    @Override
    public DistanceQuery<O, D> getDistanceQuery() {
      return distanceQuery;
    }

    @Override
    public D getDistanceFactory() {
      return distanceQuery.getDistanceFactory();
    }
  }
}