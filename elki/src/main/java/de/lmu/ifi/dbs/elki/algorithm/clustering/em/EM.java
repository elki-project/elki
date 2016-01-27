package de.lmu.ifi.dbs.elki.algorithm.clustering.em;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
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
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;
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
 * Clustering by expectation maximization (EM-Algorithm), also known as Gaussian
 * Mixture Modeling (GMM).
 * 
 * Reference:
 * <p>
 * A. P. Dempster, N. M. Laird, D. B. Rubin:<br />
 * Maximum Likelihood from Incomplete Data via the EM algorithm.<br />
 * In Journal of the Royal Statistical Society, Series B, 39(1), 1977, pp. 1-31
 * </p>
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.2
 * 
 * @apiviz.composedOf EMClusterModelFactory
 * 
 * @param <V> vector type to analyze
 * @param <M> model type to produce
 */
@Title("EM-Clustering: Clustering by Expectation Maximization")
@Description("Cluster data via Gaussian mixture modeling and the EM algorithm")
@Reference(authors = "A. P. Dempster, N. M. Laird, D. B. Rubin", //
title = "Maximum Likelihood from Incomplete Data via the EM algorithm", //
booktitle = "Journal of the Royal Statistical Society, Series B, 39(1), 1977, pp. 1-31", //
url = "http://www.jstor.org/stable/2984875")
@Alias("de.lmu.ifi.dbs.elki.algorithm.clustering.EM")
public class EM<V extends NumberVector, M extends MeanModel> extends AbstractAlgorithm<Clustering<M>> implements ClusteringAlgorithm<Clustering<M>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(EM.class);

  /**
   * Number of clusters
   */
  private int k;

  /**
   * Delta parameter
   */
  private double delta;

  /**
   * Factory for producing the initial cluster model.
   */
  private EMClusterModelFactory<V, M> mfactory;

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
   * @param mfactory EM cluster model factory
   * @param maxiter Maximum number of iterations
   * @param soft Include soft assignments
   */
  public EM(int k, double delta, EMClusterModelFactory<V, M> mfactory, int maxiter, boolean soft) {
    super();
    this.k = k;
    this.delta = delta;
    this.mfactory = mfactory;
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
  public Clustering<M> run(Database database, Relation<V> relation) {
    if(relation.size() == 0) {
      throw new IllegalArgumentException("database empty: must contain elements");
    }
    // initial models
    if(LOG.isVerbose()) {
      LOG.verbose("initializing " + k + " models");
    }
    List<? extends EMClusterModel<M>> models = mfactory.buildInitialModels(database, relation, k, SquaredEuclideanDistanceFunction.STATIC);
    WritableDataStore<double[]> probClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);
    double emNew = assignProbabilitiesToInstances(relation, models, probClusterIGivenX);

    // iteration unless no change
    if(LOG.isVerbose()) {
      LOG.verbose("iterating EM");
    }
    if(LOG.isVerbose()) {
      LOG.verbose("iteration " + 0 + " - expectation value: " + emNew);
    }

    for(int it = 1; it <= maxiter || maxiter < 0; it++) {
      final double emOld = emNew;
      recomputeCovarianceMatrices(relation, probClusterIGivenX, models);
      // reassign probabilities
      emNew = assignProbabilitiesToInstances(relation, models, probClusterIGivenX);

      if(LOG.isVerbose()) {
        LOG.verbose("iteration " + it + " - expectation value: " + emNew);
      }
      if(Math.abs(emOld - emNew) <= delta || emOld > emNew) {
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
    Clustering<M> result = new Clustering<>("EM Clustering", "em-clustering");
    // provide models within the result
    for(int i = 0; i < k; i++) {
      // TODO: re-do labeling.
      // SimpleClassLabel label = new SimpleClassLabel();
      // label.init(result.canonicalClusterLabel(i));
      Cluster<M> model = new Cluster<>(hardClusters.get(i), models.get(i).finalizeCluster());
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
   * Recompute the covariance matrixes.
   * 
   * @param relation Vector data
   * @param probClusterIGivenX Object probabilities
   * @param models Cluster models to update
   */
  public static void recomputeCovarianceMatrices(Relation<? extends NumberVector> relation, WritableDataStore<double[]> probClusterIGivenX, List<? extends EMClusterModel<?>> models) {
    for(EMClusterModel<?> m : models) {
      m.beginEStep();
    }
    double[] wsum = new double[models.size()];
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double[] clusterProbabilities = probClusterIGivenX.get(iditer);
      NumberVector instance = relation.get(iditer);
      int i = 0;
      for(EMClusterModel<?> m : models) {
        final double prior = clusterProbabilities[i];
        if(prior > 0.) {
          m.updateE(instance, prior);
        }
        wsum[i] += prior;
        ++i;
      }
    }
    int i = 0;
    for(EMClusterModel<?> m : models) {
      m.finalizeEStep();
      m.setWeight(wsum[i] / relation.size());
      i++;
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
   * @param models Cluster models
   * @param probClusterIGivenX Output storage for cluster probabilities
   * @return the expectation value of the current mixture of distributions
   */
  public static double assignProbabilitiesToInstances(Relation<? extends NumberVector> relation, List<? extends EMClusterModel<?>> models, WritableDataStore<double[]> probClusterIGivenX) {
    final int k = models.size();
    double emSum = 0.;

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      NumberVector vec = relation.get(iditer);
      double[] probabilities = new double[k];
      {
        int i = 0;
        for(EMClusterModel<?> m : models) {
          probabilities[i] = m.estimateDensity(vec);
          ++i;
        }
      }
      double priorProbability = 0.;
      for(int i = 0; i < k; i++) {
        priorProbability += probabilities[i];
      }
      double logP = Math.max(Math.log(priorProbability), MIN_LOGLIKELIHOOD);
      emSum += (logP == logP) ? logP : 0.; /* avoid NaN */

      double[] clusterProbabilities = new double[k];
      if(priorProbability > 0.) {
        for(int i = 0; i < k; i++) {
          // do not divide by zero!
          clusterProbabilities[i] = probabilities[i] / priorProbability;
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
  public static class Parameterizer<V extends NumberVector, M extends MeanModel> extends AbstractParameterizer {
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
     * Parameter to specify the EM cluster models to use.
     */
    public static final OptionID INIT_ID = new OptionID("em.model", //
    "Model factory.");

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
    protected EMClusterModelFactory<V, M> initializer;

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

      ObjectParameter<EMClusterModelFactory<V, M>> initialP = new ObjectParameter<>(INIT_ID, EMClusterModelFactory.class, MultivariateGaussianModelFactory.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }

      DoubleParameter deltaP = new DoubleParameter(DELTA_ID, 1e-7)//
      .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(deltaP)) {
        delta = deltaP.getValue();
      }

      IntParameter maxiterP = new IntParameter(KMeans.MAXITER_ID)//
      .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
      .setOptional(true);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.getValue();
      }
    }

    @Override
    protected EM<V, M> makeInstance() {
      return new EM<>(k, delta, initializer, maxiter, false);
    }
  }
}
