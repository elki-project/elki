/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
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
package elki.clustering.hierarchical.betula.initialization;

import elki.clustering.kmeans.initialization.AbstractKMeansInitialization;

import java.util.ArrayList;
import java.util.List;

import elki.clustering.hierarchical.betula.CFInterface;
import elki.clustering.hierarchical.betula.CFTree;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;
import elki.clustering.hierarchical.betula.CFTree.LeafIterator;

/**
 * Abstract base class for CF k-means initializations. For regular k-means use
 * {@link AbstractKMeansInitialization} instead.
 * 
 * @author Andreas Lang
 */
public abstract class AbstractCFKMeansInitialization {
  /**
   * Random number generator
   */
  protected RandomFactory rf;

  /**
   * Constructor.
   * 
   * @param rnd Random number generator.
   */
  public AbstractCFKMeansInitialization(RandomFactory rf) {
    this.rf = rf;
  }

  /**
   * Build the initial models.
   * 
   * @param cfs List of clustering features
   * @param k Number of clusters.
   * @param root Summary statistic of the tree.
   * @return
   */
  public abstract double[][] chooseInitialMeans(CFTree<?> tree, List<? extends CFInterface> cfs, int k);

  /**
   * Extract the leaves of the tree.
   *
   * @param tree Tree
   * @return Leaves
   */
  public static <L extends CFInterface> ArrayList<L> flattenTree(CFTree<L> tree) {
    ArrayList<L> cfs = new ArrayList<>(tree.getLeaves());
    for(LeafIterator<L> iter = tree.leafIterator(); iter.valid(); iter.advance()) {
      cfs.add(iter.get());
    }
    return cfs;
  }

  /**
   * Parameterization class.
   * 
   * @author Andreas Lang
   */
  public abstract static class Par implements Parameterizer {
    /**
     * Parameter to specify the random generator seed.
     */
    public static final OptionID SEED_ID = new OptionID("kmeans.seed", "The random number generator seed.");

    /**
     * Random generator
     */
    protected RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
    }
  }
}
