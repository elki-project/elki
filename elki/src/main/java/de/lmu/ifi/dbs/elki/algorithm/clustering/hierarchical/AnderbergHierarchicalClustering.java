package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * This is a modification of the classic AGNES algorithm for hierarchical
 * clustering using a nearest-neighbor heuristic for acceleration.
 *
 * Instead of scanning the matrix (with cost O(n^2)) to find the minimum, the
 * nearest neighbor of each object is remembered. On the downside, we need to
 * check these values at every merge, and it may now cost O(n^2) to perform a
 * merge, so there is no worst-case advantage to this approach. The average case
 * however improves from O(n^3) to O(n^2), which yields a considerable
 * improvement in running time.
 *
 * This optimization is attributed to M. R. Anderberg.
 *
 * Reference:
 * <p>
 * M. R. Anderberg<br />
 * Hierarchical Clustering Methods<br />
 * Cluster Analysis for Applications<br />
 * ISBN: 0120576503
 * </p>
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @apiviz.composedOf LinkageMethod
 * @apiviz.composedOf PointerHierarchyRepresentationBuilder
 *
 * @param <O> Object type
 */
@Reference(authors = "M. R. Anderberg", //
title = "Hierarchical Clustering Methods", //
booktitle = "Cluster Analysis for Applications")
public class AnderbergHierarchicalClustering<O> extends AbstractDistanceBasedAlgorithm<O, PointerHierarchyRepresentationResult>implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(AnderbergHierarchicalClustering.class);

  /**
   * Current linkage method in use.
   */
  LinkageMethod linkage = WardLinkageMethod.STATIC;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to use
   * @param linkage Linkage method
   */
  public AnderbergHierarchicalClustering(DistanceFunction<? super O> distanceFunction, LinkageMethod linkage) {
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
    DistanceQuery<O> dq = db.getDistanceQuery(relation, getDistanceFunction());
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    final int size = ids.size();

    if(size > 0x10000) {
      throw new AbortException("This implementation does not scale to data sets larger than " + //
      0x10000 // = 65535
      + " instances (~16 GB RAM), at which point the Java maximum array size is reached.");
    }
    if(SingleLinkageMethod.class.isInstance(linkage)) {
      LOG.verbose("Notice: SLINK is a much faster algorithm for single-linkage clustering!");
    }

    // Compute the initial (lower triangular) distance matrix.
    double[] scratch = new double[AGNES.triangleSize(size)];
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();
    // Position counter - must agree with computeOffset!
    final boolean square = WardLinkageMethod.class.isInstance(linkage) && !(SquaredEuclideanDistanceFunction.class.isInstance(dq.getDistanceFunction()));
    AGNES.initializeDistanceMatrix(scratch, dq, ix, iy, square);

    // Arrays used for caching:
    double[] bestd = new double[size];
    int[] besti = new int[size];
    initializeNNCache(scratch, bestd, besti);

    // Initialize space for result:
    PointerHierarchyRepresentationBuilder builder = new PointerHierarchyRepresentationBuilder(ids);

    // Repeat until everything merged into 1 cluster
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", size - 1, LOG) : null;
    int wsize = size;
    for(int i = 1; i < size; i++) {
      int x = findMerge(wsize, scratch, ix, iy, bestd, besti, builder);
      if(x == wsize - 1) {
        --wsize;
        for(ix.seek(wsize - 1); builder.isLinked(ix); ix.retract()) {
          --wsize;
        }
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    return builder.complete();
  }

  /**
   * Initialize the NN cache.
   *
   * @param scratch Scatch space
   * @param bestd Best distance
   * @param besti Best index
   */
  private static void initializeNNCache(double[] scratch, double[] bestd, int[] besti) {
    final int size = bestd.length;
    Arrays.fill(bestd, Double.POSITIVE_INFINITY);
    Arrays.fill(besti, -1);
    for(int x = 0, p = 0; x < size; x++) {
      assert(p == AGNES.triangleSize(x));
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
   * @param scratch Scratch space.
   * @param ix First iterator
   * @param iy Second iterator
   * @param bestd Best distance
   * @param besti Index of best distance
   * @param builder Hierarchy builder
   * @return x, for shrinking the working set.
   */
  protected int findMerge(int size, double[] scratch, DBIDArrayIter ix, DBIDArrayIter iy, double[] bestd, int[] besti, PointerHierarchyRepresentationBuilder builder) {
    double mindist = Double.POSITIVE_INFINITY;
    int x = -1, y = -1;
    // Find minimum:
    for(int cx = 0; cx < size; cx++) {
      // Skip if object has already joined a cluster:
      if(besti[cx] < 0) {
        continue;
      }
      if(bestd[cx] < mindist) {
        mindist = bestd[cx];
        x = cx;
        y = besti[cx];
      }
    }
    assert(x >= 0 && y >= 0);
    merge(size, scratch, ix, iy, bestd, besti, builder, mindist, x < y ? y : x, x < y ? x : y);
    return x;
  }

  /**
   * Execute the cluster merge.
   *
   * @param size Data set size
   * @param scratch Scratch space.
   * @param ix First iterator
   * @param iy Second iterator
   * @param bestd Best distance
   * @param besti Index of best distance
   * @param builder Hierarchy builder
   * @param mindist Distance that was used for merging
   * @param x First matrix position
   * @param y Second matrix position
   */
  protected void merge(int size, double[] scratch, DBIDArrayIter ix, DBIDArrayIter iy, double[] bestd, int[] besti, PointerHierarchyRepresentationBuilder builder, double mindist, int x, int y) {
    // Avoid allocating memory, by reusing existing iterators:
    ix.seek(x);
    iy.seek(y);
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Merging: " + DBIDUtil.toString(ix) + " -> " + DBIDUtil.toString(iy) + " " + mindist);
    }
    // Perform merge in data structure: x -> y
    assert(y < x);
    // Since y < x, prefer keeping y, dropping x.
    builder.add(ix, mindist, iy);
    // Update cluster size for y:
    final int sizex = builder.getSize(ix), sizey = builder.getSize(iy);
    builder.setSize(iy, sizex + sizey);

    // Deactivate x in cache:
    besti[x] = -1;

    // Note: this changes iy.
    updateMatrix(size, scratch, iy, bestd, besti, builder, mindist, x, y, sizex, sizey);
    if(besti[y] == x) {
      findBest(size, scratch, bestd, besti, y);
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
    final int xbase = AGNES.triangleSize(x), ybase = AGNES.triangleSize(y);

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
    int jbase = AGNES.triangleSize(j);
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
      final double d = scratch[jbase + y] = linkage.combine(sizex, scratch[jbase + x], sizey, scratch[jbase + y], sizej, mindist);
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
    // Needs slow upate.
    if(besti[j] == x || besti[j] == y) {
      findBest(size, scratch, bestd, besti, j);
    }
  }

  protected void findBest(int size, double[] scratch, double[] bestd, int[] besti, int j) {
    final int jbase = AGNES.triangleSize(j);
    // The distance has increased, we may no longer be the best merge.
    double bestdj = Double.POSITIVE_INFINITY;
    int bestij = -1;
    for(int i = 0, o = jbase; i < j; i++, o++) {
      if(besti[i] < 0) {
        continue;
      }
      if(scratch[o] < bestdj) {
        bestdj = scratch[o];
        bestij = i;
      }
    }
    for(int i = j + 1, o = jbase + j + j; i < size; o += i, i++) {
      // assert(o == AGNES.triangleSize(i) + j);
      if(besti[i] < 0) {
        continue;
      }
      if(scratch[o] < bestdj) {
        bestdj = scratch[o];
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
   * @apiviz.exclude
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Current linkage in use.
     */
    protected LinkageMethod linkage;

    @Override
    protected void makeOptions(Parameterization config) {
      // We don't call super, because we want a different default distance.
      ObjectParameter<DistanceFunction<O>> distanceFunctionP = makeParameterDistanceFunction(SquaredEuclideanDistanceFunction.class, DistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }

      ObjectParameter<LinkageMethod> linkageP = new ObjectParameter<>(AGNES.Parameterizer.LINKAGE_ID, LinkageMethod.class);
      linkageP.setDefaultValue(WardLinkageMethod.class);
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
