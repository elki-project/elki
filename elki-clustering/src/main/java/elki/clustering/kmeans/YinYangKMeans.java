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

import java.util.Arrays;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.logging.statistics.LongStatistic;
import elki.math.MathUtil;
import elki.math.linearalgebra.VMath;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Yin-Yang k-Means Clustering. This approach has one bound for each group of
 * cluster centers, and sits in-between of Hamerly (one bound only) and Elkan
 * (one bound for each center).
 * <p>
 * Reference:
 * <p>
 * Y. Ding, Y. Zhao, X. Shen, M, Musuvathi, T. Mytkowicz<br>
 * Yinyang K-Means: A Drop-In Replacement of the Classic K-Means with Consistent
 * Speedup<br>
 * Proc. International Conference on Machine Learning (ICML 2015)
 *
 * @author Minh Nhat Nguyen
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @param <V> Vector type
 */
@Reference(authors = "Y. Ding, Y. Zhao, X. Shen, M, Musuvathi, T. Mytkowicz", //
    title = "Yinyang K-Means: A Drop-In Replacement of the Classic K-Means with Consistent Speedup", //
    booktitle = "Proc. International Conference on Machine Learning (ICML 2015)", //
    url = "http://proceedings.mlr.press/v37/ding15.html", //
    bibkey = "DBLP:conf/icml/DingZSMM15")
