package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalIndex;

/**
 * Instance of a KNN query for a particular spatial index.
 * 
 * @author Erich Schubert
 */
public class MetricalIndexKNNQueryInstance<O extends NumberVector<?, ?>, D extends Distance<D>> implements ObjectKNNQuery.Instance<O, D> {
  /**
   * The index to use
   */
  MetricalIndex<O, D, ?, ?> index;

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
   * @param distanceQuery Distance query used
   * @param k maximum k value
   */
  public MetricalIndexKNNQueryInstance(MetricalIndex<O, D, ?, ?> index, DistanceQuery<O, D> distanceQuery, int k) {
    this.distanceQuery = distanceQuery;
    this.k = k;
  }

  @Override
  public DistanceQuery<O, D> getDistanceQuery() {
    return distanceQuery;
  }

  @Override
  public List<DistanceResultPair<D>> getForObject(O obj) {
    return index.kNNQuery(obj, k);
  }

  @Override
  public D getDistanceFactory() {
    return distanceQuery.getDistanceFactory();
  }
}