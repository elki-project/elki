package de.lmu.ifi.dbs.elki.algorithm.clustering;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.RandomlyGeneratedInitialMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
 * 
 * @apiviz.has EMModel
 * 
 * @param <V> a type of {@link NumberVector} as a suitable datatype for this
 *        algorithm
 */
@Title("EM-Clustering: Clustering by Expectation Maximization")
@Description("Provides k Gaussian mixtures maximizing the probability of the given data")
@Reference(authors = "A. P. Dempster, N. M. Laird, D. B. Rubin", title = "Maximum Likelihood from Incomplete Data via the EM algorithm", booktitle = "Journal of the Royal Statistical Society, Series B, 39(1), 1977, pp. 1-31", url = "http://www.jstor.org/stable/2984875")
public class EM<V extends NumberVector<V, ?>> extends AbstractAlgorithm<Clustering<EMModel<V>>> implements ClusteringAlgorithm<Clustering<EMModel<V>>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(EM.class);

  /**
   * Small value to increment diagonally of a matrix in order to avoid
   * singularity before building the inverse.
   */
  private static final double SINGULARITY_CHEAT = 1E-9;

  /**
   * Parameter to specify the number of clusters to find, must be an integer
   * greater than 0.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("em.k", "The number of clusters to find.");

  /**
   * Holds the value of {@link #K_ID}.
   */
  private int k;

  /**
   * Parameter to specify the termination criterion for maximization of E(M):
   * E(M) - E(M') < em.delta, must be a double equal to or greater than 0.
   */
  public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID("em.delta", "The termination criterion for maximization of E(M): " + "E(M) - E(M') < em.delta");

  /**
   * Parameter to specify the initialization method
   */
  public static final OptionID INIT_ID = OptionID.getOrCreateOptionID("kmeans.initialization", "Method to choose the initial means.");

  private static final double MIN_LOGLIKELIHOOD = -100000;

  /**
   * Holds the value of {@link #DELTA_ID}.
   */
  private double delta;

  /**
   * Store the individual probabilities, for use by EMOutlierDetection etc.
   */
  private WritableDataStore<double[]> probClusterIGivenX;

  /**
   * Class to choose the initial means
   */
  private KMeansInitialization<V> initializer;

  /**
   * Constructor.
   * 
   * @param k k parameter
   * @param delta delta parameter
   * @param initializer Class to choose the initial means
   */
  public EM(int k, double delta, KMeansInitialization<V> initializer) {
    super();
    this.k = k;
    this.delta = delta;
    this.initializer = initializer;
  }

  /**
   * Performs the EM clustering algorithm on the given database.
   * <p/>
   * Finally a hard clustering is provided where each clusters gets assigned the
   * points exhibiting the highest probability to belong to this cluster. But
   * still, the database objects hold associated the complete probability-vector
   * for all models.
   * 
   * @param database Database
   * @param relation Relation
   * @return Result
   */
  public Clustering<EMModel<V>> run(Database database, Relation<V> relation) {
    if(relation.size() == 0) {
      throw new IllegalArgumentException("database empty: must contain elements");
    }
    // initial models
    if(logger.isVerbose()) {
      logger.verbose("initializing " + k + " models");
    }
    List<Vector> means = initializer.chooseInitialMeans(relation, k, EuclideanDistanceFunction.STATIC);
    List<Matrix> covarianceMatrices = new ArrayList<Matrix>(k);
    List<Double> normDistrFactor = new ArrayList<Double>(k);
    List<Matrix> invCovMatr = new ArrayList<Matrix>(k);
    List<Double> clusterWeights = new ArrayList<Double>(k);
    probClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);

    final int dimensionality = means.get(0).getDimensionality();
    for(int i = 0; i < k; i++) {
      Matrix m = Matrix.identity(dimensionality, dimensionality);
      covarianceMatrices.add(m);
      normDistrFactor.add(1.0 / Math.sqrt(Math.pow(MathUtil.TWOPI, dimensionality) * m.det()));
      invCovMatr.add(m.inverse());
      clusterWeights.add(1.0 / k);
      if(logger.isDebuggingFinest()) {
        StringBuffer msg = new StringBuffer();
        msg.append(" model ").append(i).append(":\n");
        msg.append(" mean:    ").append(means.get(i)).append("\n");
        msg.append(" m:\n").append(FormatUtil.format(m, "        ")).append("\n");
        msg.append(" m.det(): ").append(m.det()).append("\n");
        msg.append(" cluster weight: ").append(clusterWeights.get(i)).append("\n");
        msg.append(" normDistFact:   ").append(normDistrFactor.get(i)).append("\n");
        logger.debugFine(msg.toString());
      }
    }
    double emNew = assignProbabilitiesToInstances(relation, normDistrFactor, means, invCovMatr, clusterWeights, probClusterIGivenX);

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
      List<Vector> meanSums = new ArrayList<Vector>(k);
      double[] sumOfClusterProbabilities = new double[k];

      for(int i = 0; i < k; i++) {
        clusterWeights.set(i, 0.0);
        meanSums.add(new Vector(dimensionality));
        covarianceMatrices.set(i, Matrix.zeroMatrix(dimensionality));
      }

      // weights and means
      for(DBID id : relation.iterDBIDs()) {
        double[] clusterProbabilities = probClusterIGivenX.get(id);

        for(int i = 0; i < k; i++) {
          sumOfClusterProbabilities[i] += clusterProbabilities[i];
          Vector summand = relation.get(id).getColumnVector().timesEquals(clusterProbabilities[i]);
          meanSums.get(i).plusEquals(summand);
        }
      }
      final int n = relation.size();
      for(int i = 0; i < k; i++) {
        clusterWeights.set(i, sumOfClusterProbabilities[i] / n);
        Vector newMean = meanSums.get(i).timesEquals(1 / sumOfClusterProbabilities[i]);
        means.set(i, newMean);
      }
      // covariance matrices
      for(DBID id : relation.iterDBIDs()) {
        double[] clusterProbabilities = probClusterIGivenX.get(id);
        Vector instance = relation.get(id).getColumnVector();
        for(int i = 0; i < k; i++) {
          Vector difference = instance.minus(means.get(i));
          covarianceMatrices.get(i).plusEquals(difference.timesTranspose(difference).timesEquals(clusterProbabilities[i]));
        }
      }
      for(int i = 0; i < k; i++) {
        covarianceMatrices.set(i, covarianceMatrices.get(i).times(1 / sumOfClusterProbabilities[i]).cheatToAvoidSingularity(SINGULARITY_CHEAT));
      }
      for(int i = 0; i < k; i++) {
        normDistrFactor.set(i, 1.0 / Math.sqrt(Math.pow(MathUtil.TWOPI, dimensionality) * covarianceMatrices.get(i).det()));
        invCovMatr.set(i, covarianceMatrices.get(i).inverse());
      }
      // reassign probabilities
      emNew = assignProbabilitiesToInstances(relation, normDistrFactor, means, invCovMatr, clusterWeights, probClusterIGivenX);
    }
    while(Math.abs(em - emNew) > delta);

    if(logger.isVerbose()) {
      logger.verbose("assigning clusters");
    }

    // fill result with clusters and models
    List<ModifiableDBIDs> hardClusters = new ArrayList<ModifiableDBIDs>(k);
    for(int i = 0; i < k; i++) {
      hardClusters.add(DBIDUtil.newHashSet());
    }

    // provide a hard clustering
    for(DBID id : relation.iterDBIDs()) {
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
    final V factory = DatabaseUtil.assumeVectorField(relation).getFactory();
    Clustering<EMModel<V>> result = new Clustering<EMModel<V>>("EM Clustering", "em-clustering");
    // provide models within the result
    for(int i = 0; i < k; i++) {
      // TODO: re-do labeling.
      // SimpleClassLabel label = new SimpleClassLabel();
      // label.init(result.canonicalClusterLabel(i));
      Cluster<EMModel<V>> model = new Cluster<EMModel<V>>(hardClusters.get(i), new EMModel<V>(factory.newNumberVector(means.get(i).getArrayRef()), covarianceMatrices.get(i)));
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
  protected double assignProbabilitiesToInstances(Relation<V> database, List<Double> normDistrFactor, List<Vector> means, List<Matrix> invCovMatr, List<Double> clusterWeights, WritableDataStore<double[]> probClusterIGivenX) {
    double emSum = 0.0;

    for(DBID id : database.iterDBIDs()) {
      Vector x = database.get(id).getColumnVector();
      List<Double> probabilities = new ArrayList<Double>(k);
      for(int i = 0; i < k; i++) {
        Vector difference = x.minus(means.get(i));
        Vector rowTimesCovTimesCol = difference.transposeTimes(invCovMatr.get(i)).times(difference);
        double power = rowTimesCovTimesCol.get(0) / 2.0;
        double prob = normDistrFactor.get(i) * Math.exp(-power);
        if(logger.isDebuggingFinest()) {
          logger.debugFinest(" difference vector= ( " + difference.toString() + " )\n" + " difference:\n" + FormatUtil.format(difference, "    ") + "\n" + " rowTimesCovTimesCol:\n" + FormatUtil.format(rowTimesCovTimesCol, "    ") + "\n" + " power= " + power + "\n" + " prob=" + prob + "\n" + " inv cov matrix: \n" + FormatUtil.format(invCovMatr.get(i), "     "));
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
   * Get the probabilities for a given point.
   * 
   * @param index Point ID
   * @return Probabilities of given point
   */
  public double[] getProbClusterIGivenX(DBID index) {
    return probClusterIGivenX.get(index);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
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
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    protected int k;

    protected double delta;

    protected KMeansInitialization<V> initializer;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID, new GreaterConstraint(0));
      if(config.grab(kP)) {
        k = kP.getValue();
      }

      ObjectParameter<KMeansInitialization<V>> initialP = new ObjectParameter<KMeansInitialization<V>>(INIT_ID, KMeansInitialization.class, RandomlyGeneratedInitialMeans.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }

      DoubleParameter deltaP = new DoubleParameter(DELTA_ID, new GreaterEqualConstraint(0.0), 0.0);
      if(config.grab(deltaP)) {
        delta = deltaP.getValue();
      }
    }

    @Override
    protected EM<V> makeInstance() {
      return new EM<V>(k, delta, initializer);
    }
  }
}