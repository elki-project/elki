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
package elki.index.tree.metrical.mtreevariants.strategies.split;

import elki.index.tree.metrical.mtreevariants.AbstractMTree;
import elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import elki.index.tree.metrical.mtreevariants.MTreeEntry;
import elki.index.tree.metrical.mtreevariants.strategies.split.distribution.DistributionStrategy;
import elki.index.tree.metrical.mtreevariants.strategies.split.distribution.GeneralizedHyperplaneDistribution;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract super class for splitting a node in an M-Tree.
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @has - - - Assignments
 *
 * @param <E> the type of MTreeEntry used in the M-Tree
 * @param <N> the type of AbstractMTreeNode used in the M-Tree
 */
public abstract class AbstractMTreeSplit<E extends MTreeEntry, N extends AbstractMTreeNode<?, N, E>> implements MTreeSplit<E, N> {
  /**
   * Entry distribution strategy.
   */
  protected DistributionStrategy distributor;

  /**
   * Constructor.
   *
   * @param distributor Entry distribution strategy
   */
  public AbstractMTreeSplit(DistributionStrategy distributor) {
    this.distributor = distributor;
  }

  /**
   * Compute the pairwise distances in the given node.
   * 
   * @param tree Tree
   * @param node Node
   * @return Distance matrix
   */
  protected static <E extends MTreeEntry, N extends AbstractMTreeNode<?, N, E>> double[][] computeDistanceMatrix(AbstractMTree<?, N, E, ?> tree, N node) {
    final int n = node.getNumEntries();
    double[][] distancematrix = new double[n][n];
    // Build distance matrix
    for(int i = 0; i < n; i++) {
      E ei = node.getEntry(i);
      double[] row_i = distancematrix[i];
      for(int j = i + 1; j < n; j++) {
        row_i[j] = distancematrix[j][i] = tree.distance(ei, node.getEntry(j));
      }
    }
    return distancematrix;
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <E> the type of MTreeEntry used in the M-Tree
   * @param <N> the type of AbstractMTreeNode used in the M-Tree
   */
  public static abstract class Par<E extends MTreeEntry, N extends AbstractMTreeNode<?, N, E>> implements Parameterizer {
    /**
     * Distribution strategy parameter.
     */
    public static final OptionID DISTRIBUTOR_ID = new OptionID("mtree.split.distributor", "Distribution strategy for mtree entries during splitting.");

    /**
     * Entry distribution strategy.
     */
    protected DistributionStrategy distributor;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<DistributionStrategy>(DISTRIBUTOR_ID, DistributionStrategy.class, GeneralizedHyperplaneDistribution.class) //
          .grab(config, x -> distributor = x);
    }

    @Override
    abstract public MTreeSplit<E, N> make();
  }
}
