package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.overflow;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.util.BitSet;
import java.util.List;

import de.lmu.ifi.dbs.elki.distance.distancefunction.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.reinsert.CloseReinsert;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.reinsert.ReinsertStrategy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayUtil;
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
  private BitSet reinsertions = new BitSet();

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
  public <N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> boolean handleOverflow(AbstractRStarTree<N, E> tree, N node, IndexTreePath<E> path) {
    final int level = tree.getHeight() - (path.getPathCount() - 1);
    // No reinsertions at root level
    if(path.getPathCount() == 1) {
      return false;
    }
    // Earlier reinsertions at the same level
    if(reinsertions.get(level)) {
      return false;
    }

    reinsertions.set(level);
    List<E> entries = node.getEntries();
    int[] cands = reinsertStrategy.computeReinserts(entries, ArrayUtil.listAdapter(entries), node);
    if(cands == null || cands.length == 0) {
      return false;
    }
    tree.reInsert(node, path, cands);
    return true;
  }

  @Override
  public void reinitialize() {
    reinsertions.clear();
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
    public static OptionID REINSERT_STRATEGY_ID = OptionID.getOrCreateOptionID("rtree.reinsertion-strategy", "The strategy to select candidates for reinsertion.");

    /**
     * The actual reinsertion strategy
     */
    ReinsertStrategy reinsertStrategy = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<ReinsertStrategy> strategyP = new ObjectParameter<ReinsertStrategy>(REINSERT_STRATEGY_ID, ReinsertStrategy.class, CloseReinsert.class);
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