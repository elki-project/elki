package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.overflow;

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

import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.reinsert.CloseReinsert;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.reinsert.ReinsertStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.NodeArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Limited reinsertions, as proposed by the R*-Tree: For each real insert, allow
 * reinsertions to happen only once per level.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger", title = "The R*-tree: an efficient and robust access method for points and rectangles", booktitle = "Proceedings of the 1990 ACM SIGMOD International Conference on Management of Data, Atlantic City, NJ, May 23-25, 1990", url = "http://dx.doi.org/10.1145/93597.98741")
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
    assert (!entry.isLeafEntry()) : "Unexpected leaf entry";
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
   * 
   * @apiviz.exclude
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