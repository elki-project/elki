package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.generic.MaskedDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromDataStore;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
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
public class GaussianUniformMixture<V extends NumberVector<V, ?>> extends AbstractAlgorithm<V, OutlierResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(GaussianUniformMixture.class);
  
  /**
   * The association id to associate the MMOD_OFLAF of an object for the
   * GaussianUniformMixture algorithm.
   */
  public static final AssociationID<Double> MMOD_OFLAG = AssociationID.getOrCreateAssociationID("mmod.oflag", Double.class);

  /**
   * Parameter to specify the fraction of expected outliers.
   */
  public static final OptionID L_ID = OptionID.getOrCreateOptionID("mmo.l", "expected fraction of outliers");

  /**
   * Parameter to specify the cutoff.
   */
  public static final OptionID C_ID = OptionID.getOrCreateOptionID("mmo.c", "cutoff");

  /**
   * Small value to increment diagonally of a matrix in order to avoid
   * singularity before building the inverse.
   */
  private static final double SINGULARITY_CHEAT = 1E-9;

  /**
   * Holds the value of {@link #C_ID}.
   */
  private double c;

  /**
   * log(l) precomputed
   */
  private double logl;

  /**
   * log(1-l) precomputed
   */
  private double logml;

  /**
   * Constructor with parameters.
   * 
   * @param l l value
   * @param c c value
   */
  public GaussianUniformMixture(double l, double c) {
    super();
    this.logl = Math.log(l);
    this.logml = Math.log(1 - l);
    this.c = c;
  }

  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {
    // Use an array list of object IDs for fast random access by an offset
    ArrayDBIDs objids = DBIDUtil.ensureArray(database.getIDs());
    // A bit set to flag objects as anomalous, none at the beginning
    BitSet bits = new BitSet(objids.size());
    // Positive masked collection
    DBIDs normalObjs = new MaskedDBIDs(objids, bits, true);
    // Positive masked collection
    DBIDs anomalousObjs = new MaskedDBIDs(objids, bits, false);
    // resulting scores
    WritableDataStore<Double> oscores = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
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
      DBID curid = objids.get(i);

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

    AnnotationResult<Double> res1 = new AnnotationFromDataStore<Double>(MMOD_OFLAG, oscores);
    OrderingResult res2 = new OrderingFromDataStore<Double>(oscores);
    return new OutlierResult(meta, res1, res2);
  }

  /**
   * Loglikelihood anomalous objects. Uniform distribution
   * 
   * @param anomalousObjs
   * @return loglikelihood for anomalous objects
   */
  private double loglikelihoodAnomalous(DBIDs anomalousObjs) {
    int n = anomalousObjs.size();

    return n * Math.log(1.0 / n);
  }

  /**
   * Computes the loglikelihood of all normal objects. Gaussian model
   * 
   * @param objids Object IDs for 'normal' objects.
   * @param database Database
   * @return loglikelihood for normal objects
   */
  private double loglikelihoodNormal(DBIDs objids, Database<V> database) {
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
    for(DBID id : objids) {
      V x = database.get(id);

      Vector x_minus_mean = x.minus(mean).getColumnVector();
      double mDist = x_minus_mean.transposeTimes(covInv).times(x_minus_mean).get(0, 0);
      prob += Math.log(fakt * Math.exp(-mDist / 2.0));
    }
    return prob;
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return ABOD Algorithm
   */
  public static <V extends NumberVector<V, ?>> GaussianUniformMixture<V> parameterize(Parameterization config) {
    double l = getParameterL(config);
    double c = getParameterC(config);
    if(config.hasErrors()) {
      return null;
    }
    return new GaussianUniformMixture<V>(l, c);
  }

  /**
   * Get the C parameter.
   * 
   * @param config Parameterization
   * @return c parameter
   */
  protected static double getParameterC(Parameterization config) {
    final DoubleParameter param = new DoubleParameter(C_ID, 1E-7);
    if(config.grab(param)) {
      return param.getValue();
    }
    return 1E-7;
  }

  /**
   * Get the L parameter.
   * 
   * @param config Parameterization
   * @return l parameter
   */
  protected static double getParameterL(Parameterization config) {
    final DoubleParameter param = new DoubleParameter(L_ID);
    if(config.grab(param)) {
      return param.getValue();
    }
    return 0;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}