/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
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
package elki.clustering.em;

import static elki.math.linearalgebra.VMath.times;
import static elki.math.linearalgebra.VMath.minus;
import static elki.math.linearalgebra.VMath.argmax;
import static elki.math.linearalgebra.VMath.timesTranspose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.em.KDTree.ClusterData;
import elki.clustering.kmeans.KMeans;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.*;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.result.Metadata;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

public class EMKD<M extends MeanModel> implements ClusteringAlgorithm<Clustering<M>> {
  //
  /**
   * Factory for producing the initial cluster model.
   */
  private EMClusterModelFactory<NumberVector, M> mfactory;

  private static final Logging LOG = Logging.getLogger(EMKD.class);

  private static final double MIN_LOGLIKELIHOOD = -100000;

  /**
   * Retain soft assignments.
   */
  private boolean soft;

  /**
   * Delta parameter
   */
  private double delta;

  /**
   * Soft assignment result type.
   */
  public static final SimpleTypeInformation<double[]> SOFT_TYPE = new SimpleTypeInformation<>(double[].class);

  private int k = 3;

  private double mbw;

  // currently not used
  private int miniter;

  private int maxiter;

  protected ArrayModifiableDBIDs sorted;

  public EMKD(int k, double mbw, double delta, EMClusterModelFactory<NumberVector, M> mfactory, int miniter, int maxiter, boolean soft) {
    this.k = k;
    this.mbw = mbw;
    this.delta = delta;
    this.mfactory = mfactory;
    this.miniter = miniter;
    this.maxiter = maxiter;
    this.soft = soft;
  }

  /**
   * I took the run method from EM.java and I am rewriting it to work on KD
   * trees.
   * 
   * @param relation
   * @return
   */
  public Clustering<M> run(Relation<? extends NumberVector> relation) {

    if(relation.size() == 0) {
      throw new IllegalArgumentException("database empty: must contain elements");
    }
    // build kd-tree
    sorted = DBIDUtil.newArray(relation.getDBIDs());
    double[] dimwidth = analyseDimWidth(relation);
    KDTree tree = new KDTree(relation, sorted, 0, sorted.size(), dimwidth, mbw);

    // initial models
    ArrayList<? extends EMClusterModel<NumberVector, M>> models = new ArrayList<EMClusterModel<NumberVector, M>>(mfactory.buildInitialModels(relation, k));
    WritableDataStore<double[]> probClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);
    double loglikelihood = assignProbabilitiesToInstances(relation, models, probClusterIGivenX);

    // iteration unless no change
    int it = 0;
    // double bestloglikelihood = loglikelihood; // For detecting instabilities.
    for(; it < maxiter || maxiter < 0; it++) {

      // recalculate probabilities
      ClusterData[] newstats = tree.makeStats(relation.size(), models);

      updateClusters(newstats, models, relation.size());
      // here i need to finish the makeStats and then apply them

      // LOG.statistics(likestat.setDouble(loglikelihood));
      // if(loglikelihood - bestloglikelihood > delta) {
      // lastimprovement = it;
      // bestloglikelihood = loglikelihood;
      // }
      // if(it >= miniter && (Math.abs(loglikelihood - oldloglikelihood) <=
      // delta || lastimprovement < it >> 1)) {
      // break;
      // }
    }
    // LOG.statistics(new LongStatistic(KEY + ".iterations", it));

    // fill result with clusters and models
    List<ModifiableDBIDs> hardClusters = new ArrayList<>(k);
    for(int i = 0; i < k; i++) {
      hardClusters.add(DBIDUtil.newArray());
    }

    loglikelihood = assignProbabilitiesToInstances(relation, models, probClusterIGivenX);

