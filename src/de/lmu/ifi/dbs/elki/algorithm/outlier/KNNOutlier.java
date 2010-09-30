package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
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
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
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
@Title("KNN outlier: Efficient Algorithms for Mining Outliers from Large Data Sets")
@Description("Outlier Detection based on the distance of an object to its k nearest neighbor.")
@Reference(authors = "S. Ramaswamy, R. Rastogi, K. Shim", title = "Efficient Algorithms for Mining Outliers from Large Data Sets", booktitle = "Proc. of the Int. Conf. on Management of Data, Dallas, Texas, 2000", url = "http://dx.doi.org/10.1145/342009.335437")
public class KNNOutlier<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends AbstractAlgorithm<O, OutlierResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(KNNOutlier.class);
  
  /**
   * The association id to associate the KNNO_KNNDISTANCE of an object for the
   * KNN outlier detection algorithm.
   */
  public static final AssociationID<Double> KNNO_KNNDISTANCE = AssociationID.getOrCreateAssociationID("knno_knndistance", Double.class);

  /**
   * Parameter to specify the k nearest neighbor
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("knno.k", "k nearest neighbor");

  /**
   * KNN query to use
   */
  protected KNNQuery<O, D> knnQuery;

  /**
   * Constructor for a single kNN query.
   * 
   * @param knnQuery knn query object
   */
  public KNNOutlier(KNNQuery<O, D> knnQuery) {
    super();
    this.knnQuery = knnQuery;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    double maxodegree = 0;
    if(logger.isVerbose()) {
      logger.verbose("Computing the kNN outlier degree (distance to the k nearest neighbor)");
    }
    FiniteProgress progressKNNDistance = logger.isVerbose() ? new FiniteProgress("kNN distance for objects", database.size(), logger) : null;

    KNNQuery.Instance<O, D> knnQueryInstance = knnQuery.instantiate(database);
    WritableDataStore<Double> knno_score = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    // compute distance to the k nearest neighbor.
    for(DBID id : database) {
      // distance to the kth nearest neighbor
      final List<DistanceResultPair<D>> knns = knnQueryInstance.get(id);
      double dkn = knns.get(knns.size() - 1).getDistance().doubleValue();

      if(dkn > maxodegree) {
        maxodegree = dkn;
      }
      knno_score.put(id, dkn);

      if(progressKNNDistance != null) {
        progressKNNDistance.incrementProcessed(logger);
      }
    }
    if(progressKNNDistance != null) {
      progressKNNDistance.ensureCompleted(logger);
    }
    AnnotationResult<Double> scoreres = new AnnotationFromDataStore<Double>("kNN Outlier Score", "knn-outlier", KNNO_KNNDISTANCE, knno_score);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(Double.NaN, maxodegree, 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(meta, scoreres);
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return KNN outlier detection algorithm
   */
  public static <O extends DatabaseObject, D extends NumberDistance<D, ?>> KNNOutlier<O, D> parameterize(Parameterization config) {
    int k = getParameterK(config);
    DistanceFunction<O, D> distanceFunction = getParameterDistanceFunction(config);
    KNNQuery<O, D> knnQuery = getParameterKNNQuery(config, k + 1, distanceFunction, DefaultKNNQuery.class);
    if(config.hasErrors()) {
      return null;
    }
    return new KNNOutlier<O, D>(knnQuery);
  }

  /**
   * Get the k parameter for the knn query
   * 
   * @param config Parameterization
   * @return k parameter
   */
  protected static int getParameterK(Parameterization config) {
    final IntParameter param = new IntParameter(K_ID);
    if(config.grab(param)) {
      return param.getValue();
    }
    return -1;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}