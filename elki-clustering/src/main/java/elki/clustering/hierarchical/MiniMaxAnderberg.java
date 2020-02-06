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

import elki.Algorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * This is a modification of the classic MiniMax algorithm for hierarchical
 * clustering using a nearest-neighbor heuristic for acceleration.
 * <p>
 * This optimization is attributed to M. R. Anderberg.
 * <p>
 * This particular implementation is based on Anderberg
 * <p>
 * Reference:
 * <p>
 * M. R. Anderberg<br>
 * Hierarchical Clustering Methods<br>
 * Cluster Analysis for Applications<br>
 * ISBN: 0120576503
 *
 * @author Julian Erhard
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - PointerPrototypeHierarchyRepresentationResult
 */
@Reference(authors = "M. R. Anderberg", //
    title = "Hierarchical Clustering Methods", //
    booktitle = "Cluster Analysis for Applications", //
    bibkey = "books/academic/Anderberg73/Ch6")
@Priority(Priority.RECOMMENDED - 5)
public class MiniMaxAnderberg<O> implements HierarchicalClusteringAlgorithm {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(MiniMaxAnderberg.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Constructor.
   *
   * @param distance Distance function to use
   */
  public MiniMaxAnderberg(Distance<? super O> distance) {
    super();
    this.distance = distance;
  }

  /**
   * Run the algorithm
   *
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public PointerHierarchyRepresentationResult run(Relation<O> relation) {
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).precomputed().distanceQuery();
    final DBIDs ids = relation.getDBIDs();
    final int size = ids.size();

    // Initialize space for result:
    PointerHierarchyRepresentationBuilder builder = new PointerHierarchyRepresentationBuilder(ids, dq.getDistance().isSquared());
    Int2ObjectOpenHashMap<ModifiableDBIDs> clusters = new Int2ObjectOpenHashMap<>();

    // Compute the initial (lower triangular) distance matrix.
    MatrixParadigm mat = new MatrixParadigm(ids);
    ArrayModifiableDBIDs prots = DBIDUtil.newArray(MatrixParadigm.triangleSize(size));
    DBIDArrayMIter protiter = prots.iter();

    MiniMax.initializeMatrices(mat, prots, dq);

    // Arrays used for caching:
    double[] bestd = new double[size];
    int[] besti = new int[size];
    Anderberg.initializeNNCache(mat.matrix, bestd, besti);

    // Repeat until everything merged into 1 cluster
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", size - 1, LOG) : null;
    DBIDArrayIter ix = mat.ix;
    for(int i = 1, end = size; i < size; i++) {
      end = AGNES.shrinkActiveSet(ix, builder, end, //
          findMerge(end, mat, protiter, builder, clusters, bestd, besti, dq));
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    return builder.complete();
  }

  /**
   * Perform the next merge step.
   * 
   * @param size size of the data set
   * @param mat matrix view
   * @param prots the prototypes of merges between clusters
   * @param builder Result builder
   * @param clusters the current clustering
   * @param bestd the distances to the nearest neighboring cluster
   * @param besti the nearest neighboring cluster
   * @param dq the range query
   * @return x, for shrinking the active set.
   */
  protected int findMerge(int size, MatrixParadigm mat, DBIDArrayMIter prots, PointerHierarchyRepresentationBuilder builder, Int2ObjectOpenHashMap<ModifiableDBIDs> clusters, double[] bestd, int[] besti, DistanceQuery<O> dq) {
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
    merge(size, mat, prots, builder, clusters, dq, bestd, besti, x, y);
    return x;
  }

  /**
   * Execute the cluster merge
   *
   * @param size size of data set
   * @param mat Matrix paradigm
   * @param prots the prototypes of merges between clusters
   * @param builder Result builder
   * @param clusters the current clustering
   * @param dq the range query
   * @param bestd the distances to the nearest neighboring cluster
   * @param besti the nearest neighboring cluster
   * @param x first cluster to merge, with {@code x > y}
   * @param y second cluster to merge, with {@code y < x}
   */
  protected void merge(int size, MatrixParadigm mat, DBIDArrayMIter prots, PointerHierarchyRepresentationBuilder builder, Int2ObjectOpenHashMap<ModifiableDBIDs> clusters, DistanceQuery<O> dq, double[] bestd, int[] besti, int x, int y) {
    // Avoid allocating memory, by reusing existing iterators:
    final DBIDArrayIter ix = mat.ix.seek(x), iy = mat.iy.seek(y);
    final double[] distances = mat.matrix;
    int offset = MatrixParadigm.triangleSize(x) + y;
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Merging: " + DBIDUtil.toString(ix) + " -> " + DBIDUtil.toString(iy) + " " + distances[offset]);
    }
    // Perform merge in data structure: x -> y
    assert y < x;
    ModifiableDBIDs cx = clusters.get(x), cy = clusters.get(y);
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
    builder.add(ix, distances[offset], iy, prots.seek(offset));
    besti[x] = -1; // Deactivate x in cache:
    updateMatrices(size, mat, prots, builder, clusters, dq, bestd, besti, x, y);
    if(y > 0) {
      Anderberg.findBest(distances, bestd, besti, y);
    }
  }

  /**
   * Update the entries of the matrices that contain a distance to y, the newly
   * merged cluster.
   *
   * @param size size of data set
   * @param mat matrix view
   * @param prots the prototypes of merges between clusters
   * @param builder Result builder
   * @param clusters the current clustering
   * @param dq the range query
   * @param bestd the distances to the nearest neighboring cluster
   * @param besti the nearest neighboring cluster
   * @param x first cluster to merge, with {@code x > y}
   * @param y second cluster to merge, with {@code y < x}
   */
  private void updateMatrices(int size, MatrixParadigm mat, DBIDArrayMIter prots, PointerHierarchyRepresentationBuilder builder, Int2ObjectOpenHashMap<ModifiableDBIDs> clusters, DistanceQuery<O> dq, double[] bestd, int[] besti, int x, int y) {
    final DBIDArrayIter ix = mat.ix, iy = mat.iy;
    final double[] distances = mat.matrix;
    // c is the new cluster.
    // Update entries (at (a,b) with a > b) in the matrix where a = y or b = y

    // Update entries at (y,b) with b < y
    int a = y, b = 0;
    ix.seek(a);
    final int yoffset = MatrixParadigm.triangleSize(y);
    for(; b < a; b++) {
      // Skip entry if already merged
      if(builder.isLinked(iy.seek(b))) {
        continue;
      }
      MiniMax.updateEntry(mat, prots, clusters, dq, a, b);
      Anderberg.updateCache(distances, bestd, besti, x, y, b, distances[yoffset + b]);
    }

    // Update entries at (a,y) with a > y
    a = y + 1;
    b = y;
    iy.seek(b);
    for(; a < size; a++) {
      // Skip entry if already merged
      if(builder.isLinked(ix.seek(a))) {
        continue;
      }
      MiniMax.updateEntry(mat, prots, clusters, dq, a, b);
      Anderberg.updateCache(distances, bestd, besti, x, y, a, distances[MatrixParadigm.triangleSize(a) + y]);
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
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
    }

    @Override
    public MiniMaxAnderberg<O> make() {
      return new MiniMaxAnderberg<>(distance);
    }
  }
}
