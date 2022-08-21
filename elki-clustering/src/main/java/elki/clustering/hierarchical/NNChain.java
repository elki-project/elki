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

import elki.clustering.hierarchical.linkage.Linkage;
import elki.clustering.hierarchical.linkage.SingleLinkage;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDUtil;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Reference;

/**
 * NNchain clustering algorithm.
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
 * @author Erich Schubert
 * @since 0.7.5
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
public class NNChain<O> extends AGNES<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(NNChain.class);

  /**
   * Constructor.
   *
   * @param distance Distance function
   */
  public NNChain(Distance<? super O> distance, Linkage linkage) {
    super(distance, linkage);
  }

  @Override
  public ClusterMergeHistory run(Relation<O> relation) {
    if(SingleLinkage.class.isInstance(linkage)) {
      LOG.verbose("Notice: SLINK is a much faster algorithm for single-linkage clustering!");
    }
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).distanceQuery();
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    ClusterDistanceMatrix mat = initializeDistanceMatrix(ids, dq, linkage);
    return new Instance(linkage).run(mat, new ClusterMergeHistoryBuilder(ids, distance.isSquared()));
  }

  /**
   * Main worker instance of NNChain.
   * 
   * @author Erich Schubert
   */
  public static class Instance extends AGNES.Instance {
    /**
     * Constructor.
     *
     * @param linkage Linkage
     */
    public Instance(Linkage linkage) {
      super(linkage);
    }

    @Override
    public ClusterMergeHistory run(ClusterDistanceMatrix mat, ClusterMergeHistoryBuilder builder) {
      this.mat = mat;
      this.builder = builder;
      this.end = mat.size;
      nnChainCore();
      builder.optimizeOrder();
      return builder.complete();
    }

    /**
     * Uses NNChain as in "Modern hierarchical, agglomerative clustering
     * algorithms" by Daniel Müllner.
     */
    private void nnChainCore() {
      final int size = mat.size;
      boolean warnedIrreducible = false;
      final double[] distances = mat.matrix;
      final int[] clustermap = mat.clustermap;
      // The maximum chain size = number of ids + 1, but usually much less
      IntegerArray chain = new IntegerArray(size >> 2);

      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Running NNChain", size - 1, LOG) : null;
      for(int k = 1; k < size; k++) {
        int a = -1, b = -1;
        if(chain.size() < 2) {
          // Accessing two arbitrary not yet merged elements could be optimized
          // to work in O(1) like in Müllner; however this usually does not have
          // a huge impact (empirically just about 1/5000 of total performance)
          a = findUnlinked(0, end, clustermap);
          b = findUnlinked(a + 1, end, clustermap);
          assert clustermap[a] >= 0 && clustermap[b] >= 0;
          chain.clear();
          chain.add(a);
        }
        else {
          a = chain.get(chain.size - 2);
          b = chain.get(chain.size - 1);
          assert clustermap[b] >= 0;
          if(clustermap[a] < 0) {
            if(!warnedIrreducible) {
              LOG.warning("Detected an inversion in the clustering. NNChain on irreducible linkages may yield different results.");
              warnedIrreducible = true;
            }
            chain.size -= 2; // cut the chain
            k--; // retry
            continue;
          }
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
        assert minDist == mat.get(a, b);
        merge(minDist, a, b);
        chain.size -= 3;
        chain.add(b);
        end = shrinkActiveSet(clustermap, end, a); // shrink working set
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);
    }

    /**
     * Find an unlinked object.
     *
     * @param pos Starting position
     * @param end End position
     * @param clustermap Map of indexes to current clusters
     * @return Position
     */
    public static int findUnlinked(int pos, int end, int[] clustermap) {
      while(pos < end) {
        if(clustermap[pos] >= 0) {
          return pos;
        }
        ++pos;
      }
      return -1;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Par<O> extends AGNES.Par<O> {
    @Override
    public NNChain<O> make() {
      return new NNChain<>(distance, linkage);
    }
  }
}
