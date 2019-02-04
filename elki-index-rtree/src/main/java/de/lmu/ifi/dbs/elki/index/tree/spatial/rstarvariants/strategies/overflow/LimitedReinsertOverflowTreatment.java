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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.overflow;

import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.reinsert.CloseReinsert;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.reinsert.ReinsertStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.NodeArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
  public static final LimitedReinsertOverflowTreatment RSTAR_OVERFLOW = new LimitedReinsertOverflowTreatment(new CloseReinsert(0.3, SquaredEuclideanDistanceFunction.STATIC));

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
    // Earlier reinsertions at the same level
    if(BitsUtil.capacity(reinsertions) < depthm1) {
      reinsertions = BitsUtil.copy(reinsertions, depthm1);
    }
    if(BitsUtil.get(reinsertions, depthm1)) {
      return false;
    }

    BitsUtil.setI(reinsertions, depthm1);
    final E entry = path.getEntry();
    assert (!(entry instanceof LeafEntry)) : "Unexpected leaf entry";
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
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Fast-insertion parameter. Optional.
     */
    public static OptionID REINSERT_STRATEGY_ID = new OptionID("rtree.reinsertion-strategy", "The strategy to select candidates for reinsertion.");

    /**
     * The actual reinsertion strategy
     */
    ReinsertStrategy reinsertStrategy = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<ReinsertStrategy> strategyP = new ObjectParameter<>(REINSERT_STRATEGY_ID, ReinsertStrategy.class, CloseReinsert.class);
      if(config.grab(strategyP)) {
        reinsertStrategy = strategyP.instantiateClass(config);
      }
    }

    @Override
    protected LimitedReinsertOverflowTreatment makeInstance() {
      return new LimitedReinsertOverflowTreatment(reinsertStrategy);
    }
  }
}