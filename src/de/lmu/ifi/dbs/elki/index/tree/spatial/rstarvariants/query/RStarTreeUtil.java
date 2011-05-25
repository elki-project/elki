package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;

/**
 * Utility class for RStar trees
 * 
 * @author Erich Schubert
 */
public final class RStarTreeUtil {
  /**
   * Get an RTree range query, using an optimized double implementation when
   * possible.
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param tree Tree to query
   * @param distanceQuery distance query
   * @param hints Optimizer hints
   * @return Query object
   */
  @SuppressWarnings({ "cast", "unchecked" })
  public static <O extends SpatialComparable, D extends Distance<D>> RangeQuery<O, D> getRangeQuery(AbstractRStarTree<?, ?> tree, SpatialDistanceQuery<O, D> distanceQuery, Object... hints) {
    // Can we support this distance function - spatial distances only!
    SpatialPrimitiveDistanceFunction<? super O, D> df = distanceQuery.getDistanceFunction();
    // Can we use an optimized query?
    if(df instanceof SpatialPrimitiveDoubleDistanceFunction) {
      DistanceQuery<O, DoubleDistance> dqc = (DistanceQuery<O, DoubleDistance>) DistanceQuery.class.cast(distanceQuery);
      SpatialPrimitiveDoubleDistanceFunction<? super O> dfc = (SpatialPrimitiveDoubleDistanceFunction<? super O>) SpatialPrimitiveDoubleDistanceFunction.class.cast(df);
      RangeQuery<O, ?> q = new DoubleDistanceRStarTreeRangeQuery<O>(tree, dqc, dfc);
      return (RangeQuery<O, D>) q;
    }
    return new GenericRStarTreeRangeQuery<O, D>(tree, distanceQuery);
  }

  /**
   * Get an RTree knn query, using an optimized double implementation when
   * possible.
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param tree Tree to query
   * @param distanceQuery distance query
   * @param hints Optimizer hints
   * @return Query object
   */
  @SuppressWarnings({ "cast", "unchecked" })
  public static <O extends SpatialComparable, D extends Distance<D>> KNNQuery<O, D> getKNNQuery(AbstractRStarTree<?, ?> tree, SpatialDistanceQuery<O, D> distanceQuery, Object... hints) {
    // Can we support this distance function - spatial distances only!
    SpatialPrimitiveDistanceFunction<? super O, D> df = distanceQuery.getDistanceFunction();
    // Can we use an optimized query?
    if(df instanceof SpatialPrimitiveDoubleDistanceFunction) {
      DistanceQuery<O, DoubleDistance> dqc = (DistanceQuery<O, DoubleDistance>) DistanceQuery.class.cast(distanceQuery);
      SpatialPrimitiveDoubleDistanceFunction<? super O> dfc = (SpatialPrimitiveDoubleDistanceFunction<? super O>) SpatialPrimitiveDoubleDistanceFunction.class.cast(df);
      KNNQuery<O, ?> q = new DoubleDistanceRStarTreeKNNQuery<O>(tree, dqc, dfc);
      return (KNNQuery<O, D>) q;
    }
    return new GenericRStarTreeKNNQuery<O, D>(tree, distanceQuery);
  }
}