package de.lmu.ifi.dbs.elki.database.query;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Interface that is a combination of distance and a similarity function.
 * For combined implementations of both.
 * 
 * @author Erich Schubert
 *
 * @param <O> Database object type
 * @param <D> Distance type
 */
public interface DistanceSimilarityQuery<O extends DatabaseObject, D extends Distance<D>> extends DistanceQuery<O, D>, SimilarityQuery<O, D> {
  // Empty
}
