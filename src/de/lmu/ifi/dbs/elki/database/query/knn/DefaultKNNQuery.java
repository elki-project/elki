package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndex.SpatialIndexKNNQuery;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Default (on-demand) KNN query class.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public class DefaultKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractFullKNNQuery<O, D> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public DefaultKNNQuery(Parameterization config) {
    super(config);
    config = config.descend(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends O> FullKNNQuery.Instance<T, D> instantiate(Database<T> database) {
    DistanceQuery<T, D> distanceQuery = distanceFunction.instantiate(database);
    // Try to use an index, if present
    if(database instanceof SpatialIndexDatabase && distanceFunction instanceof SpatialPrimitiveDistanceFunction) {
      SpatialIndexDatabase<?, ?, ?> sdb = (SpatialIndexDatabase<?, ?, ?>) database;
      FullKNNQuery.Instance<?, D> knnq = trySpatialKNN(sdb, distanceQuery);
      if(knnq != null) {
        return (FullKNNQuery.Instance<T, D>) knnq;
      }
    }
    return new Instance<T, D>(database, distanceQuery, k);
  }

  @SuppressWarnings("unchecked")
  protected <T extends NumberVector<?, ?>> FullKNNQuery.Instance<?, D> trySpatialKNN(SpatialIndexDatabase<?, ?, ?> database, DistanceQuery<?, D> distanceQuery) {
    DistanceQuery<T, D> dq = (DistanceQuery<T, D>) distanceQuery;
    SpatialIndexDatabase<T, ?, ?> sdb = (SpatialIndexDatabase<T, ?, ?>) database;
    SpatialIndexKNNQuery<T, D> knnq = sdb.getIndex().getKNNQuery(k, dq);
    return knnq;
  }

  /**
   * Instance of this query for a particular database.
   * 
   * @author Erich Schubert
   */
  public static class Instance<O extends DatabaseObject, D extends Distance<D>> extends AbstractFullKNNQuery.Instance<O, D> {
    /**
     * The query k
     */
    final int k;

    /**
     * Constructor.
     * 
     * @param database Database to query
     * @param distanceQuery Distance function to use
     */
    public Instance(Database<O> database, DistanceQuery<O, D> distanceQuery, int k) {
      super(database, distanceQuery);
      this.k = k;
    }

    @Override
    public List<DistanceResultPair<D>> getForDBID(DBID id) {
      return database.kNNQueryForID(id, k, distanceQuery);
    }

    @Override
    public List<DistanceResultPair<D>> getForObject(O obj) {
      return database.kNNQueryForObject(obj, k, distanceQuery);
    }
  }
}