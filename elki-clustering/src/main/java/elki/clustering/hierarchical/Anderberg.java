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
package elki.clustering.hierarchical;

import java.util.Arrays;

import elki.Algorithm;
import elki.clustering.hierarchical.linkage.CentroidLinkage;
import elki.clustering.hierarchical.linkage.Linkage;
import elki.clustering.hierarchical.linkage.SingleLinkage;
import elki.clustering.hierarchical.linkage.WardLinkage;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDUtil;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * This is a modification of the classic AGNES algorithm for hierarchical
 * clustering using a nearest-neighbor heuristic for acceleration.
 * <p>
 * Instead of scanning the matrix (with cost O(n²)) to find the minimum, the
 * nearest neighbor of each object is remembered. On the downside, we need to
 * check these values at every merge, and it may now cost O(n²) to perform a
 * merge, so there is no worst-case advantage to this approach. The average case
 * however improves from O(n³) to O(n²), which yields a considerable
 * improvement in running time.
 * <p>
 * This optimization is attributed to M. R. Anderberg.
 * <p>
 * Reference:
 * <p>
 * M. R. Anderberg<br>
 * Hierarchical Clustering Methods<br>
 * Cluster Analysis for Applications<br>
 * ISBN: 0120576503
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @composed - - - LinkageMethod
 * @composed - - - ClusterMergeHistoryBuilder
 *
 * @param <O> Object type
 */
@Reference(authors = "M. R. Anderberg", //
    title = "Hierarchical Clustering Methods", //
    booktitle = "Cluster Analysis for Applications", //
    bibkey = "books/academic/Anderberg73/Ch6")
