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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split;

import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.distribution.Assignments;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.distribution.DistributionStrategy;

/**
 * Farthest points split.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @param <E> the type of MTreeEntry used in the M-Tree
 * @param <N> the type of AbstractMTreeNode used in the M-Tree
 */
public class FarthestPointsSplit<E extends MTreeEntry, N extends AbstractMTreeNode<?, N, E>> extends AbstractMTreeSplit<E, N> {
  /**
   * Constructor.
   *
   * @param distributor Distribution strategy
   */
  public FarthestPointsSplit(DistributionStrategy distributor) {
    super(distributor);
  }

  @Override
  public Assignments<E> split(AbstractMTree<?, N, E, ?> tree, N node) {
    final int n = node.getNumEntries();
    double[][] distanceMatrix = computeDistanceMatrix(tree, node);

    // choose first and second routing object
    int besti = -1, bestj = -1;
    double currentMaxDist = Double.NEGATIVE_INFINITY;
    for(int i = 0; i < n; i++) {
      double[] row_i = distanceMatrix[i];
      for(int j = i + 1; j < n; j++) {
        double distance = row_i[j];
        if(distance > currentMaxDist) {
          besti = i;
          bestj = j;
          currentMaxDist = distance;
        }
      }
    }
    return distributor.distribute(node, besti, distanceMatrix[besti], bestj, distanceMatrix[bestj]);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <E> the type of MTreeEntry used in the M-Tree
   * @param <N> the type of AbstractMTreeNode used in the M-Tree
   */
  public static class Parameterizer<E extends MTreeEntry, N extends AbstractMTreeNode<?, N, E>> extends AbstractMTreeSplit.Parameterizer<E, N> {
    @Override
    protected FarthestPointsSplit<E, N> makeInstance() {
      return new FarthestPointsSplit<>(distributor);
    }
  }
}
