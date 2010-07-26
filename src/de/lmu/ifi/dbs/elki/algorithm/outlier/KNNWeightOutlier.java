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
public class KNNWeightOutlier<O extends DatabaseObject, D extends DoubleDistance> extends AbstractDistanceBasedAlgorithm<O, DoubleDistance, OutlierResult> {
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("knnwod.k", "k nearest neighbor");

  /**
   * Association ID for the KNN Weight Outlier Detection
   */
  public static final AssociationID<Double> KNNWOD_WEIGHT = AssociationID.getOrCreateAssociationID("knnwod_weight", Double.class);

  /**
   * Parameter to specify the k nearest neighbor,
   * 
   * <p>
   * Key: {@code -knnwod.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID);

  /**
   * OptionID for {@link #KNNQUERY_PARAM}
   */
  public static final OptionID KNNQUERY_ID = OptionID.getOrCreateOptionID("knnwod.knnquery", "kNN query to use");

  /**
   * The kNN query used
   * 
   * Default value: {@link DefaultKNNQuery} </p>
   * <p>
   * Key: {@code -knnwod.knnquery}
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
  public KNNWeightOutlier(Parameterization config) {
    super(config);
    // k nearest neighbor
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
    double maxweight = 0;

    if(this.isVerbose()) {
      this.verbose("computing outlier degree(sum of the distances to the k nearest neighbors");
    }
    FiniteProgress progressKNNWeight = logger.isVerbose() ? new FiniteProgress("KNNWOD_KNNWEIGHT for objects", database.size(), logger) : null;
    int counter = 0;

    KNNQuery.Instance<O, DoubleDistance> knnQueryInstance = knnQuery.instantiate(database);
    // compute distance to the k nearest neighbor. n objects with the highest
    // distance are flagged as outliers
    WritableDataStore<Double> knnw_score = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : database) {
      counter++;
      // compute sum of the distances to the k nearest neighbors

      List<DistanceResultPair<DoubleDistance>> knn = knnQueryInstance.get(id);
      DoubleDistance skn = knn.get(0).getFirst();
      for(int i = 1; i < Math.min(k, knn.size()); i++) {
        skn = skn.plus(knn.get(i).getFirst());
      }

      double doubleSkn = skn.getValue();
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

    AnnotationResult<Double> res1 = new AnnotationFromDataStore<Double>(KNNWOD_WEIGHT, knnw_score);
    OrderingResult res2 = new OrderingFromDataStore<Double>(knnw_score, true);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(Double.NaN, maxweight, 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(meta, res1, res2);
  }
}