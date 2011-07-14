package experimentalcode.hettab.outlier;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import experimentalcode.shared.outlier.generalized.neighbors.NeighborSetPredicate;

/**
 * 
 * @author Ahmed Hettab
 * 
 * @param <V>
 * @param <D> reachDist(o,p) = dist(o,p)
 */
public class SOF<N, V extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedSpatialOutlier<N, V, D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SOF.class);

  public static final AssociationID<Double> LOF_SCORE = AssociationID.getOrCreateAssociationID("lof", Double.class);

  public SOF(NeighborSetPredicate.Factory<N> npred, PrimitiveDistanceFunction<V, D> nonSpatialDistanceFunction) {
    super(npred, nonSpatialDistanceFunction);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  public OutlierResult run(Database database, Relation<N> spatial, Relation<V> relation) throws IllegalStateException {
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(spatial);

    WritableDataStore<Double> lrds = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
    WritableDataStore<Double> lofs = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    DoubleMinMax lofminmax = new DoubleMinMax();

    for(DBID id : relation.iterDBIDs()) {
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      double avg = 0;
      for(DBID n : neighbors) {
        avg += getNonSpatialDistanceFunction().distance(relation.get(id), relation.get(n)).doubleValue();
      }
      lrds.put(id, 1 / (avg / neighbors.size()));
    }

    for(DBID id : relation.iterDBIDs()) {
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      double avg = 0;
      for(DBID n : neighbors) {
        avg += lrds.get(n);
      }
      final double lrd = (avg / neighbors.size()) / lrds.get(id);
      lofs.put(id, lrd);
      lofminmax.put(lrd);
    }

    // Build result representation.
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("Local Outlier Factor", "lof-outlier", LOF_SCORE, lofs);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD);
  }

  public static class Parameterizer<N, V extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedSpatialOutlier.Parameterizer<N, V, D> {
    @Override
    protected SOF<N, V, D> makeInstance() {
      return new SOF<N, V, D>(npredf, distanceFunction);
    }
  }
}