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

import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * This is a modification of the classic MiniMax algorithm for hierarchical
 * clustering using a nearest-neighbor heuristic for acceleration.
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
 * @author Julian Erhard
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - PointerPrototypeHierarchyResult
 */
@Reference(authors = "M. R. Anderberg", //
    title = "Hierarchical Clustering Methods", //
    booktitle = "Cluster Analysis for Applications", //
    bibkey = "books/academic/Anderberg73/Ch6")
@Priority(Priority.RECOMMENDED - 5)
public class MiniMaxAnderberg<O> extends MiniMax<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(MiniMaxAnderberg.class);

  /**
   * Constructor.
   *
   * @param distance Distance function to use
   */
  public MiniMaxAnderberg(Distance<? super O> distance) {
    super(distance);
  }

  /**
   * Run the algorithm
   *
   * @param relation Relation
   * @return Clustering hierarchy
   */
  @Override
  public ClusterPrototypeMergeHistory run(Relation<O> relation) {
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).precomputed().distanceQuery();
    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    ArrayModifiableDBIDs prots = DBIDUtil.newArray(ClusterDistanceMatrix.triangleSize(ids.size()));
    ClusterDistanceMatrix mat = MiniMax.initializeMatrices(ids, prots, dq);
    return new Instance().run(ids, mat, new ClusterMergeHistoryBuilder(ids, dq.getDistance().isSquared()), dq, prots.iter());
  }

  /**
   * Main worker instance of MiniMax.
   * 
   * @author Erich Schubert
   */
  public static class Instance extends MiniMax.Instance {
    /**
     * Cache: best distance
     */
    protected double[] bestd;

    /**
     * Cache: index of best distance
     */
    protected int[] besti;

    @Override
    public ClusterPrototypeMergeHistory run(ArrayDBIDs ids, ClusterDistanceMatrix mat, ClusterMergeHistoryBuilder builder, DistanceQuery<?> dq, DBIDArrayMIter prots) {
      final int size = mat.size;
      this.mat = mat;
      this.builder = builder;
      this.end = size;
      this.clusters = new Int2ObjectOpenHashMap<>(size);
      this.protiter = prots;
      this.dq = dq;
      this.ix = ids.iter();
      this.iy = ids.iter();

      // Arrays used for caching:
      this.bestd = new double[size];
      this.besti = new int[size];
      Anderberg.Instance.initializeNNCache(mat.matrix, bestd, besti);

      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", size - 1, LOG) : null;
      for(int i = 1; i < size; i++) {
        end = shrinkActiveSet(mat.clustermap, end, findMerge());
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);
      return (ClusterPrototypeMergeHistory) builder.complete();
    }

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
      merge(x, y);
      return x;
    }

    @Override
    protected void merge(int x, int y) {
      assert x >= 0 && y >= 0;
      assert y < x;
      final double[] distances = mat.matrix;
      final int offset = ClusterDistanceMatrix.triangleSize(x) + y;
      ModifiableDBIDs cx = clusters.get(x), cy = clusters.get(y);
      // Keep y
      if(cy == null) {
        cy = DBIDUtil.newHashSet();
        cy.add(iy.seek(y));
      }
      if(cx == null) {
        cy.add(ix.seek(x));
      }
      else {
        cy.addDBIDs(cx);
        clusters.remove(x);
      }
      clusters.put(y, cy);

      // parent of x is set to y
      final int xx = mat.clustermap[x], yy = mat.clustermap[y];
      final int sizex = builder.getSize(xx), sizey = builder.getSize(yy);
      int zz = builder.strictAdd(xx, distances[offset], yy, protiter.seek(offset));
      assert builder.getSize(zz) == sizex + sizey;
      mat.clustermap[y] = zz;
      mat.clustermap[x] = besti[x] = -1; // Deactivate removed cluster.
      updateMatrices(x, y);
      if(y > 0) {
        Anderberg.Instance.findBest(distances, bestd, besti, y);
      }
    }

    /**
     * Update the entries of the matrices that contain a distance to y, the
     * newly merged cluster.
     *
     * @param x first cluster to merge, with {@code x > y}
     * @param y second cluster to merge, with {@code y < x}
     */
    private void updateMatrices(int x, int y) {
      final double[] distances = mat.matrix;
      // c is the new cluster.
      // Update entries (at (a,b) with a > b) in the matrix where a = y or b = y
      // Update entries at (y,b) with b < y
      int a = y, b = 0;
      final int yoffset = ClusterDistanceMatrix.triangleSize(y);
      for(; b < a; b++) {
        // Skip entry if already merged
        if(mat.clustermap[b] < 0) {
          continue;
        }
        updateEntry(a, b);
        Anderberg.Instance.updateCache(distances, bestd, besti, x, y, b, distances[yoffset + b]);
      }

      // Update entries at (a,y) with a > y
      a = y + 1;
      b = y;
      for(; a < end; a++) {
        // Skip entry if already merged
        if(mat.clustermap[a] < 0) {
          continue;
        }
        updateEntry(a, b);
        Anderberg.Instance.updateCache(distances, bestd, besti, x, y, a, distances[ClusterDistanceMatrix.triangleSize(a) + y]);
      }
    }
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
  public static class Par<O> extends MiniMax.Par<O> {
    @Override
    public MiniMaxAnderberg<O> make() {
      return new MiniMaxAnderberg<>(distance);
    }
  }
}
