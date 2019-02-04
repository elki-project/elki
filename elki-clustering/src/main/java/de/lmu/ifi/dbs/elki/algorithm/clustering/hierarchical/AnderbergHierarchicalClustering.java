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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.linkage.Linkage;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.linkage.SingleLinkage;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.linkage.WardLinkage;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
public class AnderbergHierarchicalClustering<O> extends AbstractDistanceBasedAlgorithm<O, PointerHierarchyRepresentationResult> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(AnderbergHierarchicalClustering.class);

  /**
   * Current linkage method in use.
   */
  Linkage linkage = WardLinkage.STATIC;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to use
   * @param linkage Linkage method
   */
  public AnderbergHierarchicalClustering(DistanceFunction<? super O> distanceFunction, Linkage linkage) {
    super(distanceFunction);
    this.linkage = linkage;
  }

  /**
   * Run the algorithm
   *
   * @param db Database
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public PointerHierarchyRepresentationResult run(Database db, Relation<O> relation) {
    if(SingleLinkage.class.isInstance(linkage)) {
      LOG.verbose("Notice: SLINK is a much faster algorithm for single-linkage clustering!");
    }
    DistanceQuery<O> dq = db.getDistanceQuery(relation, getDistanceFunction());
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
    PointerHierarchyRepresentationBuilder builder = new PointerHierarchyRepresentationBuilder(ids, dq.getDistanceFunction().isSquared());

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
  private static void initializeNNCache(double[] scratch, double[] bestd, int[] besti) {
    final int size = bestd.length;
    Arrays.fill(bestd, Double.POSITIVE_INFINITY);
    Arrays.fill(besti, -1);
    for(int x = 0, p = 0; x < size; x++) {
      assert (p == MatrixParadigm.triangleSize(x));
      double bestdx = Double.POSITIVE_INFINITY;
      int bestix = -1;
      for(int y = 0; y < x; y++, p++) {
        final double v = scratch[p];
        if(v < bestd[y]) {
          bestd[y] = v;
          besti[y] = x;
        }
        if(v < bestdx) {
          bestdx = v;
          bestix = y;
        }
      }
      bestd[x] = bestdx;
      besti[x] = bestix;
    }
  }

  /**
   * Perform the next merge step.
   *
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
    for(int cx = 0; cx < size; cx++) {
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
    assert (x >= 0 && y >= 0);
    assert (y < x); // We could swap otherwise, but this shouldn't arise.
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
    assert (y < x);
    // Since y < x, prefer keeping y, dropping x.
    builder.add(ix, linkage.restore(mindist, getDistanceFunction().isSquared()), iy);
    // Update cluster size for y:
    final int sizex = builder.getSize(ix), sizey = builder.getSize(iy);
    builder.setSize(iy, sizex + sizey);

    // Deactivate x in cache:
    besti[x] = -1;

    // Note: this changes iy.
    updateMatrix(size, mat.matrix, iy, bestd, besti, builder, mindist, x, y, sizex, sizey);
    if(besti[y] == x) {
      findBest(size, mat.matrix, bestd, besti, y);
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
   * @param sizex Old size of first cluster
   * @param sizey Old size of second cluster
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
      updateCache(size, scratch, bestd, besti, x, y, j, d);
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
      updateCache(size, scratch, bestd, besti, x, y, j, d);
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
      updateCache(size, scratch, bestd, besti, x, y, j, d);
    }
  }

  /**
   * Update the cache.
   *
   * @param size Working set size
   * @param scratch Scratch matrix
   * @param bestd Best distance
   * @param besti Best index
   * @param x First cluster
   * @param y Second cluster, {@code y < x}
   * @param j Updated value d(y, j)
   * @param d New distance
   */
  private void updateCache(int size, double[] scratch, double[] bestd, int[] besti, int x, int y, int j, double d) {
    // New best
    if(d <= bestd[j]) {
      bestd[j] = d;
      besti[j] = y;
      return;
    }
    // Needs slow update.
    if(besti[j] == x || besti[j] == y) {
      findBest(size, scratch, bestd, besti, j);
    }
  }

  protected void findBest(int size, double[] scratch, double[] bestd, int[] besti, int j) {
    final int jbase = MatrixParadigm.triangleSize(j);
    // The distance has increased, we may no longer be the best merge.
    double bestdj = Double.POSITIVE_INFINITY;
    int bestij = -1;
    for(int i = 0, o = jbase; i < j; i++, o++) {
      if(besti[i] < 0) {
        continue;
      }
      final double dist = scratch[o];
      if(dist <= bestdj) {
        bestdj = dist;
        bestij = i;
      }
    }
    for(int i = j + 1, o = jbase + j + j; i < size; o += i, i++) {
      // assert(o == MatrixParadigm.triangleSize(i) + j);
      if(besti[i] < 0) {
        continue;
      }
      final double dist = scratch[o];
      if(dist <= bestdj) {
        bestdj = dist;
        bestij = i;
      }
    }
    bestd[j] = bestdj;
    besti[j] = bestij;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    // The input relation must match our distance function:
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
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
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Current linkage in use.
     */
    protected Linkage linkage;

    @Override
    protected void makeOptions(Parameterization config) {
      // We don't call super, because we want a different default distance.
      ObjectParameter<DistanceFunction<O>> distanceFunctionP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, DistanceFunction.class, SquaredEuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }

      ObjectParameter<Linkage> linkageP = new ObjectParameter<>(AGNES.Parameterizer.LINKAGE_ID, Linkage.class);
      linkageP.setDefaultValue(WardLinkage.class);
      if(config.grab(linkageP)) {
        linkage = linkageP.instantiateClass(config);
      }
    }

    @Override
    protected AnderbergHierarchicalClustering<O> makeInstance() {
      return new AnderbergHierarchicalClustering<>(distanceFunction, linkage);
    }
  }
}
