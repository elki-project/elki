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
package elki.clustering.kmeans.initialization.betula;

import java.util.List;

import elki.clustering.kmeans.AbstractKMeans;
import elki.clustering.kmeans.initialization.AbstractKMeansInitialization;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Abstract base class for CF k-means initializations. For regular k-means use
 * {@link AbstractKMeansInitialization} instead.
 * 
 * @author Andreas Lang
 * @since 0.8.0
 */
public abstract class AbstractCFKMeansInitialization {
  /**
   * Random number generator
   */
  protected RandomFactory rf;

  /**
   * Constructor.
   * 
   * @param rf Random number generator
   */
  public AbstractCFKMeansInitialization(RandomFactory rf) {
    this.rf = rf;
  }

  /**
   * Build the initial models.
   * 
   * @param tree CF tree
   * @param cfs Cluster features of the tree (may be ignored for tree-based
   *        initializations, should be an array list for efficiency)
   * @param k Number of clusters.
   * @return initial cluster means
   */
  public abstract double[][] chooseInitialMeans(CFTree<?> tree, List<? extends ClusterFeature> cfs, int k);

  /**
   * Parameterization class.
   * 
   * @author Andreas Lang
   */
  public abstract static class Par implements Parameterizer {
    /**
     * Parameter to specify the random generator seed.
     */
    public static final OptionID SEED_ID = AbstractKMeans.SEED_ID;

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
