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

import static elki.math.linearalgebra.VMath.timesEquals;

import java.util.ArrayList;
import java.util.List;

import elki.clustering.em.EMClusterModel;
import elki.clustering.em.MultivariateGaussianModel;
import elki.clustering.hierarchical.betula.initialization.AbstractCFKMeansInitialization;
import elki.data.NumberVector;
import elki.data.model.EMModel;

import net.jafama.FastMath;

/**
 * Factory for EM with multivariate gaussian models using diagonal matrixes.
 * 
 * @author Andreas Lang
 */
public class EMMultivariateInitializer extends AbstractEMInitializer<NumberVector, EMModel> {
  /**
   * Constructor.
   *
   * @param initializer Class for choosing the inital seeds.
   */
  public EMMultivariateInitializer(AbstractCFKMeansInitialization initializer) {
    super(initializer);
  }

  @Override
  public List<? extends EMClusterModel<NumberVector, EMModel>> buildInitialModels(CFInterface[] cfs, int k, CFTree<CFInterface> tree) {
    double[][] initialMeans = initializer.chooseInitialMeans(tree, cfs, k);
    assert (initialMeans.length == k);
    double[][] covmat = tree.root.covariance().clone();
    timesEquals(covmat, FastMath.pow(k, -2. / covmat.length));

    List<MultivariateGaussianModel> models = new ArrayList<>(k);
    for(double[] nv : initialMeans) {
      models.add(new MultivariateGaussianModel(1. / k, nv, covmat));
    }
    return models;
  }

  public static class Par<V extends NumberVector> extends AbstractEMInitializer.Par<V> {
    @Override
    public EMMultivariateInitializer make() {
      return new EMMultivariateInitializer(initializer);
    }
  }
}
