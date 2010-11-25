package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.AbstractDatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Instance for the query on a particular database.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractDistanceKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractDatabaseQuery<O> implements KNNQuery<O, D> {
  /**
   * Hold the distance function to be used.
   */
  protected DistanceQuery<O, D> distanceQuery;

  /**
   * Constructor.
   * 
   * @param database Database
   */
  public AbstractDistanceKNNQuery(Database<? extends O> database, DistanceQuery<O, D> distanceQuery) {
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