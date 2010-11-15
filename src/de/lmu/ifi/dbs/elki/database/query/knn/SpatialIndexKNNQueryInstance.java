package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndex;

/**
 * Instance of a KNN query for a particular spatial index.
 * 
 * @author Erich Schubert
 */
public class SpatialIndexKNNQueryInstance<O extends NumberVector<?, ?>, D extends Distance<D>> extends DatabaseKNNQuery.Instance<O, D> {
  /**
   * The index to use
   */
  SpatialIndex<O, ?, ?> index;

  /**
   * Spatial primitive distance function
   */
  SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction;

  /**
   * The query k
   */
  final int k;

  /**
   * Distance query
   */
  private DistanceQuery<O, D> distanceQuery;

  /**
   * Constructor.
   * 
   * @param index Index to use
   * @param distanceQuery Distance query to use
   * @param distanceFunction Distance function
   * @param k maximum k value
   */
  public SpatialIndexKNNQueryInstance(Database<O> database, SpatialIndex<O, ?, ?> index, DistanceQuery<O, D> distanceQuery, SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction, int k) {
    super(database, distanceQuery);
    this.index = index;
    this.distanceFunction = distanceFunction;
    this.k = k;
  }

  @Override
  public List<DistanceResultPair<D>> getForObject(O obj) {
    return index.kNNQuery(obj, k, distanceFunction);
  }

  @Override
  public D getDistanceFactory() {
    return distanceQuery.getDistanceFactory();
  }

  @Override
  public List<DistanceResultPair<D>> getForDBID(DBID id) {
    return getForObject(database.get(id));
  }
}