public class YinYangKMeans<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(YinYangKMeans.class);

  /**
   * Use only up to 5 iterations of kmeans for grouping initial centers.
   */
  private static final int GROUP_KMEANS_MAXITER = 5;

  /**
   * Number of cluster center groups t
   */
  private int t;

  /**
   * Constructor.
   *
   * @param k Number of clusters
   * @param maxiter Maximum number of iterations
   * @param initializer Initialization method
   * @param t Number of cluster center groups for pruning
   */
  public YinYangKMeans(int k, int maxiter, KMeansInitialization initializer, int t) {
    super(k, maxiter, initializer);
    this.t = t;
  }

  @Override
  public Clustering<KMeansModel> run(Relation<V> rel) {
    Instance instance = new Instance(rel, getDistance(), initialMeans(rel), t);
    instance.run(maxiter);
    return instance.buildResult();
  }

  /**
   * Instance for a particular data set.
   * 
   * @author Minh Nhat Nguyen
   * @author Erich Schubert
   */
  protected static class Instance extends AbstractKMeans.Instance {
    /**
     * Center list for each group
     */
    int[][] groups;

    /**
     * Maximum distance moved within each group.
     */
    double[] gdrift;

    /**
     * Distance moved by each center.
     */
    double[] cdrift;

    /**
     * Current cluster sum.
     */
    double[][] sums;

    /**
     * Group label of each mean
     */
    int[] glabel = new int[k];

    /**
     * Upper bound
     */
    WritableDoubleDataStore upper;

    /**
     * Lower bounds
     */
    WritableDataStore<double[]> lower;

    /**
     * Constructor.
     *
     * @param relation Data relation
     * @param df Distance function
     * @param means Initial means
     * @param t Number of groups to use
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means, int t) {
      super(relation, df, means);
      t = t > 0 ? (t < k ? t : k) : (k >= 10 ? k / 10 : k / 2);
      this.upper = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.POSITIVE_INFINITY);
      this.lower = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, double[].class);
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        lower.put(it, new double[t]); // Filled with 0.
      }
      final int dim = means[0].length;
      this.cdrift = new double[k];
      this.sums = new double[k][dim]; // center sums to calculate mean
      this.gdrift = new double[t];
    }

    @Override
    public void run(int maxiter) {
      this.groups = groupKMeans(gdrift.length /* = t */);
      super.run(maxiter);
    }

    /**
     * Groups the initial centers into t groups.
     * 
     * @param t Number of groups
     * @return a list of groups containing mean indices.
     */
    private int[][] groupKMeans(int t) {
      if(t <= 1) {
        Arrays.fill(glabel, 0);
        return new int[][] { MathUtil.sequence(0, means.length) };
      }
      long before = diststat;
      double[][] gmean = new double[t][];
      int[] gweight = new int[t];
      initialGroupAssignment(t, gmean, gweight);
      // At most five iterations of center refinement:
      for(int it = 1; it <= GROUP_KMEANS_MAXITER; it++) {
        if(!updateGroupAssignment(t, gmean, gweight)) {
          break;
        }
      }
      // Invert to static groupid -> means table
      int[][] meanGroups = new int[t][];
      for(int i = 0; i < t; i++) {
        meanGroups[i] = new int[gweight[i]];
        for(int j = 0, p = 0; j < k; j++) {
          if(glabel[j] == i) {
            meanGroups[i][p++] = j;
          }
        }
      }
      if(getLogger().isStatistics()) {
        getLogger().statistics(new LongStatistic(key + ".yinyang-grouping.distance-computations", diststat - before));
      }
      return meanGroups;
    }

    /**
     * Initial k-means assignment for centers to groups.
     *
     * @param t Number of groups
     * @param scratch Scratch space for means
     * @param gweight group weights
     */
    private void initialGroupAssignment(int t, double[][] scratch, int[] gweight) {
      // Initial means
      for(int i = 0; i < t; i++) {
        scratch[i] = means[i].clone();
        glabel[i] = i;
      }
      Arrays.fill(gweight, 1);
      // remaining points
      for(int i = t; i < k; i++) {
        final double[] cur = means[i];
        int best = 0;
        double bestd = distance(cur, means[0]);
        for(int j = 1; j < t; j++) {
          double d = distance(cur, means[j]);
          if(d < bestd) {
            bestd = d;
            best = j;
          }
        }
        VMath.plusEquals(scratch[best], cur);
        glabel[i] = best;
        ++gweight[best];
      }
      // scale centers
      for(int i = 0; i < t; i++) {
        VMath.timesEquals(scratch[i], 1. / gweight[i]);
      }
    }

    /**
     * Perform one step of Voronoi refinement.
     *
     * @param t Number of groups
     * @param gmeans current group means
     * @param gweight Group weight
     * @return true if changed
     */
    private boolean updateGroupAssignment(int t, double[][] gmeans, int[] gweight) {
      boolean changed = false;
      for(int i = 0; i < t; i++) {
        Arrays.fill(sums[i], 0);
      }
      Arrays.fill(gweight, 0);
      for(int i = 0; i < k; i++) {
        final double[] cur = means[i];
        final int prev = glabel[i];
        double bestd = distance(cur, gmeans[0]);
        int best = 0;
        for(int j = 1; j < t; j++) {
          double d = distance(cur, gmeans[j]);
          if(d < bestd || (d == bestd && j == prev)) {
            best = j;
            bestd = d;
          }
        }
        VMath.plusEquals(sums[best], cur);
        ++gweight[best];
        glabel[i] = best;
        changed |= best != prev;
      }
      // Scale centers
      for(int i = 0; i < t; i++) {
        if(gweight[i] > 0) {
          VMath.overwriteTimes(gmeans[i], sums[i], 1. / gweight[i]);
        }
      }
      return changed;
    }

    @Override
    protected int iterate(int iteration) {
      if(iteration == 1) {
        return initialAssignToNearestCluster();
      }
      updateCenters();
      return assignToNearestCluster();
    }

    /**
     * Update centers and how much they moved.
     */
    private void updateCenters() {
      final int dim = means[0].length;
      double[] oldmean = new double[dim];
      for(int g = 0; g < groups.length; g++) {
        double gd = 0;
        for(int i : groups[g]) {
          final int size = clusters.get(i).size();
          if(size > 0) {
            double[] sum = sums[i], mean = means[i];
            System.arraycopy(mean, 0, oldmean, 0, dim);
            VMath.overwriteTimes(mean, sum, 1. / size);
            final double d = cdrift[i] = sqrtdistance(mean, oldmean);
            gd = d > gd ? d : gd; // max group drift
          }
        }
        gdrift[g] = gd;
      }
    }

    /**
     * Reassign objects, but avoid unnecessary computations based on their
     * bounds.
     * 
     * @return number of objects reassigned
     */
    @Override
    protected int assignToNearestCluster() {
      final int t = gdrift.length;
      int changed = 0;
      double[] prevlb = new double[t];

      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        NumberVector cur = relation.get(it);
        int prev = assignment.intValue(it);
        double[] lbs = lower.get(it);
        System.arraycopy(lbs, 0, prevlb, 0, lbs.length);

        // Update the upper bound
        final double drift = cdrift[prev];
        if(drift > 0) {
          upper.increment(it, drift);
        }

        double minlb = Double.POSITIVE_INFINITY;
        // Update lower bounds with the maximum distance moved within each group
        for(int g = 0; g < t; g++) {
          double lb = lbs[g] -= gdrift[g];
          minlb = lb < minlb ? lb : minlb;
        }

        // Global filter
        double ub = upper.doubleValue(it);
        if(minlb >= ub) {
          continue;
        }

        // tighten ub(x) and check again
        upper.put(it, ub = sqrtdistance(cur, means[prev]));
        // Global filter with ub tight
        if(minlb >= ub) {
          continue;
        }

        int best = prev;
        // distance to second closest:
        for(int g = 0; g < t; ++g) {
          double lb = lbs[g];
          // Group filter
          if(lb >= ub) {
            continue;
          }
          double plb = prevlb[g];
          double sc = Double.POSITIVE_INFINITY;
          for(int i : groups[g]) {
            if(i == prev) { // Already computed above
              continue;
            }
            // Local filter.
            if(sc < plb - cdrift[i]) {
              continue;
            }
            double di = sqrtdistance(cur, means[i]);
            if(di < sc) { // at least second closest
              if(di < ub) { // closest
                lb = sc = ub; // previous closest is now second
                ub = di;
                best = i;
              }
              else {
                sc = di;
              }
            }
          }
          lbs[g] = sc;
        }

        if(prev != best) {
          upper.put(it, ub);
          clusters.get(assignment.intValue(it)).remove(it);
          clusters.get(best).add(it);
          plusMinusEquals(sums[best], sums[prev], cur);
          assignment.put(it, best);
          ++changed;
        }
      }
      return changed;
    }

    /**
     * Perform initial cluster assignment,
     * 
     * @return number of changes (i.e. relation size)
     */
    private int initialAssignToNearestCluster() {
      assert k == means.length;
      for(DBIDIter id = relation.iterDBIDs(); id.valid(); id.advance()) {
        NumberVector point = relation.get(id);
        double[] lower = this.lower.get(id);
        double min = Double.POSITIVE_INFINITY;
        int globalindex = 0;

        for(int g = 0; g < groups.length; g++) {
          final int[] group = groups[g];
          if(group.length == 0) {
            continue;
          }
          // First center in group
          double min1 = distance(point, means[group[0]]);
          double min2 = Double.POSITIVE_INFINITY;
          int best = group[0];
          // remaining centers in group
          for(int c = 1; c < group.length; c++) {
            int center = group[c];
            double dist = distance(point, means[center]);
            if(dist < min1) {
              min2 = min1;
              best = center;
              min1 = dist;
            }
            else if(dist < min2) {
              min2 = dist;
            }
          }
          // For the triangle inequality, we need Euclidean not squared
          min1 = isSquared ? Math.sqrt(min1) : min1;
          min2 = min2 < Double.POSITIVE_INFINITY ? (isSquared ? Math.sqrt(min2) : min2) : min1;

          if(min1 < min) {
            if(globalindex != -1) {
              lower[glabel[globalindex]] = min;
            }
            min = min1;
            globalindex = best;
            lower[g] = min2;
          }
          else {
            lower[g] = min1;
          }
        }
        clusters.get(globalindex).add(id);
        assignment.put(id, globalindex);
        upper.put(id, min);
        plusEquals(sums[globalindex], point);
      }

      return relation.size();
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Minh Nhat Nguyen
   */
  public static class Par<V extends NumberVector> extends AbstractKMeans.Par<V> {
    /**
     * Parameter to specify t the number of centroid groups.
     */
    public static final OptionID T_ID = new OptionID("kmeans.yinyang.t", "The number of groups to use for bounding the centroids.");

    /**
     * Number of groups in the initial clustering of the centroids.
     */
    protected int t;

    @Override
    protected boolean needsMetric() {
      return true;
    }

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      int deft = k > 10 ? k / 10 : k > 1 ? k / 2 : 1;
      new IntParameter(T_ID) //
          .setDefaultValue(deft) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT)//
          .grab(config, x -> t = x);
    }

    @Override
    public YinYangKMeans<V> make() {
      return new YinYangKMeans<>(k, maxiter, initializer, t);
    }
  }
}
