package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalIndex;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Instance of a KNN query for a particular spatial index.
 * 
 * @author Erich Schubert
 */
public class MetricalIndexKNNQueryInstance<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceKNNQuery.Instance<O, D> {
  /**
   * The index to use
   */
  MetricalIndex<O, D, ?, ?> index;

  /**
   * Constructor.
   *
   * @param database Database to use
   * @param index Index to use
   * @param distanceQuery Distance query used
   */
  public MetricalIndexKNNQueryInstance(Database<O> database, MetricalIndex<O, D, ?, ?> index, DistanceQuery<O, D> distanceQuery) {
    super(database, distanceQuery);
    this.index = index;
  }

  @Override
  public List<DistanceResultPair<D>> getForObject(O obj, int k) {
    return index.kNNQuery(obj, k);
  }

  @Override
  public List<DistanceResultPair<D>> getForDBID(DBID id, int k) {
    // TODO: do this in the DB layer, we might have a better index?
    return getForObject(database.get(id), k);
  }

  @SuppressWarnings("unused")
  @Override
  public List<List<DistanceResultPair<D>>> getForBulkDBIDs(ArrayDBIDs ids, int k) {
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }
}