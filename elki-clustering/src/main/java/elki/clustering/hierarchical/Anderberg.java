/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
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
 * @composed - - - PointerHierarchyRepresentationBuilder
 *
 * @param <O> Object type
 */
@Reference(authors = "M. R. Anderberg", //
    title = "Hierarchical Clustering Methods", //
    booktitle = "Cluster Analysis for Applications", //
    bibkey = "books/academic/Anderberg73/Ch6")
@Priority(Priority.RECOMMENDED)
public class Anderberg<O> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(Anderberg.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Current linkage method in use.
   */
  protected Linkage linkage = WardLinkage.STATIC;

  /**
   * Constructor.
   *
   * @param distance Distance function to use
   * @param linkage Linkage method
   */
  public Anderberg(Distance<? super O> distance, Linkage linkage) {
    super();
    this.distance = distance;
    this.linkage = linkage;
  }

  /**
   * Run the algorithm
   *
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public PointerHierarchyRepresentationResult run(Relation<O> relation) {
    if(SingleLinkage.class.isInstance(linkage)) {
      LOG.verbose("Notice: SLINK is a much faster algorithm for single-linkage clustering!");
    }
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).distanceQuery();
    final DBIDs ids = relation.getDBIDs();
    MatrixParadigm mat = new MatrixParadigm(ids);
    final int size = ids.size();

    // Position counter - must agree with computeOffset!
    AGNES.initializeDistanceMatrix(mat, dq, linkage);

    // Arrays used for caching:
    double[] bestd = new double[size];
    int[] besti = new int[size];
    initializeNNCache(mat.matrix, bestd, besti);

    // Initialize space for result:
    PointerHierarchyRepresentationBuilder builder = new PointerHierarchyRepresentationBuilder(ids, dq.getDistance().isSquared());

    // Repeat until everything merged into 1 cluster
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", size - 1, LOG) : null;
    DBIDArrayIter ix = mat.ix;
    for(int i = 1, end = size; i < size; i++) {
      end = AGNES.shrinkActiveSet(ix, builder, end, //
          findMerge(end, mat, bestd, besti, builder));
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
    int p = 0;
    for(int x = 1; x < size; x++) {
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
    assert p == MatrixParadigm.triangleSize(size);
  }

  /**
   * Perform the next merge step.
   * <p>
   * Due to the cache, this is now O(n) each time, instead of O(n*n).
   *
   * @param size Data set size
   * @param mat Matrix paradigm
   * @param bestd Best distance
   * @param besti Index of best distance
   * @param builder Hierarchy builder
   * @return x, for shrinking the working set.
   */
  protected int findMerge(int size, MatrixParadigm mat, double[] bestd, int[] besti, PointerHierarchyRepresentationBuilder builder) {
    double mindist = Double.POSITIVE_INFINITY;
    int x = -1, y = -1;
    // Find minimum:
    for(int cx = 1; cx < size; cx++) {
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
    assert 0 <= y && y < x;
    merge(size, mat, bestd, besti, builder, mindist, x, y);
    return x;
  }

  /**
   * Execute the cluster merge.
   *
   * @param size Data set size
   * @param mat Matrix paradigm
   * @param bestd Best distance
   * @param besti Index of best distance
   * @param builder Hierarchy builder
   * @param mindist Distance that was used for merging
   * @param x First matrix position
   * @param y Second matrix position
   */
  protected void merge(int size, MatrixParadigm mat, double[] bestd, int[] besti, PointerHierarchyRepresentationBuilder builder, double mindist, int x, int y) {
    // Avoid allocating memory, by reusing existing iterators:
    final DBIDArrayIter ix = mat.ix.seek(x), iy = mat.iy.seek(y);
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Merging: " + DBIDUtil.toString(ix) + " -> " + DBIDUtil.toString(iy) + " " + mindist);
    }
    // Perform merge in data structure: x -> y
    assert y < x;
    // Since y < x, prefer keeping y, dropping x.
    builder.add(ix, linkage.restore(mindist, distance.isSquared()), iy);
    // Update cluster size for y:
    final int sizex = builder.getSize(ix), sizey = builder.getSize(iy);
    builder.setSize(iy, sizex + sizey);
    besti[x] = -1; // Deactivate removed cluster.
    updateMatrix(size, mat.matrix, iy, bestd, besti, builder, mindist, x, y, sizex, sizey);
    if(y > 0) {
      findBest(mat.matrix, bestd, besti, y);
    }
  }

  /**
   * Update the scratch distance matrix.
   *
   * @param size Data set size
   * @param scratch Scratch matrix.
   * @param ij Iterator to reuse
   * @param bestd Best distance
   * @param besti Index of best distance
   * @param builder Hierarchy builder
   * @param mindist Distance that was used for merging
   * @param x First matrix position
   * @param y Second matrix position
   * @param sizex Old size of first cluster, with {@code x > y}
   * @param sizey Old size of second cluster, with {@code y > x}
   */
  protected void updateMatrix(int size, double[] scratch, DBIDArrayIter ij, double[] bestd, int[] besti, PointerHierarchyRepresentationBuilder builder, double mindist, int x, int y, final int sizex, final int sizey) {
    // Update distance matrix. Note: miny < minx
    final int xbase = MatrixParadigm.triangleSize(x);
    final int ybase = MatrixParadigm.triangleSize(y);

    // Write to (y, j), with j < y
    int j = 0;
    for(; j < y; j++) {
      if(builder.isLinked(ij.seek(j))) {
        continue;
      }
      final int sizej = builder.getSize(ij);
      final int yb = ybase + j;
      final double d = scratch[yb] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[yb], sizej, mindist);
      updateCache(scratch, bestd, besti, x, y, j, d);
    }
    j++; // Skip y
    // Write to (j, y), with y < j < x
    int jbase = MatrixParadigm.triangleSize(j);
    for(; j < x; jbase += j++) {
      if(builder.isLinked(ij.seek(j))) {
        continue;
      }
      final int sizej = builder.getSize(ij);
      final int jb = jbase + y;
      final double d = scratch[jb] = linkage.combine(sizex, scratch[xbase + j], sizey, scratch[jb], sizej, mindist);
      updateCache(scratch, bestd, besti, x, y, j, d);
    }
    jbase += j++; // Skip x
    // Write to (j, y), with y < x < j
    for(; j < size; jbase += j++) {
      if(builder.isLinked(ij.seek(j))) {
        continue;
      }
      final int sizej = builder.getSize(ij);
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
    for(int i = 0, o = MatrixParadigm.triangleSize(j); i < j; i++, o++) {
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
