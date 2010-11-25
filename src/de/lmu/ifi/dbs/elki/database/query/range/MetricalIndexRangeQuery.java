package de.lmu.ifi.dbs.elki.database.query.range;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalIndex;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Instance of a range query for a particular spatial index.
 * 
 * @author Erich Schubert
 */
public class MetricalIndexRangeQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceRangeQuery<O, D> {
  /**
   * The index to use
   */
  protected final MetricalIndex<O, D, ?, ?> index;

  /**
   * Constructor.
   * 
   * @param database Database to use
   * @param index Index to use
   * @param distanceQuery Distance query used
   */
  public MetricalIndexRangeQuery(Database<? extends O> database, MetricalIndex<O, D, ?, ?> index, DistanceQuery<O, D> distanceQuery) {
    super(database, distanceQuery);
    this.index = index;
  }

  @Override
  public List<DistanceResultPair<D>> getRangeForObject(O obj, D range) {
    return index.rangeQuery(obj, range);
  }

  @Override
  public List<DistanceResultPair<D>> getRangeForDBID(DBID id, D range) {
    // TODO: do this in the DB layer, we might have a better index?
    return getRangeForObject(database.get(id), range);
  }

  @SuppressWarnings("unused")
  @Override
  public List<List<DistanceResultPair<D>>> getRangeForBulkDBIDs(ArrayDBIDs ids, D range) {
    // TODO: implement
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }
}