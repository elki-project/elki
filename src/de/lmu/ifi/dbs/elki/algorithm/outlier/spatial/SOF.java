package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
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
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * The Spatial Outlier Factor (SOF) is a spatial
 * {@link de.lmu.ifi.dbs.elki.algorithm.outlier.LOF LOF} variation.
 * 
 * Since the "reachability distance" of LOF cannot be used canonically in the
 * bichromatic case, this part of LOF is dropped and the exact distance is used
 * instead.
 * 
 * <p>
 * Huang, T., Qin, X.<br>
 * Detecting outliers in spatial database.<br>
 * In: Proceedings of the 3rd International Conference on Image and Graphics,
 * Hong Kong, China.
 * </p>
 * 
 * A LOF variation simplified with reachDist(o,p) == dist(o,p).
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Neighborhood object type
 * @param <O> Attribute object type
 * @param <D> Distance type
 */
@Title("Spatial Outlier Factor")
@Reference(authors = "Huang, T., Qin, X.", title = "Detecting outliers in spatial database", booktitle = "Proceedings of the 3rd International Conference on Image and Graphics", url = "http://dx.doi.org/10.1109/ICIG.2004.53")
public class SOF<N, O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedSpatialOutlier<N, O, D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SOF.class);

  /**
   * Association ID used for output
   */
  public static final AssociationID<Double> SOF_SCORE = AssociationID.getOrCreateAssociationID("sof", TypeUtil.DOUBLE);

  /**
   * Constructor.
   * 
   * @param npred Neighborhood predicate
   * @param nonSpatialDistanceFunction Distance function on non-spatial
   *        attributes
   */
  public SOF(NeighborSetPredicate.Factory<N> npred, PrimitiveDistanceFunction<O, D> nonSpatialDistanceFunction) {
    super(npred, nonSpatialDistanceFunction);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * The main run method
   * 
   * @param database Database to use (actually unused)
   * @param spatial Relation for neighborhood
   * @param relation Attributes to evaluate
   * @return Outlier result
   */
  public OutlierResult run(Database database, Relation<N> spatial, Relation<O> relation) {
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(spatial);

    WritableDataStore<Double> lrds = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
    WritableDataStore<Double> lofs = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    DoubleMinMax lofminmax = new DoubleMinMax();

    // Compute densities
    for(DBID id : relation.iterDBIDs()) {
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      double avg = 0;
      for(DBID n : neighbors) {
        avg += getNonSpatialDistanceFunction().distance(relation.get(id), relation.get(n)).doubleValue();
      }
      lrds.put(id, 1 / (avg / neighbors.size()));
    }

    // Compute density quotients
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
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("Spatial Outlier Factor", "sof-outlier", SOF_SCORE, lofs, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class
   * 
   * @author Ahmed Hettab
   * 
   * @apiviz.exclude
   * 
   * @param <N> Neighborhood type
   * @param <O> Attribute object type
   * @param <D> Distance type
   */
  public static class Parameterizer<N, O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedSpatialOutlier.Parameterizer<N, O, D> {
    @Override
    protected SOF<N, O, D> makeInstance() {
      return new SOF<N, O, D>(npredf, distanceFunction);
    }
  }
}