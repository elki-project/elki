package experimentalcode.lisa;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromDataStore;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * <p>
 * Outlier Detection based on the distance of an object to its k nearest
 * neighbor.
 * </p>
 * 
 * <p>
 * Reference:<br>
 * S. Ramaswamy, R. Rastogi, K. Shim: Efficient Algorithms for Mining Outliers
 * from Large Data Sets.</br> In: Proc. of the Int. Conf. on Management of Data,
 * Dallas, Texas, 2000.
 * </p>
 * 
 * @author Lisa Reichert
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */

public class KNNOutlierDetection<O extends DatabaseObject, D extends DoubleDistance> extends AbstractDistanceBasedAlgorithm<O, DoubleDistance, OutlierResult> {
  /**
   * The association id to associate the KNNO_KNNDISTANCE of an object for the
   * KNN outlier detection algorithm.
   */
  public static final AssociationID<Double> KNNO_KNNDISTANCE = AssociationID.getOrCreateAssociationID("knno_knndistance", Double.class);

  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("knno.k", "k nearest neighbor");

  /**
   * Parameter to specify the k nearest neighbor,
   * 
   * <p>
   * Key: {@code -knno.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID);

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  private int k;

  /**
   * Provides the result of the algorithm.
   */
  OutlierResult result;

  /**
   * Constructor, adding options to option handler.
   */
  public KNNOutlierDetection(Parameterization config) {
    super(config);
    config = config.descend(this);
    // kth nearest neighbor
    if (config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */

  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    DistanceQuery<O, DoubleDistance> distFunc = getDistanceFunction().instantiate(database);
    double maxodegree = 0;

    if(this.isVerbose()) {
      this.verbose("computing outlier degree(distance to the k nearest neighbor");
    }
    FiniteProgress progressKNNDistance = new FiniteProgress("KNNOD_KNNDISTANCE for objects", database.size());
    int counter = 0;

    WritableDataStore<Double> knno_score = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.class);
    // compute distance to the k nearest neighbor.
    for(DBID id : database) {
      counter++;
      // distance to the kth nearest neighbor
      Double dkn = database.kNNQueryForID(id, k, distFunc).get(k - 1).getDistance().getValue();

      if(dkn > maxodegree) {
        maxodegree = dkn;
      }
      knno_score.put(id, dkn);

      if(this.isVerbose()) {
        progressKNNDistance.setProcessed(counter);
        this.progress(progressKNNDistance);
      }
    }
    AnnotationResult<Double> res1 = new AnnotationFromDataStore<Double>(KNNO_KNNDISTANCE, knno_score);
    OrderingResult res2 = new OrderingFromDataStore<Double>(knno_score, true);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(Double.NaN, maxodegree, 0.0, Double.POSITIVE_INFINITY);
    // combine results.
    result = new OutlierResult(meta, res1, res2);
    return result;

  }

  /*@Override
  public OldDescription getDescription() {
    return new OldDescription("KNN outlier detection", "Efficient Algorithms for Mining Outliers from Large Data Sets", "Outlier Detection based on the distance of an object to its k nearest neighbor.", "S. Ramaswamy, R. Rastogi, K. Shim: " + "Efficient Algorithms for Mining Outliers from Large Data Sets. " + "In: Proc. of the Int. Conf. on Management of Data, Dallas, Texas, 2000.");
  }*/
}
