package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
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
 * @apiviz.has KNNQuery
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
@Title("KNNWeight outlier detection")
@Description("Outlier Detection based on the distances of an object to its k nearest neighbors.")
@Reference(authors = "F. Angiulli, C. Pizzuti", title = "Fast Outlier Detection in High Dimensional Spaces", booktitle = "Proc. European Conference on Principles of Knowledge Discovery and Data Mining (PKDD'02), Helsinki, Finland, 2002", url = "http://dx.doi.org/10.1007/3-540-45681-3_2")
public class KNNWeightOutlier<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {
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
   * Constructor with parameters.
   * 
   * @param distanceFunction Distance function
   * @param k k Parameter
   */
  public KNNWeightOutlier(DistanceFunction<? super O, D> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    final DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O, D> knnQuery = database.getKNNQuery(distanceQuery, k);

    if(logger.isVerbose()) {
      logger.verbose("computing outlier degree(sum of the distances to the k nearest neighbors");
    }
    FiniteProgress progressKNNWeight = logger.isVerbose() ? new FiniteProgress("KNNWOD_KNNWEIGHT for objects", relation.size(), logger) : null;

    double maxweight = 0;

    // compute distance to the k nearest neighbor. n objects with the highest
    // distance are flagged as outliers
    WritableDataStore<Double> knnw_score = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : distanceQuery.getRelation().iterDBIDs()) {
      // compute sum of the distances to the k nearest neighbors

      List<DistanceResultPair<D>> knn = knnQuery.getKNNForDBID(id, k);
      D skn = knn.get(0).getDistance();
      final int last = Math.min(k + 1, knn.size());
      for(int i = 1; i < last; i++) {
        skn = skn.plus(knn.get(i).getDistance());
      }

      double doubleSkn = skn.getValue().doubleValue();
      knnw_score.put(id, doubleSkn);
      maxweight = Math.max(maxweight, doubleSkn);

      if(progressKNNWeight != null) {
        progressKNNWeight.incrementProcessed(logger);
      }
    }
    if(progressKNNWeight != null) {
      progressKNNWeight.ensureCompleted(logger);
    }

    AnnotationResult<Double> res = new AnnotationFromDataStore<Double>("Weighted kNN Outlier Score", "knnw-outlier", KNNWOD_WEIGHT, knnw_score, relation.getDBIDs());
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(Double.NaN, maxweight, 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(meta, res);
  }
  
  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    protected int k = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected KNNWeightOutlier<O, D> makeInstance() {
      return new KNNWeightOutlier<O, D>(distanceFunction, k);
    }
  }
}