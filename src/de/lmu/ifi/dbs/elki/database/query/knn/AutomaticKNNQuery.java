package de.lmu.ifi.dbs.elki.database.query.knn;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.MetricalIndexDatabase;
import de.lmu.ifi.dbs.elki.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * A KNN query class that will try to use an index structure.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public class AutomaticKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceKNNQuery<O, D> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public AutomaticKNNQuery(Parameterization config) {
    super(config);
    config = config.descend(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends O> Instance<T, D> instantiate(Database<T> database) {
    DistanceQuery<T, D> distanceQuery = distanceFunction.instantiate(database);
    // Try to use an index, if present
    if(database instanceof SpatialIndexDatabase && distanceFunction instanceof SpatialPrimitiveDistanceFunction) {
      SpatialIndexDatabase<?, ?, ?> sdb = (SpatialIndexDatabase<?, ?, ?>) database;
      KNNQuery.Instance<?, D> knnq = trySpatialKNN(sdb, distanceQuery);
      if(knnq != null) {
        return (Instance<T, D>) knnq;
      }
    }
    // Try to use an index, if present
    if(database instanceof MetricalIndexDatabase && distanceFunction instanceof PrimitiveDistanceFunction) {
      MetricalIndexDatabase<?, ?, ?, ?> mdb = (MetricalIndexDatabase<?, ?, ?, ?>) database;
      KNNQuery.Instance<?, D> knnq = tryMetricalKNN(mdb, distanceQuery);
      if(knnq != null) {
        return (Instance<T, D>) knnq;
      }
    }
    return new LinearScanKNNQuery.Instance<T, D>(database, distanceQuery, k);
  }

  @SuppressWarnings("unchecked")
  protected <T extends NumberVector<?, ?>> KNNQuery.Instance<?, D> trySpatialKNN(SpatialIndexDatabase<?, ?, ?> database, DistanceQuery<?, D> distanceQuery) {
    DistanceQuery<T, D> dq = (DistanceQuery<T, D>) distanceQuery;
    SpatialIndexDatabase<T, ?, ?> sdb = (SpatialIndexDatabase<T, ?, ?>) database;
    // FIXME: Check that this distance function is supported by the index!
    SpatialPrimitiveDistanceFunction<? super T, D> sdf = (SpatialPrimitiveDistanceFunction<? super T, D>) distanceQuery.getDistanceFunction();
    SpatialIndexKNNQueryInstance<T, D> knnq = new SpatialIndexKNNQueryInstance<T, D>(sdb.getIndex(), dq, sdf, k);
    return knnq;
  }

  @SuppressWarnings("unchecked")
  protected <T extends NumberVector<?, ?>> KNNQuery.Instance<?, D> tryMetricalKNN(MetricalIndexDatabase<?, ?, ?, ?> database, DistanceQuery<?, D> distanceQuery) {
    DistanceQuery<T, D> dq = (DistanceQuery<T, D>) distanceQuery;
    MetricalIndexDatabase<T, D, ?, ?> mdb = (MetricalIndexDatabase<T, D, ?, ?>) database;
    MetricalIndexKNNQueryInstance<T, D> knnq = new MetricalIndexKNNQueryInstance<T, D>(mdb, mdb.getIndex(), dq, k);
    return knnq;
  }
}