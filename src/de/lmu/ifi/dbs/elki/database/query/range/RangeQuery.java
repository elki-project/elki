package de.lmu.ifi.dbs.elki.database.query.range;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * The interface for range queries
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses DistanceResultPair oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public interface RangeQuery<O, D extends Distance<D>> extends DatabaseQuery {
  /**
   * Get the nearest neighbors for a particular id in a given query range
   * 
   * @param id query object ID
   * @param range Query range
   * @return neighbors
   */
  public List<DistanceResultPair<D>> getRangeForDBID(DBID id, D range);

  /**
   * Bulk query method
   * 
   * @param ids query object IDs
   * @param range Query range
   * @return neighbors
   */
  public List<List<DistanceResultPair<D>>> getRangeForBulkDBIDs(ArrayDBIDs ids, D range);

  /**
   * Get the nearest neighbors for a particular object in a given query range
   * 
   * @param obj Query object
   * @param range Query range
   * @return neighbors
   */
  public List<DistanceResultPair<D>> getRangeForObject(O obj, D range);

  /**
   * Get the distance data type of the function.
   */
  public D getDistanceFactory();
  
  /**
   * Access the underlying data query.
   * 
   * @return data query in use
   */
  public abstract Relation<? extends O> getRepresentation();
}