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

import elki.AbstractDistanceBasedAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.*;
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
 * @has - - - PointerPrototypeHierarchyRepresentationResult
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
public class MiniMaxNNChain<O> extends AbstractDistanceBasedAlgorithm<Distance<? super O>, PointerPrototypeHierarchyRepresentationResult> implements HierarchicalClusteringAlgorithm {
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
  public PointerPrototypeHierarchyRepresentationResult run(Relation<O> relation) {
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).precomputed().distanceQuery();
    final DBIDs ids = relation.getDBIDs();

    // Initialize space for result:
    PointerHierarchyRepresentationBuilder builder = new PointerHierarchyRepresentationBuilder(ids, dq.getDistance().isSquared());
    Int2ObjectOpenHashMap<ModifiableDBIDs> clusters = new Int2ObjectOpenHashMap<>(ids.size());

    MatrixParadigm mat = new MatrixParadigm(ids);
    ArrayModifiableDBIDs prots = DBIDUtil.newArray(MatrixParadigm.triangleSize(ids.size()));

    MiniMax.initializeMatrices(mat, prots, dq);

    nnChainCore(mat, prots.iter(), dq, builder, clusters);

    return (PointerPrototypeHierarchyRepresentationResult) builder.complete();
  }

  /**
   * Uses NNChain as in "Modern hierarchical, agglomerative clustering
   * algorithms" by Daniel Müllner
   * 
   * @param mat distance matrix
   * @param prots computed prototypes
   * @param dq distance query of the data set
   * @param builder Result builder
   * @param clusters current clusters
   */
  private void nnChainCore(MatrixParadigm mat, DBIDArrayMIter prots, DistanceQuery<O> dq, PointerHierarchyRepresentationBuilder builder, Int2ObjectOpenHashMap<ModifiableDBIDs> clusters) {
    final DBIDArrayIter ix = mat.ix;
    final double[] distances = mat.matrix;
    final int size = mat.size;
    // The maximum chain size = number of ids + 1
    IntegerArray chain = new IntegerArray(size + 1);

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Running MiniMax-NNChain", size - 1, LOG) : null;
    for(int k = 1, end = size; k < size; k++) {
      int a = -1, b = -1;
      if(chain.size() <= 3) {
        // Accessing two arbitrary not yet merged elements could be optimized to
        // work in O(1) like in Müllner;
        // however this usually does not have a huge impact (empirically just
        // about 1/5000 of total performance)
        a = NNChain.findUnlinked(0, end, ix, builder);
        b = NNChain.findUnlinked(a + 1, end, ix, builder);
        chain.clear();
        chain.add(a);
      }
      else {
        // Chain is expected to look like (.... a, b, c, b) with b and c merged.
        int lastIndex = chain.size;
        int c = chain.get(lastIndex - 2);
        b = chain.get(lastIndex - 3);
        a = chain.get(lastIndex - 4);
        // Ensure we had a loop at the end:
        assert (chain.get(lastIndex - 1) == c || chain.get(lastIndex - 1) == b);
        // if c < b, then we merged b -> c, otherwise c -> b
        b = c < b ? c : b;
        // Cut the tail:
        chain.size -= 3;
      }
      // For ties, always prefer the second-last element b:
      double minDist = mat.get(a, b);
      do {
        int c = b;
        final int ta = MatrixParadigm.triangleSize(a);
        for(int i = 0; i < a; i++) {
          if(i != b && !builder.isLinked(ix.seek(i))) {
            double dist = distances[ta + i];
            if(dist < minDist) {
              minDist = dist;
              c = i;
            }
          }
        }
        for(int i = a + 1; i < size; i++) {
          if(i != b && !builder.isLinked(ix.seek(i))) {
            double dist = distances[MatrixParadigm.triangleSize(i) + a];
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
      assert (minDist == mat.get(a, b));
      assert (b < a);
      MiniMax.merge(size, mat, prots, builder, clusters, dq, a, b);
      end = AGNES.shrinkActiveSet(ix, builder, end, a); // Shrink working set
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
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
  public static class Par<O> extends AbstractDistanceBasedAlgorithm.Par<Distance<? super O>> {
    @Override
    public MiniMaxNNChain<O> make() {
      return new MiniMaxNNChain<>(distance);
    }
  }
}
