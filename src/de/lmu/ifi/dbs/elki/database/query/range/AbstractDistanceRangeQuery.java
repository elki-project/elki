package de.lmu.ifi.dbs.elki.database.query.range;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.AbstractDataBasedQuery;
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
public abstract class AbstractDistanceRangeQuery<O, D extends Distance<D>> extends AbstractDataBasedQuery<O> implements RangeQuery<O, D> {
  /**
   * Hold the distance function to be used.
   */
  protected DistanceQuery<O, D> distanceQuery;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance query
   */
  public AbstractDistanceRangeQuery(DistanceQuery<O, D> distanceQuery) {
    super(distanceQuery.getRelation());
    this.distanceQuery = distanceQuery;
  }

  @Override
  abstract public List<DistanceResultPair<D>> getRangeForDBID(DBID id, D range);

  @Override
  abstract public List<DistanceResultPair<D>> getRangeForObject(O obj, D range);

  @Override
  public List<List<DistanceResultPair<D>>> getRangeForBulkDBIDs(ArrayDBIDs ids, D range) {
    final List<List<DistanceResultPair<D>>> lists = new ArrayList<List<DistanceResultPair<D>>>(ids.size());
    for(DBID id : ids) {
      lists.add(getRangeForDBID(id, range));
    }
    return lists;
  }

  @Override
  public D getDistanceFactory() {
    return distanceQuery.getDistanceFactory();
  }
}