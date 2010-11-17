package de.lmu.ifi.dbs.elki.database.query.range;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Abstract base class for range queries that use a distance query in their
 * instance
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public abstract class AbstractDistanceRangeQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractRangeQuery<O, D> {
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
  public AbstractDistanceRangeQuery(Parameterization config) {
    super(config);
    config = config.descend(this);
    // distance function parameter
  }

  @Override
  abstract public <T extends O> RangeQuery.Instance<T, D> instantiate(Database<T> database);

  @Override
  public D getDistanceFactory() {
    return distanceFunction.getDistanceFactory();
  }

  /**
   * Instance for the query on a particular database.
   * 
   * @author Erich Schubert
   */
  public abstract static class Instance<O extends DatabaseObject, D extends Distance<D>> extends AbstractRangeQuery.Instance<O, D> {
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
    abstract public List<DistanceResultPair<D>> getRangeForDBID(DBID id, D range);

    @Override
    abstract public List<DistanceResultPair<D>> getRangeForObject(O obj, D range);

    @Override
    public D getDistanceFactory() {
      return distanceQuery.getDistanceFactory();
    }
  }
}