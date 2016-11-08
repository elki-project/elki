package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * This is a modification of the classic MiniMax algorithm for hierarchical
 * clustering using a nearest-neighbor heuristic for acceleration.
 *
 * This optimization is attributed to M. R. Anderberg.
 * 
 * This particular implementation is based on AnderbergHierarchicalClustering
 *
 * Reference:
 * <p>
 * M. R. Anderberg<br />
 * Hierarchical Clustering Methods<br />
 * Cluster Analysis for Applications<br />
 * ISBN: 0120576503
 * </p>
 * 
 * @author Julian Erhard
 * @author Erich Schubert
 */
@Reference(authors = "M. R. Anderberg", //
    title = "Hierarchical Clustering Methods", //
    booktitle = "Cluster Analysis for Applications")
public class MiniMaxAnderberg<O> extends AbstractDistanceBasedAlgorithm<O, PointerHierarchyRepresentationResult> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(MiniMaxAnderberg.class);

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to use
   * @param linkage Linkage method
   */
  public MiniMaxAnderberg(DistanceFunction<? super O> distanceFunction) {
    super(distanceFunction);
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

    // Initialize space for result:
    PointerHierarchyRepresentationBuilder builder = new PointerHierarchyRepresentationBuilder(ids);
    TIntObjectHashMap<ModifiableDBIDs> clusters = new TIntObjectHashMap<>();

    // Compute the initial (lower triangular) distance matrix.
    double[] distances = new double[AGNES.triangleSize(size)];
    ArrayModifiableDBIDs prots = DBIDUtil.newArray(AGNES.triangleSize(size));
    DBIDArrayMIter protiter = prots.iter();

    DBIDArrayIter ix = ids.iter(), iy = ids.iter();
    MiniMax.initializeMatrices(distances, prots, dq, ix, iy);

    // Arrays used for caching:
    double[] bestd = new double[size];
    int[] besti = new int[size];
    initializeNNCache(distances, bestd, besti);

    // Repeat until everything merged into 1 cluster
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", size - 1, LOG) : null;
    int wsize = size;
    for(int i = 1; i < size; i++) {
      findMerge(wsize, distances, protiter, ix, iy, builder, clusters, bestd, besti, dq);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    return (PointerPrototypeHierarchyRepresentationResult) builder.complete();
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
      assert (p == AGNES.triangleSize(x));
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
   * @param size size of the data set
   * @param distances the inter-cluster distance matrix
   * @param prots the prototypes of merges between clusters
   * @param ix an iterator over the data set
   * @param iy another iterator over the data set
   * @param builder Result builder
   * @param clusters the current clustering
   * @param bestd the distances to the nearest neighboring cluster
   * @param besti the nearest neighboring cluster
   * @param dq the range query
   */
  protected void findMerge(int size, double[] distances, DBIDArrayMIter prots, DBIDArrayIter ix, DBIDArrayIter iy, PointerHierarchyRepresentationBuilder builder, TIntObjectHashMap<ModifiableDBIDs> clusters, double[] bestd, int[] besti, DistanceQuery<O> dq) {
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
    // assert(lambda.doubleValue(ix.seek(x < y ? y :
    // x))==Double.POSITIVE_INFINITY);
    assert (x >= 0 && y >= 0);
    assert (x != y);
    merge(size, distances, prots, ix, iy, builder, clusters, dq, bestd, besti, x < y ? y : x, x < y ? x : y);
  }

  /**
   * Execute the cluster merge
   * 
   * @param size size of data set
   * @param distances the inter-cluster distance matrix
   * @param prots the prototypes of merges between clusters
   * @param ix an iterator over the data set
   * @param iy another iterator over the data set
   * @param builder Result builder
   * @param clusters the current clustering
   * @param dq the range query
   * @param bestd the distances to the nearest neighboring cluster
   * @param besti the nearest neighboring cluster
   * @param x first cluster to merge, with x > y
   * @param y second cluster to merge, with y < x
   */
  protected void merge(int size, double[] distances, DBIDArrayMIter prots, DBIDArrayIter ix, DBIDArrayIter iy, PointerHierarchyRepresentationBuilder builder, TIntObjectHashMap<ModifiableDBIDs> clusters, DistanceQuery<O> dq, double[] bestd, int[] besti, int x, int y) {
    int offset = AGNES.triangleSize(x) + y;

    assert (y < x);

    ix.seek(x);
    iy.seek(y);
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Merging: " + DBIDUtil.toString(ix) + " -> " + DBIDUtil.toString(iy) + " " + distances[offset]);
    }

    ModifiableDBIDs cx = clusters.get(x);
    ModifiableDBIDs cy = clusters.get(y);

    // Keep y
    if(cy == null) {
      cy = DBIDUtil.newHashSet();
      cy.add(iy);
    }
    if(cx == null) {
      cy.add(ix);
    }
    else {
      cy.addDBIDs(cx);
      clusters.remove(x);
    }
    clusters.put(y, cy);

    // parent of x is set to y
    builder.add(ix, distances[offset], iy, prots.seek(y));

    // Deactivate x in cache:
    besti[x] = -1;
    updateMatrices(size, distances, prots, ix, iy, builder, clusters, dq, bestd, besti, x, y);
    if(besti[y] == x) {
      findBest(size, distances, bestd, besti, y);
    }
  }

  /**
   * Update the entries of the matrices that contain a distance to y, the newly
   * merged cluster.
   * 
   * @param size size of data set
   * @param distances the inter-cluster distance matrix
   * @param prots the prototypes of merges between clusters
   * @param ix an iterator over the data set
   * @param iy another iterator over the data set
   * @param builder Result builder
   * @param clusters the current clustering
   * @param dq the range query
   * @param bestd the distances to the nearest neighboring cluster
   * @param besti the nearest neighboring cluster
   * @param x first cluster to merge, with x > y
   * @param y second cluster to merge, with y < x
   */
  private void updateMatrices(int size, double[] distances, DBIDArrayMIter prots, DBIDArrayIter ix, DBIDArrayIter iy, PointerHierarchyRepresentationBuilder builder, TIntObjectHashMap<ModifiableDBIDs> clusters, DistanceQuery<O> dq, double[] bestd, int[] besti, int x, int y) {
    // c is the new cluster.
    // Update entries (at (a,b) with a > b) in the matrix where a = y or b = y

    // Update entries at (y,b) with b < y
    int a = y;
    int b = 0;
    ix.seek(a);
    for(; b < a; b++) {
      iy.seek(b);
      // Skip entry if already merged
      if(builder.isLinked(iy)) {
        continue;
      }
      MiniMax.updateEntry(distances, prots, ix, iy, clusters, dq, a, b);
      updateCache(size, distances, bestd, besti, x, y, b, distances[AGNES.triangleSize(y) + b]);
    }

    // Update entries at (a,y) with a > y
    a = y + 1;
    b = y;
    iy.seek(b);
    for(; a < size; a++) {
      ix.seek(a);
      // Skip entry if already merged
      if(builder.isLinked(ix)) {
        continue;
      }
      MiniMax.updateEntry(distances, prots, ix, iy, clusters, dq, a, b);
      updateCache(size, distances, bestd, besti, x, y, a, distances[AGNES.triangleSize(a) + y]);
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
    /*
     * if(d <= bestd[j]) { //We do not need this here, as d(j, x u y) >= min { d
     * (j, x), d(j, y) } bestd[j] = d; besti[j] = y; return; }
     */
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

    @Override
    protected MiniMaxAnderberg<O> makeInstance() {
      return new MiniMaxAnderberg<>(distanceFunction);
    }
  }
}
