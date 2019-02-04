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
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Encapsulates the required methods for a split of a node in an M-Tree.
 * The routing objects are chosen according to the MLBDIST strategy.
 * <p>
 * The benefit of this strategy is that it works with precomputed distances from
 * the parent, while most other strategies would require O(n²) distance
 * computations. So if construction time is critical, this is a good choice.
 * <p>
 * Reference:
 * <p>
 * P. Ciaccia, M. Patella, P. Zezula<br>
 * M-tree: An Efficient Access Method for Similarity Search in Metric
 * Spaces<br>
 * Proc. Int. Conf. Very Large Data Bases (VLDB'97)
 *
 * @author Elke Achtert
 * @since 0.6.0
 *
 * @param <E> the type of MTreeEntry used in the M-Tree
 * @param <N> the type of AbstractMTreeNode used in the M-Tree
 */
@Priority(Priority.RECOMMENDED)
@Reference(authors = "P. Ciaccia, M. Patella, P. Zezula", //
    title = "M-tree: An Efficient Access Method for Similarity Search in Metric Spaces", //
    booktitle = "Proc. Int. Conf. Very Large Data Bases (VLDB'97)", //
    url = "http://www.vldb.org/conf/1997/P426.PDF", //
    bibkey = "DBLP:conf/vldb/CiacciaPZ97")
@Alias("de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.split.MLBDistSplit")
public class MLBDistSplit<E extends MTreeEntry, N extends AbstractMTreeNode<?, N, E>> extends AbstractMTreeSplit<E, N> {
  /**
   * Constructor.
   *
   * @param distributor Distribution strategy
   */
  public MLBDistSplit(DistributionStrategy distributor) {
    super(distributor);
  }

  /**
   * Selects the second object of the specified node to be promoted and stored
   * into the parent node and partitions the entries according to the M_LB_DIST
   * strategy.
   * <p>
   * This strategy considers all possible pairs of objects and chooses the pair
   * of objects for which the distance is maximum.
   * 
   * @param tree Tree to use
   * @param node the node to be split
   */
  @Override
  public Assignments<E> split(AbstractMTree<?, N, E, ?> tree, N node) {
    final int n = node.getNumEntries();

    // choose first and second routing object
    int b1 = 0, b2 = 0;
    double dist1 = node.getEntry(0).getParentDistance(), dist2 = dist1;
    if(Double.isNaN(dist1)) {
      // Root split does not yet have parent distances!
      // As a fallback, use the two farthest points.
      return new FarthestPointsSplit<E, N>(distributor).split(tree, node);
    }
    for(int i = 1; i < n; i++) {
      double dist = node.getEntry(i).getParentDistance();
      if(dist < dist1) {
        dist1 = dist;
        b1 = i;
      }
      else if(dist > dist2 || (dist == dist2 && i == 1)) {
        dist2 = dist;
        b2 = i;
      }
    }
    E e1 = node.getEntry(b1), e2 = node.getEntry(b2);
    double[] rowi = new double[n], rowj = new double[n];
    for(int i = 0; i < n; i++) {
      if(i == b1 || i == b2) {
        continue;
      }
      E ei = node.getEntry(i);
      rowi[i] = tree.distance(e1, ei);
      rowj[i] = tree.distance(e2, ei);
    }

    return distributor.distribute(node, b1, rowi, b2, rowj);
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
    protected MLBDistSplit<E, N> makeInstance() {
      return new MLBDistSplit<>(distributor);
    }
  }
}
