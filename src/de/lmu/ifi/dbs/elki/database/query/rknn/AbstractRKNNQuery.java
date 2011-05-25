package de.lmu.ifi.dbs.elki.database.query.rknn;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.AbstractDataBasedQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Instance for the query on a particular database.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractRKNNQuery<O, D extends Distance<D>> extends AbstractDataBasedQuery<O> implements RKNNQuery<O, D> {
  /**
   * Hold the distance function to be used.
   */
  protected final DistanceQuery<O, D> distanceQuery;

  /**
   * Constructor.
   * 
   * @param distanceQuery distance query
   */
  public AbstractRKNNQuery(DistanceQuery<O, D> distanceQuery) {
    super(distanceQuery.getRelation());
    this.distanceQuery = distanceQuery;
  }

  @Override
  abstract public List<DistanceResultPair<D>> getRKNNForDBID(DBID id, int k);

  @Override
  public D getDistanceFactory() {
    return distanceQuery.getDistanceFactory();
  }
}