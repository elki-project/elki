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
package elki.index.tree.spatial.rstarvariants.strategies.overflow;

import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.index.tree.IndexTreePath;
import elki.index.tree.LeafEntry;
import elki.index.tree.spatial.SpatialEntry;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import elki.index.tree.spatial.rstarvariants.strategies.reinsert.CloseReinsert;
import elki.index.tree.spatial.rstarvariants.strategies.reinsert.ReinsertStrategy;
import elki.index.tree.spatial.rstarvariants.util.NodeArrayAdapter;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Limited reinsertions, as proposed by the R*-Tree: For each real insert, allow
 * reinsertions to happen only once per level.
 * <p>
 * Reference:
 * <p>
 * Norbert Beckmann, Hans-Peter Kriegel, Ralf Schneider, Bernhard Seeger<br>
 * The R*-tree: an efficient and robust access method for points and
 * rectangles<br>
 * Proc. 1990 ACM SIGMOD Int. Conf. Management of Data
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "Norbert Beckmann, Hans-Peter Kriegel, Ralf Schneider, Bernhard Seeger", //
    title = "The R*-tree: an efficient and robust access method for points and rectangles", //
    booktitle = "Proc. 1990 ACM SIGMOD Int. Conf. Management of Data", //
    url = "https://doi.org/10.1145/93597.98741", //
    bibkey = "DBLP:conf/sigmod/BeckmannKSS90")
public class LimitedReinsertOverflowTreatment implements OverflowTreatment {
  /**
   * Default insert strategy used by R*-tree
   */
  public static final LimitedReinsertOverflowTreatment RSTAR_OVERFLOW = new LimitedReinsertOverflowTreatment(new CloseReinsert(0.3, SquaredEuclideanDistance.STATIC));

  /**
   * Bitset to keep track of levels a reinsert has been performed at.
   */
  private long[] reinsertions = new long[1];

  /**
   * Strategy for the actual reinsertions
   */
  private final ReinsertStrategy reinsertStrategy;

  /**
   * Constructor.
   * 
   * @param reinsertStrategy Reinsertion strategy
   */
  public LimitedReinsertOverflowTreatment(ReinsertStrategy reinsertStrategy) {
    super();
    this.reinsertStrategy = reinsertStrategy;
  }

  @Override
  public <N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> boolean handleOverflow(AbstractRStarTree<N, E, ?> tree, N node, IndexTreePath<E> path) {
    final int depthm1 = path.getPathCount() - 1;
    // No reinsertions at root level
    if(depthm1 == 0) {
      return false;
    }
    // Resize if necessary. TODO: do in reinitialize only?
    if(BitsUtil.capacity(reinsertions) < depthm1) {
      reinsertions = BitsUtil.copy(reinsertions, depthm1);
    }
    // Earlier reinsertions at the same level
    if(BitsUtil.get(reinsertions, depthm1)) {
      return false;
    }

    BitsUtil.setI(reinsertions, depthm1);
    final E entry = path.getEntry();
    assert !(entry instanceof LeafEntry) : "Unexpected leaf entry";
    int[] cands = reinsertStrategy.computeReinserts(node, NodeArrayAdapter.STATIC, entry);
    if(cands == null || cands.length == 0) {
      return false;
    }
    tree.reInsert(node, path, cands);
    return true;
  }

  @Override
  public void reinitialize() {
    BitsUtil.zeroI(reinsertions);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Fast-insertion parameter. Optional.
     */
    public static final OptionID REINSERT_STRATEGY_ID = new OptionID("rtree.reinsertion-strategy", "The strategy to select candidates for reinsertion.");

    /**
     * The actual reinsertion strategy
     */
    ReinsertStrategy reinsertStrategy = null;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<ReinsertStrategy>(REINSERT_STRATEGY_ID, ReinsertStrategy.class, CloseReinsert.class) //
          .grab(config, x -> reinsertStrategy = x);
    }

    @Override
    public LimitedReinsertOverflowTreatment make() {
      return new LimitedReinsertOverflowTreatment(reinsertStrategy);
    }
  }
}