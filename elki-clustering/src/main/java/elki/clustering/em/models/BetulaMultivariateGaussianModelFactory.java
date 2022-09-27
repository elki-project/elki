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
package elki.clustering.em.models;

import static elki.math.linearalgebra.VMath.timesEquals;

import java.util.ArrayList;
import java.util.List;

import elki.clustering.kmeans.initialization.betula.AbstractCFKMeansInitialization;
import elki.clustering.kmeans.initialization.betula.CFKPlusPlusLeaves;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Factory for EM with multivariate gaussian models using diagonal matrixes.
 * <p>
 * References:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees<br>
 * Information Systems
 * 
 * @author Andreas Lang
 * @since 0.8.0
 */
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS22")
public class BetulaMultivariateGaussianModelFactory implements BetulaClusterModelFactory<MultivariateGaussianModel> {
  /**
   * Class to choose the initial means
   */
  protected AbstractCFKMeansInitialization initializer;

  /**
   * Constructor.
   *
   * @param initializer Class for choosing the initial seeds.
   */
  public BetulaMultivariateGaussianModelFactory(AbstractCFKMeansInitialization initializer) {
    this.initializer = initializer;
  }

  @Override
  public List<MultivariateGaussianModel> buildInitialModels(List<? extends ClusterFeature> cfs, int k, CFTree<?> tree) {
    double[][] initialMeans = initializer.chooseInitialMeans(tree, cfs, k);
    assert (initialMeans.length == k);
    double[][] covmat = tree.getRoot().getCF().covariance().clone();
    timesEquals(covmat, FastMath.pow(k, -2. / covmat.length));

    List<MultivariateGaussianModel> models = new ArrayList<>(k);
    for(double[] nv : initialMeans) {
      models.add(new MultivariateGaussianModel(1. / k, nv, covmat));
    }
    return models;
  }

  /**
   * Parameterization class
   * 
   * @author Andreas Lang
   */
  public static class Par implements Parameterizer {
    /**
     * Initialization method
     */
    protected AbstractCFKMeansInitialization initializer;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<AbstractCFKMeansInitialization>(INIT_ID, AbstractCFKMeansInitialization.class, CFKPlusPlusLeaves.class) //
          .grab(config, x -> initializer = x);
    }

    @Override
    public BetulaMultivariateGaussianModelFactory make() {
      return new BetulaMultivariateGaussianModelFactory(initializer);
    }
  }
}
