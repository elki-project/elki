package experimentalcode.lisa;

import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

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

public class KNNOutlierDetection<O extends DatabaseObject, D extends DoubleDistance> extends DistanceBasedAlgorithm<O, DoubleDistance, OutlierResult> {
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
  public KNNOutlierDetection() {
    super();
    // kth nearest neighbor
    addOption(K_PARAM);
  }

  /**
   * Calls the super method and sets additionally the values of the parameter
   * {@link #K_PARAM}
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    k = K_PARAM.getValue();
    return remainingParameters;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */

  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    double maxodegree = 0;
    getDistanceFunction().setDatabase(database, isVerbose(), isTime());

    if(this.isVerbose()) {
      this.verbose("computing outlier degree(distance to the k nearest neighbor");
    }
    FiniteProgress progressKNNDistance = new FiniteProgress("KNNOD_KNNDISTANCE for objects", database.size());
    int counter = 0;

    HashMap<Integer, Double> knno_score = new HashMap<Integer,Double>(database.size());
    // compute distance to the k nearest neighbor.
    for(Integer id : database) {
      counter++;
      // distance to the kth nearest neighbor
      Double dkn = database.kNNQueryForID(id, k, getDistanceFunction()).get(k - 1).getDistance().getValue();

      if(dkn > maxodegree) {
        maxodegree = dkn;
      }
      knno_score.put(id, dkn);

      if(this.isVerbose()) {
        progressKNNDistance.setProcessed(counter);
        this.progress(progressKNNDistance);
      }
    }
    AnnotationFromHashMap<Double> res1 = new AnnotationFromHashMap<Double>(KNNO_KNNDISTANCE, knno_score);
    OrderingFromHashMap<Double> res2 = new OrderingFromHashMap<Double>(knno_score, true);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(Double.NaN, maxodegree, 0.0, Double.POSITIVE_INFINITY);
    // combine results.
    result = new OutlierResult(meta, res1, res2);
    return result;

  }

  @Override
  public Description getDescription() {
    return new Description("KNN outlier detection", "Efficient Algorithms for Mining Outliers from Large Data Sets", "Outlier Detection based on the distance of an object to its k nearest neighbor.", "S. Ramaswamy, R. Rastogi, K. Shim: " + "Efficient Algorithms for Mining Outliers from Large Data Sets. " + "In: Proc. of the Int. Conf. on Management of Data, Dallas, Texas, 2000.");
  }

  @Override
  public OutlierResult getResult() {
    return result;
  }
}
