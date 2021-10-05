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

import java.util.ArrayList;
import java.util.List;

import elki.clustering.em.DiagonalGaussianModel;
import elki.clustering.em.EMClusterModel;
import elki.clustering.hierarchical.betula.initialization.AbstractCFKMeansInitialization;
import elki.data.NumberVector;
import elki.data.model.EMModel;

import net.jafama.FastMath;

/**
 * Factory for EM with multivariate gaussian models using diagonal matrixes.
 * 
 * @author Andreas Lang
 */
public class EMDiagonalInitializer extends AbstractEMInitializer<EMModel> {
  /**
   * Constructor.
   *
   * @param initializer Class for choosing the inital seeds.
   */
  public EMDiagonalInitializer(AbstractCFKMeansInitialization initializer) {
    super(initializer);
  }

  @Override
  public List<? extends EMClusterModel<NumberVector, EMModel>> buildInitialModels(List<? extends CFInterface> cfs, int k, CFTree<?> tree) {
    final int dim = cfs.get(0).getDimensionality();
    double[][] initialMeans = initializer.chooseInitialMeans(tree, cfs, k);
    assert (initialMeans.length == k);
    double[] variances = new double[dim];
    final double f = FastMath.pow(k, -2. / variances.length);
    for(int d = 0; d < dim; d++) {
      variances[d] = tree.root.getCF().variance(d) * f;
    }

    List<DiagonalGaussianModel> models = new ArrayList<>(k);
    for(double[] nv : initialMeans) {
      models.add(new DiagonalGaussianModel(1. / k, nv, variances.clone()));
    }
    return models;
  }

  /**
   * Parameterization class
   *
   * @author Andreas Lang
   */
  public static class Par extends AbstractEMInitializer.Par {
    @Override
    public EMDiagonalInitializer make() {
      return new EMDiagonalInitializer(initializer);
    }
  }
}
