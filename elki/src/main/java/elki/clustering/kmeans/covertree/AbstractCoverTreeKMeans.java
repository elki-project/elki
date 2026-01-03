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

package elki.clustering.kmeans.covertree;

import java.util.Arrays;

import elki.clustering.kmeans.AbstractKMeans;
import elki.clustering.kmeans.covertree.KMeansCoverTree.Node;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.statistics.LongStatistic;
import elki.math.linearalgebra.VMath;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Abstract class for all variants of cover-tree k-means.
 * <p>
 * Reference:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * Accelerating k-Means Clustering with Cover Trees<br>
 * Int. Conf. on Similarity Search and Applications, SISAP 2023
 * 
 * @author Andreas Lang
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "Accelerating k-Means Clustering with Cover Trees", //
    booktitle = "Int. Conf. on Similarity Search and Applications, SISAP 2023", //
    url = "https://doi.org/10.1007/978-3-031-46994-7_13", //
    bibkey = "DBLP:conf/sisap/LangS23")
public abstract class AbstractCoverTreeKMeans<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * Flag whether to compute the final variance statistic.
   */
  protected boolean varstat = false;

  /**
   * Expansion factor for the cover tree
   */
  protected double expansion = 1.3;

  /**
   * Truncate threshold for the cover tree
   */
  protected int trunc = 10;

  /**
   * Constructor
   * 
   * @param k number of clusters
   * @param maxiter maximum number of iterations
   * @param initializer k-means initializer
   * @param varstat Flag for Variance statistics at the end
   * @param expansion Tree expansion factor
   * @param trunc Tree truncate threshold
   */
  public AbstractCoverTreeKMeans(int k, int maxiter, KMeansInitialization initializer, boolean varstat, double expansion, int trunc) {
    super(SquaredEuclideanDistance.STATIC, k, maxiter, initializer);
    this.varstat = varstat;
    this.expansion = expansion;
    this.trunc = trunc;
  }

  /**
   * Inner Class for Cover Tree k-means
   * 
   * @author Andreas Lang
   */
  protected abstract static class Instance extends AbstractKMeans.Instance {
    KMeansCoverTree<? extends NumberVector> tree;

    /**
     * inter cluster distances
     */
    double[][] cdist;

    /**
     * squared inter cluster distances
     */
    double[][] scdist;

    /**
     * Statistic for pruning at singleton level
     */
    long singletonstatPrune;

    /**
     * Statistic for filtering at singleton level
     */
    long singletonstatFilter;

    /**
     * Statistic for filtering with inter cluster distances at singleton level
     */
    long singletonstatIcDist;

    /**
     * Statistic for pruning at node level
     */
    long nodestatPrune;

    /**
     * Statistic for filtering at node level
     */
    long nodestatFilter;

    /**
     * Statistic for filtering with inter cluster distances at node level
     */
    long nodestatIcDist;

    /**
     * Node Manager class for managing cluster assignments of nodes
     */
    NodeManager nodeManager;

    /**
     * Constructor.
     *
     * @param relation Relation
     * @param df Distance function
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means, KMeansCoverTree<? extends NumberVector> tree) {
      super(relation, df, means);
      this.tree = tree;
      cdist = new double[means.length][means.length];
      scdist = new double[means.length][means.length];
      nodeManager = new NodeManager(k, means[0].length, clusters, assignment, tree, relation);
    }

    /**
     * Prune cluster candidates
     * 
     * @param dists distance of Node to clusters
     * @param cand List of candidates
     * @param nodeUpper Upper bound on distances
     * @param alive number of valid candidates
     * @return new number of valid candidates
     */
    protected int pruneD(double[] dists, int[] cand, double nodeUpper, int alive) {
      // candidate 0 and 1 are the neaest and can not be pruned
      for(int i = 2; i < alive;) {
        if(dists[i] > nodeUpper) {
          --alive;
          while(alive > i && dists[alive] > nodeUpper) {
            --alive;
          }
          swap(dists, i, alive, cand);
        }
        i++;
      }
      return alive;
    }

    /**
     * Swap cluster candidates
     * 
     * @param i index candidate a
     * @param j index candidate b
     * @param cand array with cluster candidates
     */
    protected void swap(int i, int j, int[] cand) {
      final int swap = cand[i];
      cand[i] = cand[j];
      cand[j] = swap;
    }

    /**
     * Swap cluster candidates and distances to them
     * 
     * @param dists array with distances to cluster centers
     * @param i index candidate a
     * @param j index candidate b
     * @param cand array with cluster candidates
     */
    protected void swap(double[] dists, int i, int j, int[] cand) {
      final int swap = cand[i];
      cand[i] = cand[j];
      cand[j] = swap;

      final double swapD = dists[i];
      dists[i] = dists[j];
      dists[j] = swapD;
    }

    /**
     * Separation of means.
     *
     * @param cdist Pairwise separation output (as sqrt/2)
     * @param scdist Pairwise separation output (as squared/4)
     */
    protected void combinedSeperation(double[][] cdist, double[][] scdist) {
      final int k = means.length;
      for(int i = 1; i < k; i++) {
        double[] mi = means[i];
        for(int j = 0; j < i; j++) {
          scdist[i][j] = scdist[j][i] = 0.25 * distance(mi, means[j]);
          cdist[i][j] = cdist[j][i] = Math.sqrt(scdist[i][j]);
        }
      }
    }

    /**
     * Separation of means.
     *
     * @param sep Output array of separation (half-sqrt scaled)
     * @param cdist Pairwise separation output (as sqrt/2)
     * @param scdist Pairwise separation output (as squared/4)
     */
    protected void combinedSeperation(double sep[], double[][] cdist, double[][] scdist) {
      final int k = means.length;
      Arrays.fill(sep, Double.POSITIVE_INFINITY);
      for(int i = 1; i < k; i++) {
        double[] mi = means[i];
        for(int j = 0; j < i; j++) {
          scdist[i][j] = scdist[j][i] = 0.25 * distance(mi, means[j]);
          final double halfd = Math.sqrt(scdist[i][j]);
          cdist[i][j] = cdist[j][i] = halfd;
          sep[i] = (halfd < sep[i]) ? halfd : sep[i];
          sep[j] = (halfd < sep[j]) ? halfd : sep[j];
        }
      }
    }

    /**
     * Add all elements to the cluster list.
     */
    public void generateCover() {
      generateCover(tree.getRoot());
    }

    /**
     * Adds all elements of a node to the cluster lists
     * 
     * @param cur current node
     */
    public void generateCover(Node cur) {
      int clu = nodeManager.get(cur);
      if(clu >= 0) {
        addNode(cur, clu);
        return;
      }
      for(Node n : cur.children) {
        generateCover(n);
      }
      DBIDIter it = cur.singletons.iter();
      if(cur.children.isEmpty()) {
        clusters.get(nodeManager.get(it)).add(it);
      }
      it.advance();
      for(; it.valid(); it.advance()) {
        clusters.get(nodeManager.get(it)).add(it);
      }
    }

    /**
     * Compute means from cluster sums by averaging.
     * 
     * @param dst Output means
     * @param sums Input sums
     * @param prev Previous means, to handle empty clusters
     */
    protected void meansFromSumsCT(double[][] dst, double[][] sums, double[][] prev) {
      for(int i = 0; i < k; i++) {
        final int size = nodeManager.getSize(i);
        if(size == 0) {
          System.arraycopy(prev[i], 0, dst[i], 0, prev[i].length);
          continue;
        }
        VMath.overwriteTimes(dst[i], sums[i], 1. / size);
      }
    }

    /**
     * Add Node to cluster
     * 
     * @param n Node
     * @param cluster Cluster
     * @return number of changed Nodes
     */
    protected int addNode(Node n, int cluster) {
      int changed = 0;
      ModifiableDBIDs collect = DBIDUtil.newArray(100); // size
      tree.collectSubtree(n, collect);

      ModifiableDBIDs cluList = clusters.get(cluster);
      for(DBIDIter it = collect.iter(); it.valid(); it.advance()) {
        cluList.add(it);
        if(assignment.putInt(it, cluster) != cluster) {
          ++changed;
        }
      }
      return changed;
    }

    /**
     * Validate that every point is assigned to a cluster (expensive)
     */
    public boolean testSizes() {
      int count = 0;
      for(int i = 0; i < k; i++) {
        count += nodeManager.getSize(i);
      }
      int size = relation.size();
      return count == size;
    }

    /**
     * Test upper bounds
     * 
     * @param id Element
     * @param u upper boudn
     * @return Number of invalid elements
     */
    public int testUpper(DBIDRef id, double u) {
      int invalid = 0;
      double b = Double.MAX_VALUE;
      for(int i = 0; i < means.length; i++) {
        double d = distance(relation.get(id), means[i]);
        d = Math.sqrt(d);
        if(d < b) {
          b = d;
        }
      }
      if(b > u) {
        invalid = 1;
      }
      return invalid;
    }

    /**
     * Test lower bounds
     * 
     * @param id Element
     * @param u upper bound
     * @param l lower bound
     * @param clu cluster
     * @return Number of invalid elements
     */
    public int testLower(DBIDRef id, double u, double l, int clu) {
      int invalid = 0;
      double b = Double.MAX_VALUE;
      for(int i = 0; i < means.length; i++) {
        double d = distance(relation.get(id), means[i]);
        d = Math.sqrt(d);
        if(i != clu && d < b) {
          b = d;
        }
      }
      if(b < l) {
        invalid = 1;
      }
      return invalid;
    }

    /**
     * Print statistics on cover tree pruning
     */
    public void logStatistics() {
      Logging log = getLogger();
      log.statistics(new LongStatistic(key + ".singleton.filter", singletonstatFilter));
      log.statistics(new LongStatistic(key + ".singleton.prune", singletonstatPrune));
      log.statistics(new LongStatistic(key + ".singleton.icDist", singletonstatIcDist));

      log.statistics(new LongStatistic(key + ".node.filter", nodestatFilter));
      log.statistics(new LongStatistic(key + ".node.prune", nodestatPrune));
      log.statistics(new LongStatistic(key + ".node.icDist", nodestatIcDist));
    }
  }

  /**
   * Parameterization class.
   *
   * @author Andreas Lang
   */
  public abstract static class Par<V extends NumberVector> extends AbstractKMeans.Par<V> {
    /**
     * Truncate branches when they have less than this number of instances.
     */
    public static final OptionID TRUNCATE_ID = new OptionID("covertree.truncate", "Truncate tree when branches have less than this number of instances.");

    /**
     * Expansion rate of the tree (going upward).
     */
    public static final OptionID EXPANSION_ID = new OptionID("covertree.expansionrate", "Expansion rate of the tree (Default: 1.3).");

    /**
     * Expansion factor of the cover tree
     */
    double expansion = 1.4;

    /**
     * Truncate threshold for the cover tree
     */
    int trunc = 10;

    @Override
    protected boolean needsMetric() {
      return true;
    }

    /**
     * Get the cover tree parameters
     * 
     * @param config Parameterization
     */
    protected void getParameterSlack(Parameterization config) {
      new DoubleParameter(EXPANSION_ID, 1.3) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_DOUBLE) //
          .grab(config, x -> expansion = x);
      new IntParameter(TRUNCATE_ID, 10) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> trunc = x);
    }

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      super.getParameterVarstat(config);
      getParameterSlack(config);
    }

    @Override
    public abstract AbstractCoverTreeKMeans<V> make();
  }
}
