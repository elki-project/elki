/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.algorithm.clustering.em;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.argmax;

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
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
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

import net.jafama.FastMath;

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
@Priority(Priority.RECOMMENDED)
public class EM<V extends NumberVector, M extends MeanModel> extends AbstractAlgorithm<Clustering<M>> implements ClusteringAlgorithm<Clustering<M>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(EM.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = EM.class.getName();

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

  /**
   * Minimum loglikelihood to avoid -infinity.
   */
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
   *
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
    List<? extends EMClusterModel<M>> models = mfactory.buildInitialModels(database, relation, k, SquaredEuclideanDistanceFunction.STATIC);
    WritableDataStore<double[]> probClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);
    double loglikelihood = assignProbabilitiesToInstances(relation, models, probClusterIGivenX);
    DoubleStatistic likestat = LOG.isStatistics() ? new DoubleStatistic(this.getClass().getName() + ".loglikelihood") : null;
    if(LOG.isStatistics()) {
      LOG.statistics(likestat.setDouble(loglikelihood));
    }

    // iteration unless no change
    int it = 0;
    for(++it; it <= maxiter || maxiter < 0; it++) {
      final double oldloglikelihood = loglikelihood;
      recomputeCovarianceMatrices(relation, probClusterIGivenX, models);
      // reassign probabilities
      loglikelihood = assignProbabilitiesToInstances(relation, models, probClusterIGivenX);

      if(LOG.isStatistics()) {
        LOG.statistics(likestat.setDouble(loglikelihood));
      }
      if(loglikelihood - oldloglikelihood <= delta) {
        break;
      }
    }
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", it));
    }

    // fill result with clusters and models
    List<ModifiableDBIDs> hardClusters = new ArrayList<>(k);
    for(int i = 0; i < k; i++) {
      hardClusters.add(DBIDUtil.newArray());
    }

    // provide a hard clustering
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      hardClusters.get(argmax(probClusterIGivenX.get(iditer))).add(iditer);
    }
    Clustering<M> result = new Clustering<>("EM Clustering", "em-clustering");
    // provide models within the result
    for(int i = 0; i < k; i++) {
      result.addToplevelCluster(new Cluster<>(hardClusters.get(i), models.get(i).finalizeCluster()));
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
      for(int i = 0; i < clusterProbabilities.length; i++) {
        final double prob = clusterProbabilities[i];
        if(!(prob > 0.)) {
          continue;
        }
        models.get(i).updateE(instance, prob);
        wsum[i] += prob;
      }
    }
    for(int i = 0; i < models.size(); i++) {
      EMClusterModel<?> m = models.get(i);
      // Set weight before calling finalize, because this uses the new weight already!
      m.setWeight(wsum[i] / relation.size());
      m.finalizeEStep();
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
      double[] probs = new double[k];
      for(int i = 0; i < k; i++) {
        probs[i] = models.get(i).estimateLogDensity(vec);
      }
      double logP = logSumExp(probs);
      emSum += logP > MIN_LOGLIKELIHOOD ? logP : MIN_LOGLIKELIHOOD;
      for(int i = 0; i < k; i++) {
        probs[i] = FastMath.exp(probs[i] - logP);
      }
      probClusterIGivenX.put(iditer, probs);
    }
    return emSum / relation.size();
  }

  /**
   * Compute log(sum(exp(x_i)), with attention to numerical issues.
   * 
   * @param x Input
   * @return Result
   */
  private static double logSumExp(double[] x) {
    double max = x[0];
    int maxPos = 0;
    for(int i = 1; i < x.length; i++) {
      final double v = x[i];
      if(v > max) {
        max = v;
        maxPos = i;
      }
    }
    final double cutoff = max - 35.350506209; // log_e(2**51)
    double acc = 0.;
    for(int i = 0; i < x.length; i++) {
      final double v = x[i];
      if(i != maxPos && v > cutoff) {
        acc += FastMath.exp(v - max);
      }
    }
    return acc != 0 ? (max + FastMath.log1p(acc)) : max;
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
      IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
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