@Priority(Priority.RECOMMENDED)
public class Anderberg<O> extends AGNES<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(Anderberg.class);

  /**
   * Constructor.
   *
   * @param distance Distance function to use
   * @param linkage Linkage method
   */
  public Anderberg(Distance<? super O> distance, Linkage linkage) {
    super(distance, linkage);
  }

  @Override
  public ClusterMergeHistory run(Relation<O> relation) {
    if(SingleLinkage.class.isInstance(linkage)) {
      LOG.verbose("Notice: SLINK is a much faster algorithm for single-linkage clustering!");
    }
    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).distanceQuery();
    ClusterDistanceMatrix mat = AGNES.initializeDistanceMatrix(ids, dq, linkage);
    return new Instance(linkage).run(mat, new ClusterMergeHistoryBuilder(ids, distance.isSquared()));
  }

  /**
   * Main worker instance of Anderberg's algorithm.
   *
   * @author Erich Schubert
   */
  public static class Instance extends AGNES.Instance {
    /**
     * Cache: best distance
     */
    protected double[] bestd;

    /**
     * Cache: index of best distance
     */
    protected int[] besti;

    /**
     * Constructor.
     *
     * @param linkage Linkage method
     */
    public Instance(Linkage linkage) {
      super(linkage);
    }

    @Override
    public ClusterMergeHistory run(ClusterDistanceMatrix mat, ClusterMergeHistoryBuilder builder) {
      final int size = mat.size;
      this.mat = mat;
      this.builder = builder;
      this.end = size;
      this.bestd = new double[size];
      this.besti = new int[size];
      initializeNNCache(mat.matrix, bestd, besti);

      // Repeat until everything merged into 1 cluster
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", size - 1, LOG) : null;
      for(int i = 1; i < size; i++) {
        end = shrinkActiveSet(mat.clustermap, end, findMerge());
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
      return builder.complete();
    }

    /**
     * Initialize the NN cache.
     *
     * @param scratch Scratch space
     * @param bestd Best distance
     * @param besti Best index
     */
    protected static void initializeNNCache(double[] scratch, double[] bestd, int[] besti) {
      final int size = bestd.length;
      Arrays.fill(bestd, Double.POSITIVE_INFINITY);
      Arrays.fill(besti, -1);
      besti[0] = Integer.MAX_VALUE; // invalid, but not deactivated
      for(int x = 1, p = 0; x < size; x++) {
        assert p == ClusterDistanceMatrix.triangleSize(x);
        double bestdx = Double.POSITIVE_INFINITY;
        int bestix = -1;
        for(int y = 0; y < x; y++) {
          final double v = scratch[p++];
          if(v < bestdx) {
            bestdx = v;
            bestix = y;
          }
        }
        assert 0 <= bestix && bestix < x;
        bestd[x] = bestdx;
        besti[x] = bestix;
      }
    }

    /**
     * Perform the next merge step.
     * <p>
     * Due to the cache, this is now O(n) each time, instead of O(n*n).
     *
     * @return x, for shrinking the working set.
     */
    @Override
    protected int findMerge() {
      double mindist = Double.POSITIVE_INFINITY;
      int x = -1, y = -1;
      // Find minimum:
      for(int cx = 1; cx < end; cx++) {
        // Skip if object has already joined a cluster:
        final int cy = besti[cx];
        if(cy < 0) {
          continue;
        }
        final double dist = bestd[cx];
        if(dist <= mindist) { // Prefer later on ==, to truncate more often.
          mindist = dist;
          x = cx;
          y = cy;
        }
      }
      merge(mindist, x, y);
      return x;
    }

    @Override
    protected void merge(double mindist, int x, int y) {
      assert y < x;
      final int xx = mat.clustermap[x], yy = mat.clustermap[y];
      final int sizex = builder.getSize(xx), sizey = builder.getSize(yy);
      // Since y < x, prefer keeping y, dropping x.
      int zz = builder.strictAdd(xx, linkage.restore(mindist, builder.isSquared), yy);
      assert builder.getSize(zz) == sizex + sizey;
      mat.clustermap[y] = zz;
      mat.clustermap[x] = besti[x] = -1; // Deactivate removed cluster.
      updateMatrix(mindist, x, y, sizex, sizey);
      if(y > 0) {
        findBest(mat.matrix, bestd, besti, y);
      }
    }

    @Override
    protected void updateMatrix(double mindist, int x, int y, int sizex, int sizey) {
      final int xbase = ClusterDistanceMatrix.triangleSize(x);
      final int ybase = ClusterDistanceMatrix.triangleSize(y);
      double[] scratch = mat.matrix;

      // Write to (y, j), with j < y
      int j = 0;
      for(; j < y; j++) {
        if(mat.clustermap[j] < 0) {
          continue;
        }
        final int sizej = builder.getSize(mat.clustermap[j]);
        final int yb = ybase + j;
        final double d = scratch[yb] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[yb], sizej, mindist);
        updateCache(scratch, bestd, besti, x, y, j, d);
      }
      j++; // Skip y
      // Write to (j, y), with y < j < x
      int jbase = ClusterDistanceMatrix.triangleSize(j);
      for(; j < x; jbase += j++) {
        if(mat.clustermap[j] < 0) {
          continue;
        }
        final int sizej = builder.getSize(mat.clustermap[j]);
        final int jb = jbase + y;
        final double d = scratch[jb] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[jb], sizej, mindist);
        updateCache(scratch, bestd, besti, x, y, j, d);
      }
      jbase += j++; // Skip x
      // Write to (j, y), with y < x < j
      for(; j < end; jbase += j++) {
        if(mat.clustermap[j] < 0) {
          continue;
        }
        final int sizej = builder.getSize(mat.clustermap[j]);
        final int jb = jbase + y;
        final double d = scratch[jb] = linkage.combine(sizex, scratch[jbase + x], sizey, scratch[jb], sizej, mindist);
        updateCache(scratch, bestd, besti, x, y, j, d);
      }
    }

    /**
     * Update the cache.
     *
     * @param scratch Scratch matrix
     * @param bestd Best distance
     * @param besti Best index
     * @param x First cluster
     * @param y Second cluster, {@code y < x}
     * @param j Updated value d(y, j)
     * @param d New distance
     */
    protected static void updateCache(double[] scratch, double[] bestd, int[] besti, int x, int y, int j, double d) {
      assert y < x;
      // New best
      if(y < j && d <= bestd[j]) {
        bestd[j] = d;
        besti[j] = y;
        return;
      }
      // Needs slow update.
      if(besti[j] == x || besti[j] == y) {
        findBest(scratch, bestd, besti, j);
      }
    }

    /**
     * Find the best in a row of the triangular matrix.
     *
     * @param scratch Scratch matrix
     * @param bestd Best distances cache
     * @param besti Best indexes cache
     * @param j Row to update
     */
    protected static void findBest(double[] scratch, double[] bestd, int[] besti, int j) {
      // The distance has increased, we may no longer be the best merge.
      double bestdj = Double.POSITIVE_INFINITY;
      int bestij = -1;
      for(int i = 0,
          o = ClusterDistanceMatrix.triangleSize(j); i < j; i++, o++) {
        if(besti[i] < 0) {
          continue;
        }
        final double dist = scratch[o];
        if(dist <= bestdj) {
          bestdj = dist;
          bestij = i;
        }
      }
      assert bestij < j;
      bestd[j] = bestdj;
      besti[j] = bestij;
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Current linkage in use.
     */
    protected Linkage linkage;

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Linkage>(AGNES.Par.LINKAGE_ID, Linkage.class) //
          .setDefaultValue(WardLinkage.class) //
          .grab(config, x -> linkage = x);
      Class<? extends Distance<?>> defaultD = (linkage instanceof WardLinkage || linkage instanceof CentroidLinkage) //
          ? SquaredEuclideanDistance.class : EuclideanDistance.class;
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, defaultD) //
          .grab(config, x -> distance = x);
    }

    @Override
    public Anderberg<O> make() {
      return new Anderberg<>(distance, linkage);
    }
  }
}
