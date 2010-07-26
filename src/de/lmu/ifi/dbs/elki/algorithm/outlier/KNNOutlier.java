package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DefaultKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromDataStore;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
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
@Title("KNN outlier: Efficient Algorithms for Mining Outliers from Large Data Sets")
@Description("Outlier Detection based on the distance of an object to its k nearest neighbor.")
@Reference(authors = "S. Ramaswamy, R. Rastogi, K. Shim", title = "Efficient Algorithms for Mining Outliers from Large Data Sets", booktitle = "Proc. of the Int. Conf. on Management of Data, Dallas, Texas, 2000", url = "http://dx.doi.org/10.1145/342009.335437")
public class KNNOutlier<O extends DatabaseObject, D extends DoubleDistance> extends AbstractDistanceBasedAlgorithm<O, DoubleDistance, OutlierResult> {
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
   * OptionID for {@link #KNNQUERY_PARAM}
   */
  public static final OptionID KNNQUERY_ID = OptionID.getOrCreateOptionID("knno.knnquery", "kNN query to use");

  /**
   * The kNN query used
   * 
   * Default value: {@link DefaultKNNQuery} </p>
   * <p>
   * Key: {@code -knno.knnquery}
   * </p>
   */
  private final ClassParameter<KNNQuery<O, DoubleDistance>> KNNQUERY_PARAM = new ClassParameter<KNNQuery<O, DoubleDistance>>(KNNQUERY_ID, KNNQuery.class, DefaultKNNQuery.class);

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  private int k;

  /**
   * KNN query to use
   */
  protected KNNQuery<O, DoubleDistance> knnQuery;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public KNNOutlier(Parameterization config) {
    super(config);
    // kth nearest neighbor
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
    // configure kNN query
    if(config.grab(KNNQUERY_PARAM) && DISTANCE_FUNCTION_PARAM.isDefined()) {
      ListParameterization knnParams = new ListParameterization();
      knnParams.addParameter(KNNQuery.K_ID, (k + 1));
      knnParams.addParameter(KNNQuery.DISTANCE_FUNCTION_ID, getDistanceFunction());
      ChainedParameterization chain = new ChainedParameterization(knnParams, config);
      chain.errorsTo(config);
      knnQuery = KNNQUERY_PARAM.instantiateClass(chain);
      knnParams.reportInternalParameterizationErrors(config);
    }
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    double maxodegree = 0;
    if(this.isVerbose()) {
      this.verbose("computing outlier degree (distance to the k nearest neighbor)");
    }
    FiniteProgress progressKNNDistance = logger.isVerbose() ? new FiniteProgress("KNNOD_KNNDISTANCE for objects", database.size(), logger) : null;
    int counter = 0;

    KNNQuery.Instance<O, DoubleDistance> knnQueryInstance = knnQuery.instantiate(database);
    WritableDataStore<Double> knno_score = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    // compute distance to the k nearest neighbor.
    for(DBID id : database) {
      counter++;
      // distance to the kth nearest neighbor
      final List<DistanceResultPair<DoubleDistance>> knns = knnQueryInstance.get(id);
      Double dkn = knns.get(knns.size() - 1).getDistance().getValue();

      if(dkn > maxodegree) {
        maxodegree = dkn;
      }
      knno_score.put(id, dkn);

      if(progressKNNDistance != null) {
        progressKNNDistance.setProcessed(counter, logger);
      }
    }
    if(progressKNNDistance != null) {
      progressKNNDistance.ensureCompleted(logger);
    }
    AnnotationResult<Double> res1 = new AnnotationFromDataStore<Double>(KNNO_KNNDISTANCE, knno_score);
    OrderingResult res2 = new OrderingFromDataStore<Double>(knno_score, true);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(Double.NaN, maxodegree, 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(meta, res1, res2);
  }
}