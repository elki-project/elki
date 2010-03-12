package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Outlier detection algorithm using a mixture model approach. The data is
 * modeled as a mixture of two distributions, a Gaussian distribution for
 * ordinary data and a uniform distribution for outliers. At first all Objects
 * are in the set of normal objects and the set of anomalous objects is empty.
 * An iterative procedure then transfers objects from the ordinary set to the
 * anomalous set if the transfer increases the overall likelihood of the data.
 * 
 * <p>
 * Reference:<br>
 * Eskin, Eleazar: Anomaly detection over noisy data using learned probability
 * distributions. In Proc. of the Seventeenth International Conference on
 * Machine Learning (ICML-2000).
 * </p>
 * 
 * @author Lisa Reichert
 * 
 * @param <V> Vector Type
 */
@Title("Gaussian-Uniform Mixture Model Outlier Detection")
@Description("Fits a mixture model consisting of a Gaussian and a uniform distribution to the data.")
@Reference(prefix = "Generalization using the likelihood gain as outlier score of", authors = "Eskin, Eleazar", title = "Anomaly detection over noisy data using learned probability distributions", booktitle = "Proc. of the Seventeenth International Conference on Machine Learning (ICML-2000)")
public class GaussianUniformMixture<V extends NumberVector<V, Double>> extends AbstractAlgorithm<V, OutlierResult> {
  /**
   * The association id to associate the MMOD_OFLAF of an object for the
   * GaussianUniformMixture algorithm.
   */
  public static final AssociationID<Double> MMOD_OFLAG = AssociationID.getOrCreateAssociationID("mmod.oflag", Double.class);

  /**
   * OptionID for {@link #L_PARAM}
   */
  public static final OptionID L_ID = OptionID.getOrCreateOptionID("mmo.l", "expected fraction of outliers");

  /**
   * OptionID for {@link #C_PARAM}
   */
  public static final OptionID C_ID = OptionID.getOrCreateOptionID("mmo.c", "cutoff");

  /**
   * Small value to increment diagonally of a matrix in order to avoid
   * singularity before building the inverse.
   */
  private static final double SINGULARITY_CHEAT = 1E-9;

  /**
   * Parameter to specify the fraction of expected outliers,
   * 
   * <p>
   * Key: {@code -mmo.l}
   * </p>
   */
  private final DoubleParameter L_PARAM = new DoubleParameter(L_ID);

  /**
   * Holds the value of {@link #L_PARAM}.
   */
  private double l;

  /**
   * Parameter to specify the cutoff,
   * 
   * <p>
   * Key: {@code -mmo.c}
   * </p>
   */
  private final DoubleParameter C_PARAM = new DoubleParameter(C_ID, 1E-7);

  /**
   * Holds the value of {@link #C_PARAM}.
   */
  private double c;

  /**
   * Provides the result of the algorithm.
   */
  private OutlierResult result;

  /**
   * log(l) precomputed
   */
  private double logl;

  /**
   * log(1-l) precomputed
   */
  private double logml;

  /**
   * Constructor, adding options to option handler.
   */
  public GaussianUniformMixture(Parameterization config) {
    super(config);
    if(config.grab(L_PARAM)) {
      l = L_PARAM.getValue();
      logl = Math.log(l);
      logml = Math.log(1 - l);
    }
    if(config.grab(C_PARAM)) {
      c = C_PARAM.getValue();
    }
  }

  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {
    // Use an array list of object IDs for fast random access by ID
    ArrayList<Integer> objids = new ArrayList<Integer>(database.getIDs());
    // A bit set to flag objects as anomalous, none at the beginning
    BitSet bits = new BitSet(objids.size());
    // Positive masked collection
    Collection<Integer> normalObjs = new Util.MaskedArrayList<Integer>(objids, bits, true);
    // Positive masked collection
    Collection<Integer> anomalousObjs = new Util.MaskedArrayList<Integer>(objids, bits, false);
    // resulting scores
    HashMap<Integer, Double> oscores = new HashMap<Integer, Double>(database.size());
    // compute loglikelihood
    double logLike = database.size() * logml + loglikelihoodNormal(normalObjs, database);
    // logger.debugFine("normalsize   " + normalObjs.size() + " anormalsize  " +
    // anomalousObjs.size() + " all " + (anomalousObjs.size() +
    // normalObjs.size()));
    // logger.debugFine(logLike + " loglike beginning" +
    // loglikelihoodNormal(normalObjs, database));
    MinMax<Double> minmax = new MinMax<Double>();
    for(int i = 0; i < objids.size(); i++) {
      // logger.debugFine("i     " + i);
      // Change mask to make the current object anomalous
      bits.set(i);
      // Compute new likelihoods
      double currentLogLike = normalObjs.size() * logml + loglikelihoodNormal(normalObjs, database) + anomalousObjs.size() * logl + loglikelihoodAnomalous(anomalousObjs);

      // Get the actual object id
      int curid = objids.get(i);

      // if the loglike increases more than a threshold, object stays in
      // anomalous set and is flagged as outlier
      final double loglikeGain = currentLogLike - logLike;
      oscores.put(curid, loglikeGain);
      minmax.put(loglikeGain);

      if(loglikeGain > c) {
        // flag as outlier
        // logger.debugFine("Outlier: " + curid + " " + (currentLogLike -
        // logLike));
        // Update best logLike
        logLike = currentLogLike;
      }
      else {
        // logger.debugFine("Inlier: " + curid + " " + (currentLogLike -
        // logLike));
        // undo bit set
        bits.clear(i);
      }
    }

    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0);

    AnnotationFromHashMap<Double> res1 = new AnnotationFromHashMap<Double>(MMOD_OFLAG, oscores);
    OrderingFromHashMap<Double> res2 = new OrderingFromHashMap<Double>(oscores);
    result = new OutlierResult(meta, res1, res2);
    return result;
  }

  /**
   * Loglikelihood anomalous objects. Uniform distribution
   * 
   * @param anomalousObjs
   * @return loglikelihood for anomalous objects
   */
  private double loglikelihoodAnomalous(Collection<Integer> anomalousObjs) {
    int n = anomalousObjs.size();

    return n * Math.log(1.0 / n);
  }

  /**
   * Computes the loglikelihood of all normal objects. Gaussian model
   * 
   * @param normalObjs
   * @param database Database
   * @return loglikelihood for normal objects
   */
  private double loglikelihoodNormal(Collection<Integer> objids, Database<V> database) {
    if(objids.isEmpty()) {
      return 0;
    }
    double prob = 0;
    V mean = DatabaseUtil.centroid(database, objids);
    Matrix covarianceMatrix = DatabaseUtil.covarianceMatrix(database, objids);

    // test singulaere matrix
    Matrix covInv = covarianceMatrix.cheatToAvoidSingularity(SINGULARITY_CHEAT).inverse();

    double covarianceDet = covarianceMatrix.det();
    double fakt = 1.0 / Math.sqrt(Math.pow(2 * Math.PI, database.dimensionality()) * covarianceDet);
    // for each object compute probability and sum
    for(Integer id : objids) {
      V x = database.get(id);

      Vector x_minus_mean = x.minus(mean).getColumnVector();
      double mDist = x_minus_mean.transposeTimes(covInv).times(x_minus_mean).get(0, 0);
      prob += Math.log(fakt * Math.exp(-mDist / 2.0));
    }
    return prob;
  }

  @Override
  public OutlierResult getResult() {
    return result;
  }
}