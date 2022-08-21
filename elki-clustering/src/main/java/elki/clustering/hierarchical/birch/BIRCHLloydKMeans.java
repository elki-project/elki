/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.clustering.hierarchical.birch;

import static elki.math.linearalgebra.VMath.*;

import java.util.Arrays;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.hierarchical.birch.CFTree.LeafIterator;
import elki.clustering.kmeans.KMeans;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.data.model.MeanModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * BIRCH-based clustering algorithm that simply treats the leafs of the CFTree
 * as clusters.
 * <p>
 * References:
 * <p>
 * T. Zhang, R. Ramakrishnan, M. Livny<br>
 * BIRCH: An Efficient Data Clustering Method for Very Large Databases
 * Proc. 1996 ACM SIGMOD International Conference on Management of Data
 * <p>
 * T. Zhang, R. Ramakrishnan, M. Livny<br>
 * BIRCH: A New Data Clustering Algorithm and Its Applications
 * Data. Min. Knowl. Discovery
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @depend - - - CFTree
 */
@Reference(authors = "T. Zhang, R. Ramakrishnan, M. Livny", //
    title = "BIRCH: An Efficient Data Clustering Method for Very Large Databases", //
    booktitle = "Proc. 1996 ACM SIGMOD International Conference on Management of Data", //
    url = "https://doi.org/10.1145/233269.233324", //
    bibkey = "DBLP:conf/sigmod/ZhangRL96")
@Reference(authors = "T. Zhang, R. Ramakrishnan, M. Livny", //
    title = "BIRCH: A New Data Clustering Algorithm and Its Applications", //
    booktitle = "Data Min. Knowl. Discovery", //
    url = "https://doi.org/10.1023/A:1009783824328", //
    bibkey = "DBLP:journals/datamine/ZhangRL97")
