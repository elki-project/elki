package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split;

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

import java.util.Random;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Encapsulates the required methods for a split of a node in an M-Tree. The
 * routing objects are chosen according to the RANDOM strategy.
 * 
 * Note: only the routing objects are chosen at random, this is not a random
 * assignment!
 * 
 * Reference:
 * <p>
 * P. Ciaccia, M. Patella, P. Zezula<br />
 * M-tree: An Efficient Access Method for Similarity Search in Metric Spaces<br />
 * In Proceedings of 23rd International Conference on Very Large Data Bases
 * (VLDB'97), August 25-29, 1997, Athens, Greece
 * </p>
 * 
 * @author Elke Achtert
 * @since 0.2
 * 
 * @param <O> the type of DatabaseObject to be stored in the M-Tree
 * @param <N> the type of AbstractMTreeNode used in the M-Tree
 * @param <E> the type of MetricalEntry used in the M-Tree
 */
@Reference(authors = "P. Ciaccia, M. Patella, P. Zezula", //
title = "M-tree: An Efficient Access Method for Similarity Search in Metric Spaces", //
booktitle = "VLDB'97, Proceedings of 23rd International Conference on Very Large Data Bases, August 25-29, 1997, Athens, Greece", //
url = "http://www.vldb.org/conf/1997/P426.PDF")
public class RandomSplit<O, N extends AbstractMTreeNode<O, N, E>, E extends MTreeEntry> extends MTreeSplit<O, N, E> {
  /**
   * Random generator.
   */
  private Random random;

  /**
   * Creates a new split object.
   */
  public RandomSplit(RandomFactory rnd) {
    super();
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
  public Assignments<E> split(AbstractMTree<O, N, E, ?> tree, N node) {
    final int n = node.getNumEntries();
    int pos1 = random.nextInt(n);
    int pos2 = random.nextInt(n - 1);
    if(pos2 >= pos1) {
      ++pos2;
    }
    DBID id1 = node.getEntry(pos1).getRoutingObjectID();
    DBID id2 = node.getEntry(pos2).getRoutingObjectID();
    return balancedPartition(tree, node, id1, id2);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <O> the type of DatabaseObject to be stored in the M-Tree
   * @param <N> the type of AbstractMTreeNode used in the M-Tree
   * @param <E> the type of MetricalEntry used in the M-Tree
   */
  public static class Parameterizer<O, N extends AbstractMTreeNode<O, N, E>, E extends MTreeEntry> extends AbstractParameterizer {
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
    protected RandomSplit<O, N, E> makeInstance() {
      return new RandomSplit<>(rnd);
    }
  }
}
