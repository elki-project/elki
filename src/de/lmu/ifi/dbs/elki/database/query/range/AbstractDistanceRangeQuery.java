package de.lmu.ifi.dbs.elki.database.query.range;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.AbstractDataBasedQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
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
public abstract class AbstractDistanceRangeQuery<O, D extends Distance<D>> extends AbstractDataBasedQuery<O> implements RangeQuery<O, D> {
  /**
   * Hold the distance function to be used.
   */
  protected DistanceQuery<O, D> distanceQuery;

  /**
   * Constructor.
   * 
   * @param relation Relation
   * @param distanceQuery Distance query
   */
  public AbstractDistanceRangeQuery(Relation<? extends O> relation, DistanceQuery<O, D> distanceQuery) {
    super(relation);
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