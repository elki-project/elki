package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.normalization.AttributeWiseMinMaxNormalization;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Provides the EM algorithm (clustering by expectation maximization).
 * <p/>
 * Initialization is implemented as random initialization of means (uniformly
 * distributed within the attribute ranges of the given database) and initial
 * zero-covariance and variance=1 in covariance matrices.
 * </p>
 * <p>
 * Reference: A. P. Dempster, N. M. Laird, D. B. Rubin: Maximum Likelihood from
 * Incomplete Data via the EM algorithm. <br>
 * In Journal of the Royal Statistical Society, Series B, 39(1), 1977, pp. 1-31
 * </p>
 * 
 * @author Arthur Zimek
 * @param <V> a type of {@link NumberVector} as a suitable datatype for this
 *        algorithm
 */
@Title("EM-Clustering: Clustering by Expectation Maximization")
@Description("Provides k Gaussian mixtures maximizing the probability of the given data")
@Reference(authors = "A. P. Dempster, N. M. Laird, D. B. Rubin", title = "Maximum Likelihood from Incomplete Data via the EM algorithm", booktitle = "Journal of the Royal Statistical Society, Series B, 39(1), 1977, pp. 1-31", url="http://www.jstor.org/stable/2984875")
public class EM<V extends NumberVector<V, ?>> extends AbstractAlgorithm<V, Clustering<EMModel<V>>> implements ClusteringAlgorithm<Clustering<EMModel<V>>, V> {
  /**
   * Small value to increment diagonally of a matrix in order to avoid
   * singularity before building the inverse.
   */
  private static final double SINGULARITY_CHEAT = 1E-9;

  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("em.k", "The number of clusters to find.");

  /**
   * Parameter to specify the number of clusters to find, must be an integer
   * greater than 0.
   * <p>
   * Key: {@code -em.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0));

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  private int k;

  /**
   * OptionID for {@link #DELTA_PARAM}
   */
  public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID("em.delta", "The termination criterion for maximization of E(M): " + "E(M) - E(M') < em.delta");

  private static final double MIN_LOGLIKELIHOOD = -100000;

  /**
   * Parameter to specify the termination criterion for maximization of E(M):
   * E(M) - E(M') < em.delta, must be a double equal to or greater than 0.
   * <p>
   * Default value: {@code 0.0}
   * </p>
   * <p>
   * Key: {@code -em.delta}
   * </p>
   */
  private final DoubleParameter DELTA_PARAM = new DoubleParameter(DELTA_ID, new GreaterEqualConstraint(0.0), 0.0);

  /**
   * Holds the value of {@link #DELTA_PARAM}.
   */
  private double delta;

  /**
   * Keeps the result.
   */
  private Clustering<EMModel<V>> result;

  /**
   * Store the individual probabilities, for use by EMOutlierDetection etc.
   */
  private HashMap<Integer, double[]> probClusterIGivenX;

  /**
   * Provides the EM algorithm (clustering by expectation maximization), adding
   * parameters {@link #K_PARAM} and {@link #DELTA_PARAM} to the option handler
   * additionally to parameters of super class.
   */
  public EM(Parameterization config) {
    super(config);
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
    if(config.grab(DELTA_PARAM)) {
      delta = DELTA_PARAM.getValue();
    }
  }