public class BIRCHLloydKMeans implements ClusteringAlgorithm<Clustering<MeanModel>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(BIRCHLloydKMeans.class);

  /**
   * CFTree factory.
   */
  CFTree.Factory cffactory;

  /**
   * Number of cluster centers to initialize.
   */
  int k;

  /**
   * Maximum number of iterations.
   */
  int maxiter;

  /**
   * k-means++ initialization
   */
  BIRCHKMeansPlusPlus initialization;

  /**
   * Constructor.
   *
   * @param cffactory CFTree factory
   * @param k Number of clusters
   * @param maxiter Maximum number of iterations
   * @param initialization Initialization method for k-means
   */
  public BIRCHLloydKMeans(CFTree.Factory cffactory, int k, int maxiter, BIRCHKMeansPlusPlus initialization) {
    super();
    this.cffactory = cffactory;
    this.k = k;
    this.maxiter = maxiter;
    this.initialization = initialization;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Run the clustering algorithm.
   *
   * @param relation Input data
   * @return Clustering
   */
  public Clustering<KMeansModel> run(Relation<NumberVector> relation) {
    CFTree tree = cffactory.newTree(relation.getDBIDs(), relation);

    // For efficiency, we also need the mean of each CF:
    ClusteringFeature[] cfs = new ClusteringFeature[tree.leaves];
    double[][] cfmeans = new double[tree.leaves][];

    int z = 0;
    for(LeafIterator iter = tree.leafIterator(); iter.valid(); iter.advance()) {
      ClusteringFeature f = cfs[z] = iter.get();
      cfmeans[z] = times(f.ls, 1. / f.n);
      z++;
    }

    int[] assignment = new int[tree.leaves], weights = new int[k];
    Arrays.fill(assignment, -1);
    double[][] means = kmeans(cfmeans, cfs, assignment, weights);

    // The CFTree does not store points. We have to reassign them; but rather
    // than assigning them to n > k cluster features, we just assign them to the
    // nearest CF, we assign them to the means directly (there currently is no
    // API that would allow a user to get the means only, and no cluster
    // assignment for each point).
    double[] varsum = new double[k];
    ModifiableDBIDs[] ids = new ModifiableDBIDs[k];
    for(int i = 0; i < k; i++) {
      ids[i] = DBIDUtil.newArray(weights[i]);
    }
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      NumberVector fv = relation.get(iter);
      double mindist = distance(fv, means[0]);
      int minIndex = 0;
      for(int i = 1; i < k; i++) {
        double dist = distance(fv, means[i]);
        if(dist < mindist) {
          minIndex = i;
          mindist = dist;
        }
      }
      varsum[minIndex] += mindist;
      ids[minIndex].add(iter);
    }
    Clustering<KMeansModel> result = new Clustering<>();
    for(int i = 0; i < ids.length; i++) {
      KMeansModel model = new KMeansModel(means[i], varsum[i]);
      result.addToplevelCluster(new Cluster<KMeansModel>(ids[i], model));
    }
    Metadata.of(result).setLongName("BIRCH k-Means Clustering");
    return result;
  }

  /**
   * Perform k-means clustering.
   *
   * @param cfmeans Cluster feature means
   * @param cfs Cluster features
   * @param assignment Cluster assignment of each CF
   * @param weights Cluster weight output
   * @return Cluster means
   */
  private double[][] kmeans(double[][] cfmeans, ClusteringFeature[] cfs, int[] assignment, int[] weights) {
    double[][] means = initialization.run(cfmeans, k);
    for(int i = 1; i <= maxiter || maxiter <= 0; i++) {
      means = i == 1 ? means : means(assignment, means, cfs, weights);
      if(i > 1 && LOG.isStatistics()) {
        // This function is only correct after reassigning the points and then
        // updating the means, so in the second iteration onward.
        double varsum = sum(calculateVariances(assignment, means, cfs, weights));
        LOG.statistics(new DoubleStatistic(getClass().getName() + "." + (i - 1) + ".varsum", varsum));
      }
      int changed = assignToNearestCluster(assignment, means, cfmeans, cfs, weights);
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(getClass().getName() + "." + i + ".reassigned", changed));
      }
      if(changed == 0) {
        break;
      }
    }
    return means;
  }

  /**
   * Calculate means of clusters.
   * 
   * @param assignment Cluster assignment
   * @param means Means of clusters
   * @param cfs Clustering features
   * @return Means of clusters.
   */
  private double[][] means(int[] assignment, double[][] means, ClusteringFeature[] cfs, int[] weights) {
    Arrays.fill(weights, 0);
    double[][] newMeans = new double[k][];
    for(int i = 0; i < assignment.length; i++) {
      int c = assignment[i];
      final ClusteringFeature cf = cfs[i];
      if(newMeans[c] == null) {
        newMeans[c] = cf.ls.clone();
      }
      else {
        plusEquals(newMeans[c], cf.ls);
      }
      weights[c] += cf.n;
    }
    for(int i = 0; i < k; i++) {
      if(weights[i] == 0) {
        newMeans[i] = means[i];
        continue;
      }
      timesEquals(newMeans[i], 1.0 / weights[i]);
    }
    return newMeans;
  }

  /**
   * Assign each element to nearest cluster.
   * 
   * @param assignment Current cluster assignment
   * @param means k-means cluster means
   * @param cfmeans Cluster feature means
   * @param cfs Cluster features
   * @param weights Cluster weights (output)
   * @return Number of reassigned elements
   */
  private int assignToNearestCluster(int[] assignment, double[][] means, double[][] cfmeans, ClusteringFeature[] cfs, int[] weights) {
    Arrays.fill(weights, 0);
    int changed = 0;
    for(int i = 0; i < cfmeans.length; i++) {
      double mindist = distance(cfmeans[i], means[0]);
      int minIndex = 0;
      for(int j = 1; j < k; j++) {
        double dist = distance(cfmeans[i], means[j]);
        if(dist < mindist) {
          minIndex = j;
          mindist = dist;
        }
      }
      if(assignment[i] != minIndex) {
        changed++;
        assignment[i] = minIndex;
      }
      weights[minIndex] += cfs[i].n;
    }
    return changed;
  }

  /**
   * Compute a distance (and count the distance computations).
   *
   * @param x First object
   * @param y Second object
   * @return Distance
   */
  protected double distance(NumberVector x, double[] y) {
    double v = 0;
    for(int i = 0; i < y.length; i++) {
      double d = x.doubleValue(i) - y[i];
      v += d * d;
    }
    return v;
  }

  /**
   * Compute a distance (and count the distance computations).
   *
   * @param x First object
   * @param y Second object
   * @return Distance
   */
  protected double distance(double[] x, double[] y) {
    double v = 0;
    for(int i = 0; i < x.length; i++) {
      double d = x[i] - y[i];
      v += d * d;
    }
    return v;
  }

  /**
   * Calculate variance of clusters based on clustering features.
   * <p>
   * The result is only correct after updating the means!
   *
   * @param assignment Cluster assignment of CFs
   * @param means Cluster means
   * @param cfs CF leaves
   * @param weights Cluster weights
   * @return Per-cluster variances
   */
  private double[] calculateVariances(int[] assignment, double[][] means, ClusteringFeature[] cfs, int[] weights) {
    double[] ss = new double[k];
    for(int i = 0; i < assignment.length; i++) {
      ss[assignment[i]] += cfs[i].ss;
    }
    for(int i = 0; i < k; i++) {
      ss[i] -= squareSum(means[i]) * weights[i];
    }
    return ss;
  }

  /**
   * Parameterization class.
   *
   * @author Andreas Lang
   */
  public static class Par implements Parameterizer {
    /**
     * CFTree factory.
     */
    CFTree.Factory cffactory;

    /**
     * k Parameter.
     */
    protected int k;

    /**
     * Maximum number of iterations.
     */
    protected int maxiter;

    /**
     * initialization method
     */
    protected BIRCHKMeansPlusPlus initialization;

    @Override
    public void configure(Parameterization config) {
      cffactory = config.tryInstantiate(CFTree.Factory.class);
      new IntParameter(KMeans.K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new IntParameter(KMeans.MAXITER_ID, 0)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> maxiter = x);
      // TODO: make parameterizable, implement alternatives?
      initialization = config.tryInstantiate(BIRCHKMeansPlusPlus.class);
    }

    @Override
    public BIRCHLloydKMeans make() {
      return new BIRCHLloydKMeans(cffactory, k, maxiter, initialization);
    }
  }
}
