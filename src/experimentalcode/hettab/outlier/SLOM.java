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
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import experimentalcode.shared.outlier.generalized.neighbors.NeighborSetPredicate;

/**
 * SLOM: a new measure for local spatial outliers
 * 
 * <p>
 * Reference:<br>
 * Sanjay Chawla and Pei Sun<br>
 * SLOM: a new measure for local spatial outliers<br>
 * in Knowledge and Information Systems 2005
 * </p>
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> the type the spatial neighborhood is defined over
 * @param <V> the type of DatabaseObjects handled by the algorithm
 * @param <D> the type of Distance used for non spatial attributes
 */
@Title("SLOM: a new measure for local spatial outliers")
@Description("Spatial local outlier measure (SLOM), which captures the local behaviour of datum in their spatial neighbourhood")
@Reference(authors = "Sanjay Chawla and Pei Sun", title = "SLOM: a new measure for local spatial outliers", booktitle = "Knowledge and Information Systems 2005", url = "http://rp-www.cs.usyd.edu.au/~chawlarg/papers/KAIS_online.pdf")
public class SLOM<N, V extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedSpatialOutlier<N, V, D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SLOM.class);

  /**
   * The association id to associate the SLOM_SCORE of an object for the SLOM
   * algorithm.
   */
  public static final AssociationID<Double> SLOM_SCORE = AssociationID.getOrCreateAssociationID("slom", Double.class);

  /**
   * Constructor.
   * 
   * @param npred Neighborhood predicate
   * @param nonSpatialDistanceFunction Distance function to use on the
   *        non-spatial attributes
   */
  public SLOM(NeighborSetPredicate.Factory<N> npred, PrimitiveDistanceFunction<V, D> nonSpatialDistanceFunction) {
    super(npred, nonSpatialDistanceFunction);
  }

  /**
   * @param database Database to process
   * @param spatial Spatial Relation to use.
   * @param relation Relation to use.
   * @return Outlier detection result
   */
  public OutlierResult run(Database database, Relation<N> spatial, Relation<V> relation) {
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(spatial);

    WritableDataStore<Double> modifiedDistance = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    // calculate D-Tilde
    for(DBID id : relation.getDBIDs()) {
      double sum = 0;
      double maxDist = 0;
      int cnt = 0;

      final DBIDs neighbors = npred.getNeighborDBIDs(id);
      for(DBID neighbor : neighbors) {
        if(id.equals(neighbor)) {
          continue;
        }
        double dist = getNonSpatialDistanceFunction().distance(relation.get(id), relation.get(neighbor)).doubleValue();
        sum += dist;
        cnt++;
        maxDist = Math.max(maxDist, dist);
      }
      modifiedDistance.put(id, ((sum - maxDist) / (cnt - 1)));
    }

    // Second step - compute actual SLOM values
    DoubleMinMax slomminmax = new DoubleMinMax();
    WritableDataStore<Double> sloms = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);

    for(DBID id : relation.getDBIDs()) {
      double sum = 0;
      int cnt = 0;

      final DBIDs neighbors = npred.getNeighborDBIDs(id);
      for(DBID neighbor : neighbors) {
        if(neighbor.equals(id)) {
          continue;
        }
        sum += modifiedDistance.get(neighbor);
        cnt++;
      }
      // TODO: assert that neighbors contained the object itself.
      double avgPlus = (sum + modifiedDistance.get(id)) / (cnt + 1);
      double avg = sum / cnt;

      double beta = 0;
      for(DBID neighbor : neighbors) {
        if(modifiedDistance.get(neighbor).doubleValue() > avgPlus) {
          beta += 1;
        }
        if(modifiedDistance.get(neighbor).doubleValue() < avgPlus) {
          beta -= 1;
        }
      }
      beta = Math.abs(beta);
      beta = Math.max(beta, 1) / (cnt - 2);
      beta = beta / (1 + avg);

      double slom = beta * modifiedDistance.get(id);
      sloms.put(id, slom);
      slomminmax.put(slom);
    }

    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("SLOM", "SLOM-outlier", SLOM_SCORE, sloms);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(slomminmax.getMin(), slomminmax.getMax(), 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class.
   * 
   * @author Ahmed Hettab
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<N, V extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedSpatialOutlier.Parameterizer<N, V, D> {
    @Override
    protected SLOM<N, V, D> makeInstance() {
      return new SLOM<N, V, D>(npredf, distanceFunction);
    }
  }
}