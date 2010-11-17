package de.lmu.ifi.dbs.elki.database.query.range;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndex;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Instance of a range query for a particular spatial index.
 * 
 * @author Erich Schubert
 */
public class SpatialIndexRangeQueryInstance<O extends NumberVector<?, ?>, D extends Distance<D>> extends DatabaseRangeQuery.Instance<O, D> {
  /**
   * The index to use
   */
  SpatialIndex<O, ?, ?> index;

  /**
   * Spatial primitive distance function
   */
  SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction;

  /**
   * Constructor.
   * 
   * @param index Index to use
   * @param distanceQuery Distance query to use
   * @param distanceFunction Distance function
   */
  public SpatialIndexRangeQueryInstance(Database<O> database, SpatialIndex<O, ?, ?> index, DistanceQuery<O, D> distanceQuery, SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction) {
    super(database, distanceQuery);
    this.index = index;
    this.distanceFunction = distanceFunction;
  }

  @Override
  public List<DistanceResultPair<D>> getRangeForObject(O obj, D range) {
    return index.rangeQuery(obj, range, distanceFunction);
  }

  @Override
  public List<DistanceResultPair<D>> getRangeForDBID(DBID id, D range) {
    return getRangeForObject(database.get(id), range);
  }
  
  @SuppressWarnings("unused")
  @Override
  public List<List<DistanceResultPair<D>>> getRangeForBulkDBIDs(ArrayDBIDs ids, D range) {
    // TODO: implement
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }
}