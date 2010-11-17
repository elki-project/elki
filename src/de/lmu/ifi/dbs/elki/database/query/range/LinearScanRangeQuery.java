package de.lmu.ifi.dbs.elki.database.query.range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Default linear scan range query class.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public class LinearScanRangeQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceRangeQuery<O, D> {
  /**
   * Constructor.
   * 
   * @param database Database to query
   * @param distanceQuery Distance function to use
   */
  public LinearScanRangeQuery(Database<? extends O> database, DistanceQuery<O, D> distanceQuery) {
    super(database, distanceQuery);
  }

  @Override
  public List<DistanceResultPair<D>> getRangeForDBID(DBID id, D range) {
    List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
    for(DBID currentID : database) {
      D currentDistance = distanceQuery.distance(id, currentID);
      if(currentDistance.compareTo(range) <= 0) {
        result.add(new DistanceResultPair<D>(currentDistance, currentID));
      }
    }
    Collections.sort(result);
    return result;
  }

  @SuppressWarnings("unused")
  @Override
  public List<List<DistanceResultPair<D>>> getRangeForBulkDBIDs(ArrayDBIDs ids, D range) {
    // TODO: implement
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  @Override
  public List<DistanceResultPair<D>> getRangeForObject(O obj, D range) {
    List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
    for(DBID currentID : database) {
      D currentDistance = distanceQuery.distance(currentID, obj);
      if(currentDistance.compareTo(range) <= 0) {
        result.add(new DistanceResultPair<D>(currentDistance, currentID));
      }
    }
    Collections.sort(result);
    return result;
  }
}