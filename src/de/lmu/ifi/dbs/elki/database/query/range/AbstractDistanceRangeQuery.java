package de.lmu.ifi.dbs.elki.database.query.range;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.AbstractDatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Abstract base class for range queries that use a distance query in their
 * instance
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public abstract class AbstractDistanceRangeQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractDatabaseQuery<O> implements RangeQuery<O, D> {
  /**
   * Hold the distance function to be used.
   */
  protected DistanceQuery<O, D> distanceQuery;

  /**
   * Constructor.
   * 
   * @param database Database
   */
  public AbstractDistanceRangeQuery(Database<? extends O> database, DistanceQuery<O, D> distanceQuery) {
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