  /**
   * Performs the EM clustering algorithm on the given database.
   * <p/>
   * Finally a hard clustering is provided where each clusters gets assigned the
   * points exhibiting the highest probability to belong to this cluster. But
   * still, the database objects hold associated the complete probability-vector
   * for all models.
   */
  @Override
  protected Clustering<EMModel<V>> runInTime(Database<V> database) throws IllegalStateException {
    if(database.size() == 0) {
      throw new IllegalArgumentException("database empty: must contain elements");
    }
    // initial models
    if(logger.isVerbose()) {
      logger.verbose("initializing " + k + " models");
    }
    List<V> means = initialMeans(database);
    List<Matrix> covarianceMatrices = new ArrayList<Matrix>(k);
    List<Double> normDistrFactor = new ArrayList<Double>(k);
    List<Matrix> invCovMatr = new ArrayList<Matrix>(k);
    List<Double> clusterWeights = new ArrayList<Double>(k);
    probClusterIGivenX = new HashMap<Integer, double[]>(database.size());

    int dimensionality = means.get(0).getDimensionality();
    for(int i = 0; i < k; i++) {
      Matrix m = Matrix.identity(dimensionality, dimensionality);
      covarianceMatrices.add(m);
      normDistrFactor.add(1.0 / Math.sqrt(Math.pow(2 * Math.PI, dimensionality) * m.det()));
      invCovMatr.add(m.inverse());
      clusterWeights.add(1.0 / k);
      if(logger.isDebuggingFinest()) {
        StringBuffer msg = new StringBuffer();
        msg.append(" model ").append(i).append(":\n");
        msg.append(" mean:    ").append(means.get(i)).append("\n");
        msg.append(" m:\n").append(m.toString("        ")).append("\n");
        msg.append(" m.det(): ").append(m.det()).append("\n");
        msg.append(" cluster weight: ").append(clusterWeights.get(i)).append("\n");
        msg.append(" normDistFact:   ").append(normDistrFactor.get(i)).append("\n");
        logger.debugFine(msg.toString());
      }
    }
    double emNew = assignProbabilitiesToInstances(database, normDistrFactor, means, invCovMatr, clusterWeights, probClusterIGivenX);

    // iteration unless no change
    if(logger.isVerbose()) {
      logger.verbose("iterating EM");
    }

    double em;
    int it = 0;
    do {
      it++;
      if(logger.isVerbose()) {
        logger.verbose("iteration " + it + " - expectation value: " + emNew);
      }
      em = emNew;

      // recompute models
      List<V> meanSums = new ArrayList<V>(k);
      double[] sumOfClusterProbabilities = new double[k];

      for(int i = 0; i < k; i++) {
        clusterWeights.set(i, 0.0);
        meanSums.add(means.get(i).nullVector());
        covarianceMatrices.set(i, Matrix.zeroMatrix(dimensionality));
      }

      // weights and means
      for(Integer id : database) {
        double[] clusterProbabilities = probClusterIGivenX.get(id);

        for(int i = 0; i < k; i++) {
          sumOfClusterProbabilities[i] += clusterProbabilities[i];
          V summand = database.get(id).multiplicate(clusterProbabilities[i]);
          V currentMeanSum = meanSums.get(i).plus(summand);
          meanSums.set(i, currentMeanSum);
        }
      }
      int n = database.size();
      for(int i = 0; i < k; i++) {
        clusterWeights.set(i, sumOfClusterProbabilities[i] / n);
        V newMean = meanSums.get(i).multiplicate(1 / sumOfClusterProbabilities[i]);
        means.set(i, newMean);
      }
      // covariance matrices
      for(Integer id : database) {
        double[] clusterProbabilities = probClusterIGivenX.get(id);
        V instance = database.get(id);
        for(int i = 0; i < k; i++) {
          V difference = instance.minus(means.get(i));
          Matrix newCovMatr = covarianceMatrices.get(i).plus(difference.getColumnVector().times(difference.getRowVector()).times(clusterProbabilities[i]));
          covarianceMatrices.set(i, newCovMatr);
        }
      }
      for(int i = 0; i < k; i++) {
        covarianceMatrices.set(i, covarianceMatrices.get(i).times(1 / sumOfClusterProbabilities[i]).cheatToAvoidSingularity(SINGULARITY_CHEAT));
      }
      for(int i = 0; i < k; i++) {
        normDistrFactor.set(i, 1.0 / Math.sqrt(Math.pow(2 * Math.PI, dimensionality) * covarianceMatrices.get(i).det()));
        invCovMatr.set(i, covarianceMatrices.get(i).inverse());
      }
      // reassign probabilities
      emNew = assignProbabilitiesToInstances(database, normDistrFactor, means, invCovMatr, clusterWeights, probClusterIGivenX);
    }
    while(Math.abs(em - emNew) > delta);

    if(logger.isVerbose()) {
      logger.verbose("assigning clusters");
    }

    // fill result with clusters and models
    List<List<Integer>> hardClusters = new ArrayList<List<Integer>>(k);
    for(int i = 0; i < k; i++) {
      hardClusters.add(new LinkedList<Integer>());
    }

    // provide a hard clustering
    for(Integer id : database) {
      double[] clusterProbabilities = probClusterIGivenX.get(id);
      int maxIndex = 0;
      double currentMax = 0.0;
      for(int i = 0; i < k; i++) {
        if(clusterProbabilities[i] > currentMax) {
          maxIndex = i;
          currentMax = clusterProbabilities[i];
        }
      }
      hardClusters.get(maxIndex).add(id);
    }
    Integer[][] resultClusters = new Integer[k][];
    for(int i = 0; i < k; i++) {
      resultClusters[i] = hardClusters.get(i).toArray(new Integer[hardClusters.get(i).size()]);
    }
    result = new Clustering<EMModel<V>>();
    // provide models within the result
    for(int i = 0; i < k; i++) {
      // TODO: re-do labeling.
      // SimpleClassLabel label = new SimpleClassLabel();
      // label.init(result.canonicalClusterLabel(i));
      DatabaseObjectGroup group = new DatabaseObjectGroupCollection<List<Integer>>(hardClusters.get(i));
      Cluster<EMModel<V>> model = new Cluster<EMModel<V>>(group, new EMModel<V>(means.get(i), covarianceMatrices.get(i)));
      result.addCluster(model);
    }
    return result;
  }

