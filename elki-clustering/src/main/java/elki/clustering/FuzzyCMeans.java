/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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
package elki.clustering;

import static elki.math.linearalgebra.VMath.argmax;

import java.util.ArrayList;
import java.util.List;

import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.kmeans.initialization.RandomlyChosen;
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
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.Relation;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.math.linearalgebra.VMath;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Fuzzy Clustering developed by James Bezdek
 * 
 * @author Robert Gehde
 *
 * @param <V> Vector Type of the data, must be subclass of {@link NumberVector}
 */
@Reference(authors = "Bezdek, James", //
    title = "Pattern Recognition With Fuzzy Objective Function Algorithms", //
    booktitle = "Pattern Recognition With Fuzzy Objective Function Algorithms", //
    url = "https://doi.org/10.1007/978-1-4757-0450-1", //
    bibkey = "DBLP:books/sp/Bezdek81")
public class FuzzyCMeans<V extends NumberVector> implements ClusteringAlgorithm<Clustering<MeanModel>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(FuzzyCMeans.class);

  /**
   * Key for statistics logging.
   */
  private static final String KEY = FuzzyCMeans.class.getName();

  /**
   * Number of clusters
   */
  private int k;

  /**
   * weight exponent
   */
  private double m;

  /**
   * Delta parameter
   */
  private double delta;

  /**
   * Minimum number of iterations to do
   */
  private int miniter;

  /**
   * Maximum number of iterations to allow
   */
  private int maxiter;

  /**
   * Retain soft assignments.
   */
  private boolean soft;

  /**
   * Soft assignment result type.
   */
  public static final SimpleTypeInformation<double[]> SOFT_TYPE = new SimpleTypeInformation<>(double[].class);

  /**
   * Produces initial cluster.
   */
  KMeansInitialization initializer;

  /**
   * 
   * Constructor.
   *
   * @param k number of clusters
   * @param miniter minimum iterations
   * @param maxiter maximum iterations
   * @param delta stopping threshold
   * @param m weight exponent
   * @param soft retain soft clustering?
   * @param initialization initial cluster centers
   */
  public FuzzyCMeans(int k, int miniter, int maxiter, double delta, double m, boolean soft, KMeansInitialization initialization) {
    this.miniter = miniter;
    this.maxiter = maxiter;
    this.delta = delta;
    this.k = k;
    this.m = m;
    this.soft = soft;
    this.initializer = initialization;
  }

  /**
   * Runs Fuzzy C Means clustering on the given Relation
   * 
   * @param relation data to cluster
   * @return Clustering
   */
  public Clustering<MeanModel> run(Relation<V> relation) {
    if(relation.size() == 0) {
      throw new IllegalArgumentException("database empty: must contain elements");
    }
    WritableDataStore<double[]> probClusterIGivenX = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_SORTED, double[].class);
    int d = relation.get(relation.iterDBIDs()).getDimensionality();

    // build initial fuzzy partition
    double[][] means = initializer.chooseInitialMeans(relation, k, SquaredEuclideanDistance.STATIC);
    double objfctvalue = assignProbabilitiesToInstances(relation, means, probClusterIGivenX);

    DoubleStatistic likestat = new DoubleStatistic(this.getClass().getName() + ".objectivevalue");
    LOG.statistics(likestat.setDouble(objfctvalue));

    int it = 0, lastimprovement = 0;
    double bestobjfctvalue = objfctvalue; // For detecting instabilities.
    for(++it; it < maxiter || maxiter < 0; it++) {
      final double oldobjfctvalue = objfctvalue;
      // calculate centers
      updateMeans(relation, probClusterIGivenX, means, d);
      // assign
      objfctvalue = assignProbabilitiesToInstances(relation, means, probClusterIGivenX);

      LOG.statistics(likestat.setDouble(objfctvalue));
      // delta negative, because minimization and not maximization
      if(bestobjfctvalue - objfctvalue > delta) {
        lastimprovement = it;
        bestobjfctvalue = objfctvalue;
      }
      if(it >= miniter && (Math.abs(objfctvalue - oldobjfctvalue) <= delta || lastimprovement < it >> 1)) {
        break;
      }
    }
    LOG.statistics(new LongStatistic(KEY + ".iterations", it));
    // create result
    Clustering<MeanModel> result = new Clustering<>();
    Metadata.of(result).setLongName("FCM Clustering");

    List<ModifiableDBIDs> hardClusters = new ArrayList<>(k);
    for(int i = 0; i < k; i++) {
      hardClusters.add(DBIDUtil.newArray());
    }

    // provide a hard clustering
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      hardClusters.get(argmax(probClusterIGivenX.get(iditer))).add(iditer);
    }
    // provide models within the result
    for(int i = 0; i < k; i++) {
      result.addToplevelCluster(new Cluster<MeanModel>(hardClusters.get(i), new MeanModel(means[i])));
    }
    // retain soft assignments
    if(soft) {
      Metadata.hierarchyOf(result).addChild(new MaterializedRelation<>("FCM Cluster Probabilities", SOFT_TYPE, relation.getDBIDs(), probClusterIGivenX));
    }
    else {
      probClusterIGivenX.destroy();
    }
    return result;
  }

  /**
   * Updates the means according to the weighted means of all data points.
   * 
   * @param relation data points
   * @param probClusterIGivenX weights of clusters
   * @param means destination array for means
   * @param d dimensionality of the data
   */
  private void updateMeans(Relation<V> relation, WritableDataStore<double[]> probClusterIGivenX, double[][] means, int d) {
    double[] weight = new double[k];
    double[][] tmeans = new double[k][d];
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double[] probs = probClusterIGivenX.get(iditer);
      for(int i = 0; i < k; i++) {
        double w = FastMath.pow(probs[i], m);
        weight[i] += w;
        tmeans[i] = VMath.plusTimesEquals(tmeans[i], relation.get(iditer).toArray(), w);
      }
    }
    for(int i = 0; i < k; i++) {
      means[i] = VMath.times(tmeans[i], 1 / weight[i]);
    }
  }

  /**
   * calculates the weights of all points and clusters. As they add up to one
   * for each point, they can be seen as cluster probabilities P(c_i|x_j)
   * 
   * @param relation data points
   * @param centers current cluster centers
   * @param probClusterIGivenX destination datastore for probabilities/weights
   * @return value of the objective function
   */
  public double assignProbabilitiesToInstances(Relation<V> relation, double[][] centers, WritableDataStore<double[]> probClusterIGivenX) {
    final int k = centers.length;
    double objval = 0;
    int dirass = -1; // direct assignmend if dist == 0
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      V vec = relation.get(iditer);
      double[] probs = new double[k];
      double[] dists = new double[k];
      double sumprob = 0;
      double objval2 = 0;
      for(int i = 0; i < k; i++) {
        double[] clustermean = centers[i];
        for(int d = 0; d < clustermean.length; d++) {
          dists[i] += (vec.doubleValue(d) - clustermean[d]) * (vec.doubleValue(d) - clustermean[d]);
        }
        if(dists[i] == 0) {
          dirass = i;
          probs[i] = 1;
          break;
        }
        probs[i] = FastMath.pow(FastMath.sqrt(dists[i]), 2 / (m - 1)); // d^(2/(m-1))
        sumprob += 1 / probs[i]; // sum 1/d^(1/(m-1))

      }
      if(dirass != -1) {
        // set all other values in probs = 0:
        for(int i = 0; i < probs.length; i++) {
          if(i != dirass)
            probs[i] = 0;
        }
        probClusterIGivenX.put(iditer, probs);
        continue;
      }
      // conv sum to logspace
      // sumprob = FastMath.log(sumprob);
      // calc actual probs
      for(int i = 0; i < k; i++) {
        probs[i] = 1 / (probs[i] * sumprob);
        objval2 += Math.pow(probs[i], m) * dists[i];
      }
      probClusterIGivenX.put(iditer, probs);
      objval += objval2;
    }
    return objval;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  public static class Par implements Parameterizer {
    /**
     * Parameter to specify the number of clusters to find, must be an integer
     * greater than 0.
     */
    public static final OptionID K_ID = new OptionID("fcm.k", "The number of clusters to find.");

    /**
     * Parameter to specify the termination criterion for maximization of E(M):
     * E(M) - E(M') &lt; fcm.delta, must be a double equal to or greater than 0.
     */
    public static final OptionID DELTA_ID = new OptionID("fcm.delta", //
        "The termination criterion for maximization of E(M): E(M) - E(M') < fcm.delta");

    /**
     * Parameter to specify a minimum number of iterations
     */
    public static final OptionID MINITER_ID = new OptionID("fcm.miniter", "Minimum number of iterations.");

    /**
     * Parameter to set weight exponent
     */
    public static final OptionID M_ID = new OptionID("fcm.m", "Weight exponent.");

    /**
     * Parameter to retain soft assignments
     */
    public static final OptionID SOFT_ID = new OptionID("fcm.soft", "Retain soft assignments.");

    /**
     * Parameter for k-Means init for initial cluster centers
     */
    public static final OptionID INIT_ID = new OptionID("fcm.init", "Cluster Initialization");

    /**
     * Number of clusters.
     */
    protected int k;

    /**
     * Stopping threshold
     */
    protected double delta;

    /**
     * Minimum number of iterations.
     */
    protected int miniter = 1;

    /**
     * Maximum number of iterations.
     */
    protected int maxiter = -1;

    /**
     * Weight exponent
     */
    protected double m = 2;

    /**
     * retain soft assignments
     */
    protected boolean soft = false;

    /**
     * K-Means init for initial cluster centers
     */
    KMeansInitialization initializer;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new ObjectParameter<KMeansInitialization>(INIT_ID, KMeansInitialization.class, RandomlyChosen.class) //
          .grab(config, x -> initializer = x);
      new DoubleParameter(DELTA_ID, 1e-7) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> delta = x);
      new DoubleParameter(M_ID, 2) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_DOUBLE) //
          .grab(config, x -> m = x);
      new IntParameter(MINITER_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .setOptional(true) //
          .grab(config, x -> miniter = x);
      new IntParameter(KMeans.MAXITER_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .setOptional(true) //
          .grab(config, x -> maxiter = x);
      new Flag(SOFT_ID) //
          .grab(config, x -> soft = x);
    }

    @Override
    public FuzzyCMeans<NumberVector> make() {
      return new FuzzyCMeans<NumberVector>(k, miniter, maxiter, delta, m, soft, initializer);
    }

  }
}
