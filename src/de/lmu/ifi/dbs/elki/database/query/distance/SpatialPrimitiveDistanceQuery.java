package de.lmu.ifi.dbs.elki.database.query.distance;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Distance query for spatial distance functions
 * @author Erich Schubert
 *
 * @apiviz.uses SpatialPrimitiveDistanceFunction
 * 
 * @param <V> Vector type to use
 * @param <D> Distance result type
 */
public class SpatialPrimitiveDistanceQuery<V extends FeatureVector<?, ?> & SpatialComparable, D extends Distance<D>> extends PrimitiveDistanceQuery<V, D> implements SpatialDistanceQuery<V, D> {
  /**
   * The distance function we use.
   */
  final protected SpatialPrimitiveDistanceFunction<? super V, D> distanceFunction;
  
  /**
   * @param relation Representation to use
   * @param distanceFunction Distance function to use
   */
  public SpatialPrimitiveDistanceQuery(Relation<? extends V> relation, SpatialPrimitiveDistanceFunction<? super V, D> distanceFunction) {
    super(relation, distanceFunction);
    this.distanceFunction = distanceFunction;
  }

  @Override
  public D centerDistance(SpatialComparable mbr1, SpatialComparable mbr2) {
    return distanceFunction.centerDistance(mbr1, mbr2);
  }

  @Override
  public D mbrDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    return distanceFunction.mbrDist(mbr1, mbr2);
  }

  @Override
  public D minDist(SpatialComparable mbr, V v) {
    return distanceFunction.minDist(mbr, v);
  }

  @Override
  public D minDist(SpatialComparable mbr, DBID id) {
    return distanceFunction.minDist(mbr, relation.get(id));
  }

  @Override
  public SpatialPrimitiveDistanceFunction<? super V, D> getDistanceFunction() {
    return distanceFunction;
  }
}