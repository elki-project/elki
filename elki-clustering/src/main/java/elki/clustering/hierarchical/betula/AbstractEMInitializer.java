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
package elki.clustering.hierarchical.betula;

import java.util.List;

import elki.clustering.em.EMClusterModel;
import elki.clustering.hierarchical.betula.initialization.AbstractCFKMeansInitialization;
import elki.clustering.hierarchical.betula.initialization.CFKMeansPlusPlus;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Initializes selected EM Model.
 *
 * @author Andreas Lang
 */
public abstract class AbstractEMInitializer<M extends MeanModel> {
  /**
   * Class to choose the initial means
   */
  protected AbstractCFKMeansInitialization initializer;

  /**
   * Constructor.
   *
   * @param initializer k Means initialization algorithm.
   */
  public AbstractEMInitializer(AbstractCFKMeansInitialization initializer) {
    this.initializer = initializer;
  }

  /**
   * Build the initial models.
   * 
   * @param cfs List of clustering features
   * @param k Number of clusters.
   * @param root Summary statistic of the tree.
   * @return Initial models
   */
  public abstract List<? extends EMClusterModel<NumberVector, M>> buildInitialModels(List<? extends CFInterface> cfs, int k, CFTree<?> tree);

  /**
   * Parameterization class
   *
   * @author Andreas Lang
   */
  public abstract static class Par implements Parameterizer {
    /**
     * Parameter to specify the cluster center initialization.
     */
    public static final OptionID INIT_ID = new OptionID("em.centers", "Method to choose the initial cluster centers.");

    /**
     * Initialization method
     */
    protected AbstractCFKMeansInitialization initializer;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<AbstractCFKMeansInitialization>(INIT_ID, AbstractCFKMeansInitialization.class, CFKMeansPlusPlus.class) //
          .grab(config, x -> initializer = x);
    }
  }
}