  /**
   * Assigns the current probability values to the instances in the database and
   * compute the expectation value of the current mixture of distributions.
   * 
   * Computed as the sum of the logarithms of the prior probability of each
   * instance.
   * 
   * @param database the database used for assignment to instances
   * @param normDistrFactor normalization factor for density function, based on
   *        current covariance matrix
   * @param means the current means
   * @param invCovMatr the inverse covariance matrices
   * @param clusterWeights the weights of the current clusters
   * @return the expectation value of the current mixture of distributions
   */
  protected double assignProbabilitiesToInstances(Database<V> database, List<Double> normDistrFactor, List<V> means, List<Matrix> invCovMatr, List<Double> clusterWeights, HashMap<Integer, double[]> probClusterIGivenX) {
    double emSum = 0.0;

    for(Integer id : database) {
      V x = database.get(id);
      List<Double> probabilities = new ArrayList<Double>(k);
      for(int i = 0; i < k; i++) {
        V difference = x.minus(means.get(i));
        Matrix differenceRow = difference.getRowVector();
        Matrix differenceCol = difference.getColumnVector();
        Matrix rowTimesCov = differenceRow.times(invCovMatr.get(i));
        Matrix rowTimesCovTimesCol = rowTimesCov.times(differenceCol);
        double power = rowTimesCovTimesCol.get(0, 0) / 2.0;
        double prob = normDistrFactor.get(i) * Math.exp(-power);
        if(logger.isDebuggingFinest()) {
          logger.debugFinest(" difference vector= ( " + difference.toString() + " )\n" + " differenceRow:\n" + differenceRow.toString("    ") + "\n" + " differenceCol:\n" + differenceCol.toString("    ") + "\n" + " rowTimesCov:\n" + rowTimesCov.toString("    ") + "\n" + " rowTimesCovTimesCol:\n" + rowTimesCovTimesCol.toString("    ") + "\n" + " power= " + power + "\n" + " prob=" + prob + "\n" + " inv cov matrix: \n" + invCovMatr.get(i).toString("     "));
        }

        probabilities.add(prob);
      }
      double priorProbability = 0.0;
      for(int i = 0; i < k; i++) {
        priorProbability += probabilities.get(i) * clusterWeights.get(i);
      }
      double logP = Math.max(Math.log(priorProbability), MIN_LOGLIKELIHOOD);
      if(!Double.isNaN(logP)) {
        emSum += logP;
      }

      double[] clusterProbabilities = new double[k];
      for(int i = 0; i < k; i++) {
        assert (priorProbability >= 0.0);
        assert (clusterWeights.get(i) >= 0.0);
        // do not divide by zero!
        if(priorProbability == 0.0) {
          clusterProbabilities[i] = 0.0;
        }
        else {
          clusterProbabilities[i] = probabilities.get(i) / priorProbability * clusterWeights.get(i);
        }
      }
      probClusterIGivenX.put(id, clusterProbabilities);
    }

    return emSum;
  }

  /**
   * Creates {@link #k k} random points distributed uniformly within the
   * attribute ranges of the given database.
   * 
   * @param database the database must contain enough points in order to
   *        ascertain the range of attribute values. Less than two points would
   *        make no sense. The content of the database is not touched otherwise.
   * @return a list of {@link #k k} random points distributed uniformly within
   *         the attribute ranges of the given database
   */
  protected List<V> initialMeans(Database<V> database) {
    Random random = new Random();
    if(database.size() > 0) {
      // needs normalization to ensure the randomly generated means
      // are in the same range as the vectors in the database
      // XXX perhaps this can be done more conveniently?
      V randomBase = database.get(database.iterator().next());
      EmptyParameterization parameters = new EmptyParameterization();
      AttributeWiseMinMaxNormalization<V> normalization = new AttributeWiseMinMaxNormalization<V>(parameters);
      for(ParameterException e : parameters.getErrors()) {
        logger.warning("Error in internal parameterization: " + e.getMessage());
      }
      List<V> list = new ArrayList<V>(database.size());
      for(Integer id : database) {
        list.add(database.get(id));
      }
      try {
        normalization.normalize(list);
      }
      catch(NonNumericFeaturesException e) {
        logger.warning(e.getMessage());
      }
      List<V> means = new ArrayList<V>(k);
      if(logger.isVerbose()) {
        logger.verbose("initializing random vectors");
      }
      for(int i = 0; i < k; i++) {
        V randomVector = randomBase.randomInstance(random);
        try {
          means.add(normalization.restore(randomVector));
        }
        catch(NonNumericFeaturesException e) {
          logger.warning(e.getMessage());
          means.add(randomVector);
        }
      }
      return means;
    }
    else {
      return new ArrayList<V>(0);
    }
  }

  public Clustering<EMModel<V>> getResult() {
    return this.result;
  }

  /**
   * Get the probabilities for a given point.
   * 
   * @param index
   * @return Probabilities of given point
   */
  public double[] getProbClusterIGivenX(Integer index) {
    return probClusterIGivenX.get(index);
  }
}
