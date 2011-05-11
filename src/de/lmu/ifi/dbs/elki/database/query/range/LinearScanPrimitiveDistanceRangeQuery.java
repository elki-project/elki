package de.lmu.ifi.dbs.elki.database.query.range;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Default linear scan range query class.
 * 
 * Subtle optimization: for primitive distances, retrieve the query object only
 * once from the relation.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses PrimitiveDistanceQuery
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public class LinearScanPrimitiveDistanceRangeQuery<O, D extends Distance<D>> extends LinearScanRangeQuery<O, D> {
  /**
   * Constructor.
   * 
   * @param relation Data to query
   * @param distanceQuery Distance function to use
   */
  public LinearScanPrimitiveDistanceRangeQuery(Relation<? extends O> relation, PrimitiveDistanceQuery<O, D> distanceQuery) {
    super(relation, distanceQuery);
  }

  @Override
  public List<DistanceResultPair<D>> getRangeForDBID(DBID id, D range) {
    return getRangeForObject(relation.get(id), range);
  }
}