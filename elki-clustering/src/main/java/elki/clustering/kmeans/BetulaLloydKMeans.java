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
package elki.clustering.kmeans;

import static elki.math.linearalgebra.VMath.timesEquals;

import java.util.ArrayList;
import java.util.Arrays;

import elki.clustering.kmeans.initialization.betula.AbstractCFKMeansInitialization;
import elki.clustering.kmeans.initialization.betula.CFKPlusPlusLeaves;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.features.ClusterFeature;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.math.linearalgebra.VMath;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * BIRCH/BETULA-based clustering algorithm that simply treats the leafs of the
 * CFTree as clusters.
 * <p>
 * References:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees<br>
 * Information Systems
 * 
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @depend - - - CFTree
 */
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS22")
public class BetulaLloydKMeans extends AbstractKMeans<NumberVector, KMeansModel> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(BetulaLloydKMeans.class);

  /**
   * CFTree factory.
   */
  CFTree.Factory<?> cffactory;

  /**
   * k-means++ initialization
   */
  AbstractCFKMeansInitialization initialization;

  /**
   * Store ids
   */
  boolean storeIds = false;

  /**
   * Ignore weight
   */
  boolean ignoreWeight = false;

  /**
   * Number of distance caclulations
   */
  long diststat = 0;

  /**
   * Constructor.
   *
   * @param k Number of clusters
   * @param maxiter Maximum number of iterations
   * @param cffactory CFTree factory
   * @param initialization Initialization method for k-means
   * @param storeIds Store IDs to avoid reassignment cost
   * @param ignoreWeight Ignore the leaf weights
   */
  public BetulaLloydKMeans(int k, int maxiter, CFTree.Factory<?> cffactory, AbstractCFKMeansInitialization initialization, boolean storeIds, boolean ignoreWeight) {
    super(k, maxiter, null);
    this.cffactory = cffactory;
    this.initialization = initialization;
    this.storeIds = storeIds;
    this.ignoreWeight = ignoreWeight;
  }

  /**
   * Run the clustering algorithm.
   *
   * @param relation Input data
   * @return Clustering
   */
  @Override
  public Clustering<KMeansModel> run(Relation<NumberVector> relation) {
    CFTree<?> tree = cffactory.newTree(relation.getDBIDs(), relation, storeIds);
    ArrayList<? extends ClusterFeature> cfs = tree.getLeaves();

    Duration modeltime = LOG.newDuration(getClass().getName() + ".modeltime").begin();
    int[] assignment = new int[cfs.size()], weights = new int[k];
    Arrays.fill(assignment, -1);
    double[][] means = kmeans(cfs, assignment, weights, tree);
    LOG.statistics(modeltime.end());
    ModifiableDBIDs[] ids = new ModifiableDBIDs[k];
    for(int i = 0; i < k; i++) {
      ids[i] = DBIDUtil.newArray(weights[i]);
    }
    double[] varsum = new double[k];
    if(storeIds) {
      for(int i = 0; i < assignment.length; i++) {
        ClusterFeature cfsi = cfs.get(i);
        final double[] mean = means[assignment[i]];
        double s = cfsi.sumdev();
        for(int d = 0; d < means[0].length; d++) {
          final double dx = cfsi.centroid(d) - mean[d];
          s += cfsi.getWeight() * dx * dx;
        }
        varsum[assignment[i]] += s;
        ids[assignment[i]].addDBIDs(tree.getDBIDs(cfsi));
      }
    }
    else {
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
    }
    LOG.statistics(new LongStatistic(getClass().getName() + ".distance-computations", diststat));
    LOG.statistics(new DoubleStatistic(getClass().getName() + ".variance-sum", VMath.sum(varsum)));
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
   * @param cfs Cluster features
   * @param assignment Cluster assignment of each CF
   * @param weights Cluster weight output
   * @param tree CF tree
   * @return Cluster means
   */
  private double[][] kmeans(ArrayList<? extends ClusterFeature> cfs, int[] assignment, int[] weights, CFTree<?> tree) {
    double[][] means = initialization.chooseInitialMeans(tree, cfs, k);
    for(int i = 1; i <= maxiter || maxiter < 0; i++) {
      long prevdiststat = diststat;
      means = i == 1 ? means : means(assignment, means, cfs, weights);
      if(i > 1 && LOG.isStatistics()) {
        // This function is only correct after updating the means:
        double varsum = VMath.sum(calculateVariances(assignment, means, cfs, weights));
        LOG.statistics(new DoubleStatistic(getClass().getName() + "." + (i - 1) + ".variance-sum", varsum));
      }
      int changed = assignToNearestCluster(assignment, means, cfs, weights);
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(getClass().getName() + "." + i + ".reassigned", changed));
        if(diststat > prevdiststat) {
          LOG.statistics(new LongStatistic(getClass().getName() + "." + i + ".distance-computations", diststat - prevdiststat));
        }
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
   * @param weights Cluster weights
   * @return Means of clusters.
   */
  private double[][] means(int[] assignment, double[][] means, ArrayList<? extends ClusterFeature> cfs, int[] weights) {
    Arrays.fill(weights, 0);
    double[][] newMeans = new double[k][];
    for(int i = 0; i < assignment.length; i++) {
      int c = assignment[i];
      final ClusterFeature cf = cfs.get(i);
      int d = cf.getDimensionality();
      int n = cf.getWeight();
      if(newMeans[c] == null) {
        newMeans[c] = new double[d];
        for(int j = 0; j < d; j++) {
          newMeans[c][j] = cf.centroid(j) * n;
        }
      }
      else {
        for(int j = 0; j < d; j++) {
          newMeans[c][j] += cf.centroid(j) * n;
        }
      }
      weights[c] += n;
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
   * @param cfs Cluster features
   * @param weights Cluster weights (output)
   * @return Number of reassigned elements
   */
  private int assignToNearestCluster(int[] assignment, double[][] means, ArrayList<? extends ClusterFeature> cfs, int[] weights) {
    Arrays.fill(weights, 0);
    int changed = 0;
    for(int i = 0; i < cfs.size(); i++) {
      ClusterFeature cfsi = cfs.get(i);
      double[] mean = new double[cfsi.getDimensionality()];
      for(int j = 0; j < mean.length; j++) {
        mean[j] = cfsi.centroid(j);
      }
      double mindist = distance(mean, means[0]);
      int minIndex = 0;
      for(int j = 1; j < k; j++) {
        double dist = distance(mean, means[j]);
        if(dist < mindist) {
          minIndex = j;
          mindist = dist;
        }
      }
      if(assignment[i] != minIndex) {
        changed++;
        assignment[i] = minIndex;
      }
      weights[minIndex] += ignoreWeight ? 1 : cfsi.getWeight();
    }
    return changed;
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
  protected double[] calculateVariances(int[] assignment, double[][] means, ArrayList<? extends ClusterFeature> cfs, int[] weights) {
    double[] ss = new double[k];
    for(int i = 0; i < assignment.length; i++) {
      ClusterFeature cfsi = cfs.get(i);
      final double[] mean = means[assignment[i]];
      double s = ignoreWeight ? cfsi.sumdev() / cfsi.getWeight() : cfsi.sumdev();
      for(int d = 0; d < means[0].length; d++) {
        final double dx = cfsi.centroid(d) - mean[d];
        s += (ignoreWeight ? 1 : cfsi.getWeight()) * dx * dx;
      }
      ss[assignment[i]] += s;
    }
    return ss;
  }

  /**
   * Updates statistics and calculates distance between two Objects based on
   * selected criteria.
   * <p>
   * Note: specializing this rather than calling SquaredEuclideanDistance was
   * much faster, as we can avoid wrapping the array.
   * 
   * @param x Point x
   * @param y Point y
   * @return distance
   */
  private double distance(NumberVector x, double[] y) {
    ++diststat;
    double v = 0;
    for(int i = 0; i < y.length; i++) {
      final double d = x.doubleValue(i) - y[i];
      v += d * d;
    }
    return v;
  }

  /**
   * Updates statistics and calculates distance between two Objects based on
   * selected criteria.
   * 
   * @param x Point x
   * @param y Point y
   * @return distance
   */
  private double distance(double[] x, double[] y) {
    ++diststat;
    double v = 0;
    for(int i = 0; i < x.length; i++) {
      final double d = x[i] - y[i];
      v += d * d;
    }
    return v;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Andreas Lang
   */
  public static class Par extends AbstractKMeans.Par<NumberVector> {
    /**
     * Option to store ids rather than reassigning.
     */
    public static final OptionID STORE_IDS_ID = new OptionID("betula.storeids", "Store IDs when building the tree, and use when assigning to leaves.");

    /**
     * Ignore cluster weights (naive approach)
     */
    public static final OptionID IGNORE_WEIGHT_ID = new OptionID("betulakm.naive", "Treat leaves as single points, not weighted points.");

    /**
     * CFTree factory.
     */
    CFTree.Factory<?> cffactory;

    /**
     * initialization method
     */
    AbstractCFKMeansInitialization initialization;

    /**
     * Store ids
     */
    boolean storeIds = false;

    /**
     * Ignore weight
     */
    boolean ignoreWeight = false;

    @Override
    public void configure(Parameterization config) {
      cffactory = config.tryInstantiate(CFTree.Factory.class);
      super.getParameterK(config);
      super.getParameterMaxIter(config);
      new ObjectParameter<AbstractCFKMeansInitialization>(AbstractKMeans.INIT_ID, AbstractCFKMeansInitialization.class, CFKPlusPlusLeaves.class) //
          .grab(config, x -> initialization = x);
      new Flag(STORE_IDS_ID).grab(config, x -> storeIds = x);
      new Flag(IGNORE_WEIGHT_ID).grab(config, x -> ignoreWeight = x);
    }

    @Override
    public BetulaLloydKMeans make() {
      return new BetulaLloydKMeans(k, maxiter, cffactory, initialization, storeIds, ignoreWeight);
    }
  }
}