    // provide a hard clustering
    // add each point to cluster of max density
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      hardClusters.get(argmax(probClusterIGivenX.get(iditer))).add(iditer);
    }
    Clustering<M> result = new Clustering<>();
    Metadata.of(result).setLongName("EMKD Clustering");
    // provide models within the result
    for(int i = 0; i < k; i++) {
      result.addToplevelCluster(new Cluster<>(hardClusters.get(i), models.get(i).finalizeCluster()));
    }
    if(soft) {
      Metadata.hierarchyOf(result).addChild(new MaterializedRelation<>("EMKD Cluster Probabilities", SOFT_TYPE, relation.getDBIDs(), probClusterIGivenX));
    }
    else {
      probClusterIGivenX.destroy();
    }
    return result;
  }

  private void updateClusters(ClusterData[] newstats, ArrayList<? extends EMClusterModel<NumberVector, M>> models, int size) {
    for(int i = 0; i < k; i++) {
      double prob = FastMath.exp(newstats[i].logApriori_sw - FastMath.log(size));
      models.get(i).setWeight(prob);
      // this might need to change to a set / get method and not a tvar method
      // bcause models might apply changes during set
      // this doesnt affect it currently as there are no changes made so far
      double[] tcenter = times(newstats[i].center_swx, 1. / FastMath.exp(newstats[i].logApriori_sw));
      models.get(i).setCenter(tcenter);
      double[][] tcov = minus(times(newstats[i].cov_swxx, 1. / FastMath.exp(newstats[i].logApriori_sw)), timesTranspose(tcenter, tcenter));
      models.get(i).updateCovariance(tcov);
    }
  }

  

  /**
   * helper method to retrieve the widths of all data in all dimensions
   * 
   * @param relation
   * @return
   */
  private double[] analyseDimWidth(Relation<? extends NumberVector> relation) {
    DBIDIter it = relation.iterDBIDs();
    int d = relation.get(it).getDimensionality();
    // TODO remove TEST
    // if(true) {
    // return new double[d];
    // }
    // TEST
    double[][] arr = new double[d][2];
    for(int i = 0; i < d; i++) {
      arr[i][0] = Double.MAX_VALUE;
    }
    double[] result = new double[d];
    for(; it.valid(); it.advance()) {
      NumberVector x = relation.get(it);
      for(int i = 0; i < d; i++) {
        double t = x.doubleValue(i);
        arr[i][0] = arr[i][0] < t ? arr[i][0] : t;
        arr[i][1] = arr[i][1] > t ? arr[i][1] : t;
      }
    }
    for(int i = 0; i < d; i++) {
      result[i] = arr[i][1] - arr[i][0];
    }
    return result;
  }

  /**
   * Assigns the current probability values to the instances in the database and
   * compute the expectation value of the current mixture of distributions.
   * <p>
   * Computed as the sum of the logarithms of the prior probability of each
   * instance.
   * 
   * @param relation the database used for assignment to instances
   * @param models Cluster models
   * @param probClusterIGivenX Output storage for cluster probabilities
   * @param <O> Object type
   * @return the expectation value of the current mixture of distributions
   */
  public static <O> double assignProbabilitiesToInstances(Relation<? extends O> relation, List<? extends EMClusterModel<O, ?>> models, WritableDataStore<double[]> probClusterIGivenX) {
    final int k = models.size();
    double emSum = 0.;

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      O vec = relation.get(iditer);
      double[] probs = new double[k];
      for(int i = 0; i < k; i++) {
        double v = models.get(i).estimateLogDensity(vec);
        probs[i] = v > MIN_LOGLIKELIHOOD ? v : MIN_LOGLIKELIHOOD;
      }
      final double logP = logSumExp(probs);
      for(int i = 0; i < k; i++) {
        probs[i] = FastMath.exp(probs[i] - logP);
      }
      probClusterIGivenX.put(iditer, probs);
      emSum += logP;
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
    for(int i = 1; i < x.length; i++) {
      final double v = x[i];
      max = v > max ? v : max;
    }
    final double cutoff = max - 35.350506209; // log_e(2**51)
    double acc = 0.;
    for(int i = 0; i < x.length; i++) {
      final double v = x[i];
      if(v > cutoff) {
        acc += v < max ? FastMath.exp(v - max) : 1.;
      }
    }
    return acc > 1. ? (max + FastMath.log(acc)) : max;
  }


  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<M extends MeanModel> implements Parameterizer {
    /**
     * Parameter to specify the number of clusters to find, must be an integer
     * greater than 0.
     */
    public static final OptionID K_ID = new OptionID("emkd.k", "The number of clusters to find.");

    /**
     * Parameter to specify the termination criterion for KD-Tree construction.
     * Stop splitting nodes when the width is smaller then mbw * dataset_width.
     * Musst be between 0 and 1.
     */
    public static final OptionID MBW_ID = new OptionID("emkd.mbw", //
        "Pruning criterion for the KD-Tree. Stop splitting when leafwidth < mbw * width");

    /**
     * Parameter to specify the termination criterion for maximization of E(M):
     * E(M) - E(M') &lt; em.delta, must be a double equal to or greater than 0.
     */
    public static final OptionID DELTA_ID = new OptionID("emkd.delta", //
        "The termination criterion for maximization of E(M): E(M) - E(M') < em.delta");

    /**
     * Parameter to specify the EM cluster models to use.
     */
    public static final OptionID INIT_ID = new OptionID("emkd.model", "Model factory.");

    /**
     * Parameter to specify a minimum number of iterations
     */
    public static final OptionID MINITER_ID = new OptionID("emkd.miniter", "Minimum number of iterations.");

    /**
     * Parameter to specify the MAP prior
     */
    public static final OptionID PRIOR_ID = new OptionID("emkd.map.prior", "Regularization factor for MAP estimation.");

    /**
     * Number of clusters.
     */
    protected int k;

    /**
     * construction threshold
     */
    protected double mbw;

    /**
     * Stopping threshold
     */
    protected double delta;

    /**
     * Initialization method
     */
    protected EMClusterModelFactory<NumberVector, M> initializer;

    /**
     * Minimum number of iterations.
     */
    protected int miniter = 1;

    /**
     * Maximum number of iterations.
     */
    protected int maxiter = -1;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new DoubleParameter(MBW_ID, 0.01)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE) //
          .grab(config, x -> mbw = x);
      new ObjectParameter<EMClusterModelFactory<NumberVector, M>>(INIT_ID, EMClusterModelFactory.class, MultivariateGaussianModelFactory.class) //
          .grab(config, x -> initializer = x);
      new DoubleParameter(DELTA_ID, 1e-7)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> delta = x);
      new IntParameter(MINITER_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .setOptional(true) //
          .grab(config, x -> miniter = x);
      new IntParameter(KMeans.MAXITER_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .setOptional(true) //
          .grab(config, x -> maxiter = x);
    }

    @Override
    public EMKD<M> make() {
      return new EMKD<>(k, mbw, delta, initializer, miniter, maxiter, false);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }
}
