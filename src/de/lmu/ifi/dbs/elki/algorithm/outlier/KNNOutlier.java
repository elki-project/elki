package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
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
 * @apiviz.has KNNQuery
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
@Title("KNN outlier: Efficient Algorithms for Mining Outliers from Large Data Sets")
@Description("Outlier Detection based on the distance of an object to its k nearest neighbor.")
@Reference(authors = "S. Ramaswamy, R. Rastogi, K. Shim", title = "Efficient Algorithms for Mining Outliers from Large Data Sets", booktitle = "Proc. of the Int. Conf. on Management of Data, Dallas, Texas, 2000", url = "http://dx.doi.org/10.1145/342009.335437")
public class KNNOutlier<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {
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
   * The parameter k
   */
  private int k;

  /**
   * Constructor for a single kNN query.
   * 
   * @param distanceFunction distance function to use
   * @param k Value of k
   */
  public KNNOutlier(DistanceFunction<O, D> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  @Override
  protected OutlierResult runInTime(Database database) throws IllegalStateException {
    final DistanceQuery<O, D> distanceQuery = getDistanceQuery(database);
    KNNQuery<O, D> knnQuery = database.getKNNQuery(distanceQuery, k);

    if(logger.isVerbose()) {
      logger.verbose("Computing the kNN outlier degree (distance to the k nearest neighbor)");
    }
    FiniteProgress progressKNNDistance = logger.isVerbose() ? new FiniteProgress("kNN distance for objects", database.size(), logger) : null;

    double maxodegree = 0;
    WritableDataStore<Double> knno_score = DataStoreUtil.makeStorage(database.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    // compute distance to the k nearest neighbor.
    for(DBID id : distanceQuery.getRelation().iterDBIDs()) {
      // distance to the kth nearest neighbor
      final List<DistanceResultPair<D>> knns = knnQuery.getKNNForDBID(id, k);
      final int last = Math.min(k - 1, knns.size() - 1);

      double dkn = knns.get(last).getDistance().doubleValue();
      knno_score.put(id, dkn);

      maxodegree = Math.max(maxodegree, dkn);

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

  @Override
  public TypeInformation getInputTypeRestriction() {
    return getDistanceFunction().getInputTypeRestriction();
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
    protected KNNOutlier<O, D> makeInstance() {
      return new KNNOutlier<O, D>(distanceFunction, k);
    }
  }
}