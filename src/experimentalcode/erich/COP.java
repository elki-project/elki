package experimentalcode.erich;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.ErrorFunctions;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Algorithm to compute local correlation outlier probability.
 * <p/>
 * Publication pending
 * 
 * @author Erich Schubert
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("COP: Correlation Outlier Probability")
@Description("Algorithm to compute correlation-based local outlier probabilitys in a database based on the parameter 'k' and different distance functions.")
public class COP<V extends NumberVector<V, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<V, D, OutlierResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(COP.class);

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its COP_SCORE, must be an integer greater than 0.
   * <p/>
   * Key: {@code -cop.k}
   * </p>
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("cop.k", "The number of nearest neighbors of an object to be considered for computing its COP_SCORE.");

  /**
   * Number of neighbors to be considered.
   */
  int k;

  /**
   * Holds the object performing the dependency derivation
   */
  private DependencyDerivator<V, D> dependencyDerivator;

  /**
   * The association id to associate the Correlation Outlier Probability of an
   * object
   */
  public static final AssociationID<Double> COP_SCORE = AssociationID.getOrCreateAssociationID("cop", Double.class);

  /**
   * The association id to associate the COP_SCORE error vector of an object for
   * the COP_SCORE algorithm.
   */
  public static final AssociationID<Vector> COP_ERROR_VECTOR = AssociationID.getOrCreateAssociationID("cop error vector", Vector.class);

  /**
   * The association id to associate the COP_SCORE data vector of an object for
   * the COP_SCORE algorithm.
   */
  // TODO: use or remove.
  public static final AssociationID<Matrix> COP_DATA_VECTORS = AssociationID.getOrCreateAssociationID("cop data vectors", Matrix.class);

  /**
   * The association id to associate the COP_SCORE correlation dimensionality of
   * an object for the COP_SCORE algorithm.
   */
  public static final AssociationID<Integer> COP_DIM = AssociationID.getOrCreateAssociationID("cop dim", Integer.class);

  /**
   * The association id to associate the COP_SCORE correlation solution
   */
  public static final AssociationID<CorrelationAnalysisSolution<?>> COP_SOL = AssociationID.getOrCreateAssociationIDGenerics("cop sol", CorrelationAnalysisSolution.class);

  /**
   * Constructor.
   * 
   * @param distanceFunction
   * @param k
   * @param dependencyDerivator
   */
  public COP(DistanceFunction<? super V, D> distanceFunction, int k, DependencyDerivator<V, D> dependencyDerivator) {
    super(distanceFunction);
    this.k = k;
    this.dependencyDerivator = dependencyDerivator;
  }

  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    Relation<V> data = getRelation(database);
    KNNQuery<V, D> knnQuery = database.getKNNQuery(data, getDistanceFunction(), k + 1);

    DBIDs ids = data.getDBIDs();

    WritableDataStore<Double> cop_score = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.class);
    WritableDataStore<Vector> cop_err_v = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Vector.class);
    WritableDataStore<Matrix> cop_datav = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Matrix.class);
    WritableDataStore<Integer> cop_dim = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Integer.class);
    WritableDataStore<CorrelationAnalysisSolution<?>> cop_sol = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, CorrelationAnalysisSolution.class);
    {// compute neighbors of each db object
      FiniteProgress progressLocalPCA = logger.isVerbose() ? new FiniteProgress("Correlation Outlier Probabilities", database.size(), logger) : null;
      double sqrt2 = Math.sqrt(2.0);
      for(DBID id : data.iterDBIDs()) {
        List<DistanceResultPair<D>> neighbors = knnQuery.getKNNForDBID(id, k + 1);
        neighbors.remove(0);

        ModifiableDBIDs nids = DBIDUtil.newArray(neighbors.size());
        for(DistanceResultPair<D> n : neighbors) {
          nids.add(n.getID());
        }

        // TODO: do we want to use the query point as centroid?
        CorrelationAnalysisSolution<V> depsol = dependencyDerivator.generateModel(data, nids);

        // temp code, experimental.
        /*
         * if(false) { double traddistance =
         * depsol.getCentroid().minus(database.
         * get(id).getColumnVector()).euclideanNorm(0); if(traddistance > 0.0) {
         * double distance = depsol.distance(database.get(id));
         * cop_score.put(id, distance / traddistance); } else {
         * cop_score.put(id, 0.0); } }
         */
        double stddev = depsol.getStandardDeviation();
        double distance = depsol.distance(data.get(id));
        double prob = ErrorFunctions.erf(distance / (stddev * sqrt2));

        cop_score.put(id, prob);

        Vector errv = depsol.errorVector(data.get(id));
        cop_err_v.put(id, errv);

        Matrix datav = depsol.dataProjections(data.get(id));
        cop_datav.put(id, datav);

        cop_dim.put(id, depsol.getCorrelationDimensionality());

        cop_sol.put(id, depsol);

        if(progressLocalPCA != null) {
          progressLocalPCA.incrementProcessed(logger);
        }
      }
      if(progressLocalPCA != null) {
        progressLocalPCA.ensureCompleted(logger);
      }
    }
    // combine results.
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("Correlation Outlier Probabilities", "cop-outlier", COP_SCORE, cop_score);
    OutlierScoreMeta scoreMeta = new ProbabilisticOutlierScore();
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    // extra results
    result.addChildResult(new AnnotationFromDataStore<Integer>("Local Dimensionality", "cop-dim", COP_DIM, cop_dim));
    result.addChildResult(new AnnotationFromDataStore<Vector>("Error vectors", "cop-errorvec", COP_ERROR_VECTOR, cop_err_v));
    result.addChildResult(new AnnotationFromDataStore<Matrix>("Data vectors", "cop-datavec", COP_DATA_VECTORS, cop_datav));
    result.addChildResult(new AnnotationFromDataStore<CorrelationAnalysisSolution<?>>("Correlation analysis", "cop-sol", COP_SOL, cop_sol));
    return result;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
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
  public static class Parameterizer<V extends NumberVector<V, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<V, D> {
    /**
     * Number of neighbors to be considered.
     */
    int k;

    /**
     * Holds the object performing the dependency derivation
     */
    protected DependencyDerivator<V, D> dependencyDerivator;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID, new GreaterConstraint(0));
      if(config.grab(kP)) {
        k = kP.getValue();
      }
      Class<DependencyDerivator<V, D>> cls = ClassGenericsUtil.uglyCastIntoSubclass(DependencyDerivator.class);
      dependencyDerivator = config.tryInstantiate(cls);
    }

    @Override
    protected COP<V, D> makeInstance() {
      return new COP<V, D>(distanceFunction, k, dependencyDerivator);
    }
  }
}