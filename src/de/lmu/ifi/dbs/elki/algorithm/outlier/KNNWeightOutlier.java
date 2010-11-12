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
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanKNNQuery;
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
 * Outlier Detection based on the accumulated distances of a point to its k
 * nearest neighbors.
 * 
 * Based on: F. Angiulli, C. Pizzuti: Fast Outlier Detection in High Dimensional
 * Spaces. In: Proc. European Conference on Principles of Knowledge Discovery
 * and Data Mining (PKDD'02), Helsinki, Finland, 2002.
 * 
 * @author Lisa Reichert
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
@Title("KNNWeight outlier detection")
@Description("Outlier Detection based on the distances of an object to its k nearest neighbors.")
@Reference(authors = "F. Angiulli, C. Pizzuti", title = "Fast Outlier Detection in High Dimensional Spaces", booktitle = "Proc. European Conference on Principles of Knowledge Discovery and Data Mining (PKDD'02), Helsinki, Finland, 2002", url = "http://dx.doi.org/10.1007/3-540-45681-3_2")
public class KNNWeightOutlier<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends AbstractAlgorithm<O, OutlierResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(KNNWeightOutlier.class);
  
  /**
   * Parameter to specify the k nearest neighbor
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("knnwod.k", "k nearest neighbor");

  /**
   * Association ID for the KNN Weight Outlier Detection
   */
  public static final AssociationID<Double> KNNWOD_WEIGHT = AssociationID.getOrCreateAssociationID("knnwod_weight", Double.class);

  /**
   * The kNN query used.
   */
  public static final OptionID KNNQUERY_ID = OptionID.getOrCreateOptionID("knnwod.knnquery", "kNN query to use");

  /**
   * Holds the value of {@link #K_ID}.
   */
  private int k;

  /**
   * KNN query to use
   */
  protected KNNQuery<O, D> knnQuery;

  /**
   * Constructor with parameters.
   * 
   * @param k k Parameter
   * @param knnQuery knn query object 
   */
  public KNNWeightOutlier(int k, KNNQuery<O, D> knnQuery) {
    super();
    this.k = k;
    this.knnQuery = knnQuery;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    double maxweight = 0;

    if(logger.isVerbose()) {
      logger.verbose("computing outlier degree(sum of the distances to the k nearest neighbors");
    }
    FiniteProgress progressKNNWeight = logger.isVerbose() ? new FiniteProgress("KNNWOD_KNNWEIGHT for objects", database.size(), logger) : null;
    int counter = 0;

    KNNQuery.Instance<O, D> knnQueryInstance = knnQuery.instantiate(database);
    // compute distance to the k nearest neighbor. n objects with the highest
    // distance are flagged as outliers
    WritableDataStore<Double> knnw_score = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : database) {
      counter++;
      // compute sum of the distances to the k nearest neighbors

      List<DistanceResultPair<D>> knn = knnQueryInstance.getForDBID(id);
      D skn = knn.get(0).getFirst();
      for(int i = 1; i < Math.min(k + 1, knn.size()); i++) {
        skn = skn.plus(knn.get(i).getFirst());
      }

      double doubleSkn = skn.getValue().doubleValue();
      if(doubleSkn > maxweight) {
        maxweight = doubleSkn;
      }
      knnw_score.put(id, doubleSkn);

      if(progressKNNWeight != null) {
        progressKNNWeight.setProcessed(counter, logger);
      }
    }
    if(progressKNNWeight != null) {
      progressKNNWeight.ensureCompleted(logger);
    }

    AnnotationResult<Double> res = new AnnotationFromDataStore<Double>("Weighted kNN Outlier Score", "knnw-outlier", KNNWOD_WEIGHT, knnw_score);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(Double.NaN, maxweight, 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(meta, res);
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return KNN outlier detection algorithm
   */
  public static <O extends DatabaseObject, D extends NumberDistance<D, ?>> KNNWeightOutlier<O, D> parameterize(Parameterization config) {
    int k = getParameterK(config);
    DistanceFunction<O, D> distanceFunction = getParameterDistanceFunction(config);
    KNNQuery<O, D> knnQuery = getParameterKNNQuery(config, k + 1, distanceFunction, LinearScanKNNQuery.class);
    if(config.hasErrors()) {
      return null;
    }
    return new KNNWeightOutlier<O, D>(k, knnQuery);
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