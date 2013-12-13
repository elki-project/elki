package de.lmu.ifi.dbs.elki.algorithm.clustering;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.RandomlyGeneratedInitialMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
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
 * Reference: A. P. Dempster, N. M. Laird, D. B. Rubin:<br />
 * Maximum Likelihood from Incomplete Data via the EM algorithm.<br>
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
public class EM<V extends NumberVector<?>> extends AbstractAlgorithm<Clustering<EMModel<V>>> implements ClusteringAlgorithm<Clustering<EMModel<V>>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(EM.class);

  /**
   * Small value to increment diagonally of a matrix in order to avoid
   * singularity before building the inverse.
   */
  private static final double SINGULARITY_CHEAT = 1E-9;

  /**
   * Number of clusters
   */
  private int k;

  /**
   * Delta parameter
   */
  private double delta;

  /**
   * Class to choose the initial means
   */
  private KMeansInitialization<V> initializer;

  /**
   * Maximum number of iterations to allow
   */
  private int maxiter;

  /**
   * Retain soft assignments.
   */
  private boolean soft;

  private static final double MIN_LOGLIKELIHOOD = -100000;

  /**
   * Soft assignment result type.
   */
  public static final SimpleTypeInformation<double[]> SOFT_TYPE = new SimpleTypeInformation<>(double[].class);

  /**
   * Constructor.
   * 
   * @param k k parameter
   * @param delta delta parameter
   * @param initializer Class to choose the initial means
   * @param maxiter Maximum number of iterations
   * @param soft Include soft assignments
   */
  public EM(int k, double delta, KMeansInitialization<V> initializer, int maxiter, boolean soft) {
    super();
    this.k = k;
    this.delta = delta;
    this.initializer = initializer;
    this.maxiter = maxiter;
    this.setSoft(soft);
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
    if(LOG.isVerbose()) {
      LOG.verbose("initializing " + k + " models");
    }
    final List<V> initialMeans = initializer.chooseInitialMeans(database, relation, k, EuclideanDistanceFunction.STATIC);
    assert (initialMeans.size() == k);
    Vector[] means = new Vector[k];
    {
      int i = 0;
      for(NumberVector<?> nv : initialMeans) {
        means[i] = nv.getColumnVector();
        i++;
      }
    }
    Matrix[] covarianceMatrices = new Matrix[k];
    double[] normDistrFactor = new double[k];
    Matrix[] invCovMatr = new Matrix[k];
    double[] clusterWeights = new double[k];
    WritableDataStore<double[]> probClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);

    final int dimensionality = means[0].getDimensionality();
    final double norm = MathUtil.powi(MathUtil.TWOPI, dimensionality);
    for(int i = 0; i < k; i++) {
      Matrix m = Matrix.identity(dimensionality, dimensionality);
      covarianceMatrices[i] = m;
      normDistrFactor[i] = 1.0 / Math.sqrt(norm);
      invCovMatr[i] = Matrix.identity(dimensionality, dimensionality);
      clusterWeights[i] = 1.0 / k;
    }
    double emNew = assignProbabilitiesToInstances(relation, normDistrFactor, means, invCovMatr, clusterWeights, probClusterIGivenX);

    // iteration unless no change
    if(LOG.isVerbose()) {
      LOG.verbose("iterating EM");
    }
    if(LOG.isVerbose()) {
      LOG.verbose("iteration " + 0 + " - expectation value: " + emNew);
    }

    for(int it = 1; it <= maxiter || maxiter < 0; it++) {
      final double emOld = emNew;
      recomputeCovarianceMatrices(relation, probClusterIGivenX, means, covarianceMatrices, dimensionality);
      computeInverseMatrixes(covarianceMatrices, invCovMatr, normDistrFactor, norm);
      // reassign probabilities
      emNew = assignProbabilitiesToInstances(relation, normDistrFactor, means, invCovMatr, clusterWeights, probClusterIGivenX);

      if(LOG.isVerbose()) {
        LOG.verbose("iteration " + it + " - expectation value: " + emNew);
      }
      if(Math.abs(emOld - emNew) <= delta) {
        break;
      }
    }

    if(LOG.isVerbose()) {
      LOG.verbose("assigning clusters");
    }

    // fill result with clusters and models
    List<ModifiableDBIDs> hardClusters = new ArrayList<>(k);
    for(int i = 0; i < k; i++) {
      hardClusters.add(DBIDUtil.newHashSet());
    }

    // provide a hard clustering
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double[] clusterProbabilities = probClusterIGivenX.get(iditer);
      int maxIndex = 0;
      double currentMax = 0.0;
      for(int i = 0; i < k; i++) {
        if(clusterProbabilities[i] > currentMax) {
          maxIndex = i;
          currentMax = clusterProbabilities[i];
        }
      }
      hardClusters.get(maxIndex).add(iditer);
    }
    final NumberVector.Factory<V, ?> factory = RelationUtil.getNumberVectorFactory(relation);
    Clustering<EMModel<V>> result = new Clustering<>("EM Clustering", "em-clustering");
    // provide models within the result
    for(int i = 0; i < k; i++) {
      // TODO: re-do labeling.
      // SimpleClassLabel label = new SimpleClassLabel();
      // label.init(result.canonicalClusterLabel(i));
      Cluster<EMModel<V>> model = new Cluster<>(hardClusters.get(i), new EMModel<>(factory.newNumberVector(means[i].getArrayRef()), covarianceMatrices[i]));
      result.addToplevelCluster(model);
    }
    if(isSoft()) {
      result.addChildResult(new MaterializedRelation<>("cluster assignments", "em-soft-score", SOFT_TYPE, probClusterIGivenX, relation.getDBIDs()));
    }
    else {
      probClusterIGivenX.destroy();
    }
    return result;
  }

  /**
   * Compute the inverse cluster matrices.
   * 
   * @param covarianceMatrices Input covariance matrices
   * @param invCovMatr Output array for inverse matrices
   * @param normDistrFactor Output array for norm distribution factors.
   * @param norm Normalization factor, usually (2pi)^d
   */
  public static void computeInverseMatrixes(Matrix[] covarianceMatrices, Matrix[] invCovMatr, double[] normDistrFactor, final double norm) {
    int k = covarianceMatrices.length;
    for(int i = 0; i < k; i++) {
      final double det = covarianceMatrices[i].det();
      if(det > 0.) {
        normDistrFactor[i] = 1. / Math.sqrt(norm * det);
      }
      else {
        LOG.warning("Encountered matrix with 0 determinant - degenerated.");
        normDistrFactor[i] = 1.; // Not really well defined
      }
      invCovMatr[i] = covarianceMatrices[i].inverse();
    }
  }

  /**
   * Recompute the covariance matrixes.
   * 
   * @param relation Vector data
   * @param probClusterIGivenX Object probabilities
   * @param means Cluster means output
   * @param covarianceMatrices Output covariance matrixes
   * @param dimensionality Data set dimensionality
   */
  public static void recomputeCovarianceMatrices(Relation<? extends NumberVector<?>> relation, WritableDataStore<double[]> probClusterIGivenX, Vector[] means, Matrix[] covarianceMatrices, final int dimensionality) {
    final int k = means.length;
    CovarianceMatrix[] cms = new CovarianceMatrix[k];
    for(int i = 0; i < k; i++) {
      cms[i] = new CovarianceMatrix(dimensionality);
    }
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double[] clusterProbabilities = probClusterIGivenX.get(iditer);
      Vector instance = relation.get(iditer).getColumnVector();
      for(int i = 0; i < k; i++) {
        if(clusterProbabilities[i] > 0.) {
          cms[i].put(instance, clusterProbabilities[i]);
        }
      }
    }
    for(int i = 0; i < k; i++) {
      if(cms[i].getWeight() <= 0.) {
        means[i] = new Vector(dimensionality);
        covarianceMatrices[i] = Matrix.identity(dimensionality, dimensionality);
      }
      else {
        means[i] = cms[i].getMeanVector();
        covarianceMatrices[i] = cms[i].destroyToNaiveMatrix().cheatToAvoidSingularity(SINGULARITY_CHEAT);
      }
    }
  }

  /**
   * Assigns the current probability values to the instances in the database and
   * compute the expectation value of the current mixture of distributions.
   * 
   * Computed as the sum of the logarithms of the prior probability of each
   * instance.
   * 
   * @param relation the database used for assignment to instances
   * @param normDistrFactor normalization factor for density function, based on
   *        current covariance matrix
   * @param means the current means
   * @param invCovMatr the inverse covariance matrices
   * @param clusterWeights the weights of the current clusters
   * @return the expectation value of the current mixture of distributions
   */
  public static double assignProbabilitiesToInstances(Relation<? extends NumberVector<?>> relation, double[] normDistrFactor, Vector[] means, Matrix[] invCovMatr, double[] clusterWeights, WritableDataStore<double[]> probClusterIGivenX) {
    final int k = clusterWeights.length;
    double emSum = 0.;

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      Vector x = relation.get(iditer).getColumnVector();
      double[] probabilities = new double[k];
      for(int i = 0; i < k; i++) {
        Vector difference = x.minus(means[i]);
        double rowTimesCovTimesCol = difference.transposeTimesTimes(invCovMatr[i], difference);
        double power = rowTimesCovTimesCol / 2.;
        double prob = normDistrFactor[i] * Math.exp(-power);
        if(LOG.isDebuggingFinest()) {
          LOG.debugFinest(" difference vector= ( " + difference.toString() + " )\n" + //
          " difference:\n" + FormatUtil.format(difference, "    ") + "\n" + //
          " rowTimesCovTimesCol:\n" + rowTimesCovTimesCol + "\n" + //
          " power= " + power + "\n" + " prob=" + prob + "\n" + //
          " inv cov matrix: \n" + FormatUtil.format(invCovMatr[i], "     "));
        }
        if(!(prob >= 0.)) {
          LOG.warning("Invalid probability: " + prob + " power: " + power + " factor: " + normDistrFactor[i]);
          prob = 0.;
        }
        probabilities[i] = prob;
      }
      double priorProbability = 0.;
      for(int i = 0; i < k; i++) {
        priorProbability += probabilities[i] * clusterWeights[i];
      }
      double logP = Math.max(Math.log(priorProbability), MIN_LOGLIKELIHOOD);
      if(!Double.isNaN(logP)) {
        emSum += logP;
      }

      double[] clusterProbabilities = new double[k];
      for(int i = 0; i < k; i++) {
        assert (clusterWeights[i] >= 0.);
        // do not divide by zero!
        if(priorProbability > 0.) {
          clusterProbabilities[i] = probabilities[i] / priorProbability * clusterWeights[i];
        }
        else {
          clusterProbabilities[i] = 0.;
        }
      }
      probClusterIGivenX.put(iditer, clusterProbabilities);
    }

    return emSum / relation.size();
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * @return the soft
   */
  public boolean isSoft() {
    return soft;
  }

  /**
   * @param soft the soft to set
   */
  public void setSoft(boolean soft) {
    this.soft = soft;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
    /**
     * Parameter to specify the number of clusters to find, must be an integer
     * greater than 0.
     */
    public static final OptionID K_ID = new OptionID("em.k", "The number of clusters to find.");

    /**
     * Parameter to specify the termination criterion for maximization of E(M):
     * E(M) - E(M') < em.delta, must be a double equal to or greater than 0.
     */
    public static final OptionID DELTA_ID = new OptionID("em.delta", //
    "The termination criterion for maximization of E(M): " + //
    "E(M) - E(M') < em.delta");

    /**
     * Parameter to specify the initialization method
     */
    public static final OptionID INIT_ID = new OptionID("kmeans.initialization", //
    "Method to choose the initial means.");

    /**
     * Number of clusters.
     */
    protected int k;

    /**
     * Stopping threshold
     */
    protected double delta;

    /**
     * Initialization method
     */
    protected KMeansInitialization<V> initializer;

    /**
     * Maximum number of iterations.
     */
    protected int maxiter = -1;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID);
      kP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }

      ObjectParameter<KMeansInitialization<V>> initialP = new ObjectParameter<>(INIT_ID, KMeansInitialization.class, RandomlyGeneratedInitialMeans.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }

      DoubleParameter deltaP = new DoubleParameter(DELTA_ID, 0.0);
      deltaP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(deltaP)) {
        delta = deltaP.getValue();
      }

      IntParameter maxiterP = new IntParameter(KMeans.MAXITER_ID);
      maxiterP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      maxiterP.setOptional(true);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.getValue();
      }
    }

    @Override
    protected EM<V> makeInstance() {
      return new EM<>(k, delta, initializer, maxiter, false);
    }
  }
}
