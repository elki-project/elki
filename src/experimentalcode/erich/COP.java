package experimentalcode.erich;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.ErrorFunctions;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
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
public class COP<V extends NumberVector<V, ?>, D extends NumberDistance<D, ?>> extends DistanceBasedAlgorithm<V, D, MultiResult> {
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("cop.k", "The number of nearest neighbors of an object to be considered for computing its COP_SCORE.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its COP_SCORE, must be an integer greater than 0.
   * <p/>
   * Key: {@code -cop.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0));

  /**
   * Number of neighbors to be considered.
   */
  int k;

  /**
   * Holds the object performing the dependency derivation
   */
  private DependencyDerivator<V, D> dependencyDerivator;

  /**
   * Provides the result of the algorithm.
   */
  MultiResult result;

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
   * Sets minimum points to the optionhandler additionally to the parameters
   * provided by super-classes.
   */
  public COP(Parameterization config) {
    super(config);
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
    dependencyDerivator = new DependencyDerivator<V, D>(config);
  }

  @Override
  protected MultiResult runInTime(Database<V> database) throws IllegalStateException {
    getDistanceFunction().setDatabase(database);

    HashMap<Integer, Double> cop_score = new HashMap<Integer, Double>(database.size());
    HashMap<Integer, Vector> cop_err_v = new HashMap<Integer, Vector>(database.size());
    HashMap<Integer, Matrix> cop_datav = new HashMap<Integer, Matrix>(database.size());
    HashMap<Integer, Integer> cop_dim = new HashMap<Integer, Integer>(database.size());
    HashMap<Integer, CorrelationAnalysisSolution<?>> cop_sol = new HashMap<Integer, CorrelationAnalysisSolution<?>>(database.size());
    {// compute neighbors of each db object
      FiniteProgress progressLocalPCA = logger.isVerbose() ? new FiniteProgress("Correlation Outlier Probabilities", database.size(), logger) : null;
      double sqrt2 = Math.sqrt(2.0);
      for(Integer id : database) {
        List<DistanceResultPair<D>> neighbors = database.kNNQueryForID(id, k + 1, getDistanceFunction());
        neighbors.remove(0);

        List<Integer> ids = new ArrayList<Integer>(neighbors.size());
        for(DistanceResultPair<D> n : neighbors) {
          ids.add(n.getID());
        }

        // TODO: do we want to use the query point as centroid?
        CorrelationAnalysisSolution<V> depsol = dependencyDerivator.generateModel(database, ids);

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
        double distance = depsol.distance(database.get(id));
        double prob = ErrorFunctions.erf(distance / (stddev * sqrt2));

        cop_score.put(id, prob);

        Vector errv = depsol.errorVector(database.get(id));
        cop_err_v.put(id, errv);

        Matrix datav = depsol.dataProjections(database.get(id));
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
    AnnotationResult<Double> scoreResult = new AnnotationFromHashMap<Double>(COP_SCORE, cop_score);
    OrderingResult orderingResult = new OrderingFromHashMap<Double>(cop_score, true);
    OutlierScoreMeta scoreMeta = new ProbabilisticOutlierScore();
    result = new OutlierResult(scoreMeta, scoreResult, orderingResult);
    // extra results
    result.addResult(new AnnotationFromHashMap<Integer>(COP_DIM, cop_dim));
    result.addResult(new AnnotationFromHashMap<Vector>(COP_ERROR_VECTOR, cop_err_v));
    result.addResult(new AnnotationFromHashMap<Matrix>(COP_DATA_VECTORS, cop_datav));
    result.addResult(new AnnotationFromHashMap<CorrelationAnalysisSolution<?>>(COP_SOL, cop_sol));
    return result;
  }

  public MultiResult getResult() {
    return result;
  }
}
