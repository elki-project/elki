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

import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayMIter;
import elki.database.ids.DBIDUtil;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Reference;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * MiniMax hierarchical clustering using the NNchain algorithm.
 * <p>
 * Reference:
 * <p>
 * F. Murtagh<br>
 * A survey of recent advances in hierarchical clustering algorithms<br>
 * The Computer Journal 26(4)
 * <p>
 * D. Müllner<br>
 * Modern hierarchical, agglomerative clustering algorithms<br>
 * arXiv preprint arXiv:1109.2378
 *
 * @author Julian Erhard
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - PointerPrototypeHierarchyResult
 *
 * @param <O> Object type
 */
@Reference(authors = "F. Murtagh", //
    title = "A survey of recent advances in hierarchical clustering algorithms", //
    booktitle = "The Computer Journal 26(4)", //
    url = "https://doi.org/10.1093/comjnl/26.4.354", //
    bibkey = "DBLP:journals/cj/Murtagh83")
@Reference(authors = "D. Müllner", //
    title = "Modern hierarchical, agglomerative clustering algorithms", //
    booktitle = "arXiv preprint arXiv:1109.2378", //
    url = "https://arxiv.org/abs/1109.2378", //
    bibkey = "DBLP:journals/corr/abs-1109-2378")
public class MiniMaxNNChain<O> extends MiniMax<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(MiniMaxNNChain.class);

  /**
   * Constructor.
   *
   * @param distance Distance function
   */
  public MiniMaxNNChain(Distance<? super O> distance) {
    super(distance);
  }

  /**
   * Run the algorithm
   *
   * @param relation Data relation
   * @return Clustering result
   */
  @Override
  public ClusterPrototypeMergeHistory run(Relation<O> relation) {
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).precomputed().distanceQuery();
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    ArrayModifiableDBIDs prots = DBIDUtil.newArray(ClusterDistanceMatrix.triangleSize(ids.size()));
    ClusterDistanceMatrix mat = MiniMax.initializeMatrices(ids, prots, dq);
    ClusterMergeHistoryBuilder builder = new ClusterMergeHistoryBuilder(ids, distance.isSquared());
    return new Instance().run(ids, mat, builder, dq, prots.iter());
  }

  /**
   * Main worker instance of MiniMaxNNChain.
   * 
   * @author Erich Schubert
   */
  public static class Instance extends MiniMax.Instance {
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
      nnChainCore();
      builder.optimizeOrder();
      return (ClusterPrototypeMergeHistory) builder.complete();
    }

    /**
     * Uses NNChain as in "Modern hierarchical, agglomerative clustering
     * algorithms" by Daniel Müllner.
     */
    private void nnChainCore() {
      final int size = mat.size;
      final double[] distances = mat.matrix;
      final int[] clustermap = mat.clustermap;
      // The maximum chain size = number of ids + 1, but usually much less
      IntegerArray chain = new IntegerArray(size << 1);

      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Running MiniMax-NNChain", size - 1, LOG) : null;
      for(int k = 1; k < size; k++) {
        int a = -1, b = -1;
        if(chain.size() < 2) {
          // Accessing two arbitrary not yet merged elements could be optimized
          // to work in O(1) like in Müllner; however this usually does not have
          // a huge impact (empirically just about 1/5000 of total performance)
          a = NNChain.Instance.findUnlinked(0, end, clustermap);
          b = NNChain.Instance.findUnlinked(a + 1, end, clustermap);
          chain.clear();
          chain.add(a);
        }
        else {
          a = chain.get(chain.size - 2);
          b = chain.get(chain.size - 1);
          assert clustermap[a] >= 0 && clustermap[b] >= 0;
          chain.size--; // Remove b
        }
        // For ties, always prefer the second-last element b:
        double minDist = mat.get(a, b);
        do {
          int c = b;
          final int ta = ClusterDistanceMatrix.triangleSize(a);
          for(int i = 0; i < a; i++) {
            if(i != b && clustermap[i] >= 0) {
              double dist = distances[ta + i];
              if(dist < minDist) {
                minDist = dist;
                c = i;
              }
            }
          }
          for(int i = a + 1; i < end; i++) {
            if(i != b && clustermap[i] >= 0) {
              double dist = distances[ClusterDistanceMatrix.triangleSize(i) + a];
              if(dist < minDist) {
                minDist = dist;
                c = i;
              }
            }
          }
          b = a;
          a = c;
          chain.add(a);
        }
        while(chain.size() < 3 || a != chain.get(chain.size - 1 - 2));

        // We always merge the larger into the smaller index:
        if(a < b) {
          int tmp = a;
          a = b;
          b = tmp;
        }
        merge(a, b);
        end = shrinkActiveSet(clustermap, end, a); // shrink working set
        chain.size -= 3;
        chain.add(b);
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Parameterization class.
   *
   * @author Julian Erhard
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Par<O> extends MiniMax.Par<O> {
    @Override
    public MiniMaxNNChain<O> make() {
      return new MiniMaxNNChain<>(distance);
    }
  }
}
