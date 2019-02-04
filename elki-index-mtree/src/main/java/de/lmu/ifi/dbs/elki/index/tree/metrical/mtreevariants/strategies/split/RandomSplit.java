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

import java.util.Random;

import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.distribution.Assignments;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.distribution.DistributionStrategy;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Encapsulates the required methods for a split of a node in an M-Tree. The
 * routing objects are chosen according to the RANDOM strategy.
 * <p>
 * Note: only the routing objects are chosen at random, this is not a random
 * assignment!
 * <p>
 * Reference:
 * <p>
 * P. Ciaccia, M. Patella, P. Zezula<br>
 * M-tree: An Efficient Access Method for Similarity Search in Metric Spaces<br>
 * In Proc. Int. Conf. Very Large Data Bases (VLDB'97)
 *
 * @author Elke Achtert
 * @since 0.2
 *
 * @param <E> the type of MTreeEntry used in the M-Tree
 * @param <N> the type of AbstractMTreeNode used in the M-Tree
 */
@Reference(authors = "P. Ciaccia, M. Patella, P. Zezula", //
    title = "M-tree: An Efficient Access Method for Similarity Search in Metric Spaces", //
    booktitle = "Proc. Int. Conf. Very Large Data Bases (VLDB'97)", //
    url = "http://www.vldb.org/conf/1997/P426.PDF", //
    bibkey = "DBLP:conf/vldb/CiacciaPZ97")
public class RandomSplit<E extends MTreeEntry, N extends AbstractMTreeNode<?, N, E>> extends AbstractMTreeSplit<E, N> {
  /**
   * Random generator.
   */
  private Random random;

  /**
   * Constructor.
   *
   * @param distributor Distribution strategy
   * @param rnd Random generator
   */
  public RandomSplit(DistributionStrategy distributor, RandomFactory rnd) {
    super(distributor);
    this.random = rnd.getSingleThreadedRandom();
  }

  /**
   * Selects two objects of the specified node to be promoted and stored into
   * the parent node. The m-RAD strategy considers all possible pairs of objects
   * and, after partitioning the set of entries, promotes the pair of objects
   * for which the sum of covering radiuses is minimum.
   * 
   * @param tree Tree to use
   * @param node the node to be split
   */
  @Override
  public Assignments<E> split(AbstractMTree<?, N, E, ?> tree, N node) {
    final int n = node.getNumEntries();
    int pos1 = random.nextInt(n), pos2 = random.nextInt(n - 1);
    pos2 = pos2 >= pos1 ? pos2 + 1 : pos2;

    // Build distance arrays:
    double[] dis1 = new double[n], dis2 = new double[n];
    E e1 = node.getEntry(pos1), e2 = node.getEntry(pos2);
    for(int i = 0; i < n; i++) {
      if(i == pos1 || i == pos2) {
        continue;
      }
      final E ej = node.getEntry(i);
      dis1[i] = tree.distance(e1, ej);
      dis2[i] = tree.distance(e2, ej);
    }
    return distributor.distribute(node, pos1, dis1, pos2, dis2);
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
    /**
     * Option ID for the random generator.
     */
    public static final OptionID RANDOM_ID = new OptionID("mtree.randomsplit.random", "Random generator / seed for the randomized split.");

    /**
     * Random generator
     */
    RandomFactory rnd = RandomFactory.DEFAULT;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      RandomParameter rndP = new RandomParameter(RANDOM_ID);
      if(config.grab(rndP)) {
        rnd = rndP.getValue();
      }
    }

    @Override
    protected RandomSplit<E, N> makeInstance() {
      return new RandomSplit<>(distributor, rnd);
    }
  }
}
