package experimentalcode.lisa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.OldDescription;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Outlier detection algorithm using a mixture model approach. The data is
 * modeled as a mixture of two distributions, one for ordinary data and one for
 * outliers. At first all Objects are in the set of normal objects and the set
 * of anomalous objects is empty. An iterative procedure then transfers objects
 * from the ordinary set to the anomalous set if the transfer increases the
 * overall likelihood of the data.
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
public class MixtureModelOutlierDetection<V extends NumberVector<V, Double>> extends AbstractAlgorithm<V, OutlierResult> {
  /**
   * The association id to associate the MMOD_OFLAF of an object for the
   * MixtureModelOutlierDetection algorithm.
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
  private final DoubleParameter C_PARAM = new DoubleParameter(C_ID);

  /**
   * Holds the value of {@link #C_PARAM}.
   */
  private double c;

  /**
   * Provides the result of the algorithm.
   */
  OutlierResult result;

  /**
   * Constructor, adding options to option handler.
   */
  public MixtureModelOutlierDetection(Parameterization config) {
    super(config);
    if (config.grab(C_PARAM)) {
      c = C_PARAM.getValue();
    }
    if (config.grab(L_PARAM)) {
      l = L_PARAM.getValue();
    }
  }

  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {

    // set of normal objects (containing all data in the beginning) and a
    List<Integer> normalObjs = database.getIDs();
    // set of anomalous objects(empty in the beginning)
    List<Integer> anomalousObjs = new ArrayList<Integer>();
    // resulting scores
    HashMap<Integer, Double> oscores = new HashMap<Integer, Double>(database.size());
    // compute loglikelihood
    double logLike = database.size() * Math.log(1 - l) + loglikelihoodNormal(normalObjs, database);
    //debugFine("normalsize   " + normalObjs.size() + " anormalsize  " + anomalousObjs.size() + " all " + (anomalousObjs.size() + normalObjs.size()));
    //debugFine(logLike + " loglike beginning" + loglikelihoodNormal(normalObjs, database));
    for(int i = 0; i < normalObjs.size() && normalObjs.size() > 0; i++) {
      //debugFine("i     " + i);
      // move object to anomalousObjs and test if the loglikelyhood increases
      // significantly
      Integer x = normalObjs.get(i);
      anomalousObjs.add(x);
      // FIXME: BUG: doesn't that lead us to skip element i+1, since we always do "i++"?
      // FIXME: these operations on an array list are really expensive.
      normalObjs.remove(i);
      double currentLogLike = normalObjs.size() * Math.log(1 - l) + loglikelihoodNormal(normalObjs, database) + anomalousObjs.size() * Math.log(l) + loglikelihoodAnomalous(anomalousObjs);

      double deltaLog = Math.abs(logLike - currentLogLike);

      // if the loglike increases more than a threshold, object stays in
      // anomalous set and is flagged as outlier
      if(deltaLog > c && currentLogLike > logLike) {
        // flag as outlier
        //debugFine("outlier id" + x);
        oscores.put(x, 1.0);
        logLike = currentLogLike;
        i--;
      }
      else {
        // move object back to normalObjects
        // FIXME: slow/expensive!
        normalObjs.add(i, x);
        //debugFine("nonoutlier id" + x);
        anomalousObjs.remove(anomalousObjs.size() - 1);
        // flag as non outlier
        oscores.put(x, 0.0);
      }
    }

    OutlierScoreMeta meta = new ProbabilisticOutlierScore();

    AnnotationFromHashMap<Double> res1 = new AnnotationFromHashMap<Double>(MMOD_OFLAG, oscores );
    OrderingFromHashMap<Double> res2 = new OrderingFromHashMap<Double>(oscores);
    result = new OutlierResult(meta, res1, res2);
    return result;
  }

  /**
   *loglikelihood anomalous objects. normal distribution
   * 
   * @param anomalousObjs
   * @return
   */
  private double loglikelihoodAnomalous(List<Integer> anomalousObjs) {
    int n = anomalousObjs.size();

    double prob = n * Math.log(Math.pow(1.0 / n, n));

    return prob;
  }

  /**
   * * computes the loglikelihood of all normal objects. gaussian model
   * 
   * @param normalObjs
   * @return
   */
  private double loglikelihoodNormal(List<Integer> normalObjs, Database<V> database) {
    double prob = 0;
    if(normalObjs.isEmpty()) {
      return prob;
    }
    else {
      V mean = DatabaseUtil.centroid(database, normalObjs);

      Matrix covarianceMatrix = DatabaseUtil.covarianceMatrix(database, normalObjs);
      Matrix covInv;

      // test singulaere matrix
      covInv = covarianceMatrix.cheatToAvoidSingularity(SINGULARITY_CHEAT).inverse();

      double covarianceDet = covarianceMatrix.det();
      double fakt = (1.0 / (Math.sqrt(Math.pow(2 * Math.PI, database.dimensionality()) * (covarianceDet))));
      // for each object compute probability and sum
      for(Integer id : normalObjs) {
        V x = database.get(id);
        Vector x_minus_mean = x.minus(mean).getColumnVector();
        double mDist = x_minus_mean.transposeTimes(covInv).times(x_minus_mean).get(0, 0);
        prob += Math.log(fakt * Math.exp(-mDist / 2.0));

      }
      return prob;
    }
  }

  @Override
  public OldDescription getDescription() {
    return new OldDescription("Mixture Model", "Mixture Model Outlier Detection", "sd", "Eskin, Eleazar: Anomaly detection over noisy data using learned probability distributions. +" + "In: Proc. of the Seventeenth International Conference on Machine Learning (ICML-2000).");
  }

  @Override
  public OutlierResult getResult() {
    return result;
  }

}
