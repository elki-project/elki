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

import elki.clustering.em.SphericalGaussianModel;
import elki.clustering.hierarchical.betula.initialization.AbstractCFKMeansInitialization;
import elki.data.model.EMModel;

import net.jafama.FastMath;

/**
 * Factory for EM with multivariate gaussian models using a single variance.
 * 
 * @author Andreas Lang
 */
public class EMSphericalInitializer extends AbstractEMInitializer<EMModel> {
  /**
   * Constructor.
   *
   * @param initializer Class for choosing the inital seeds.
   */
  public EMSphericalInitializer(AbstractCFKMeansInitialization initializer) {
    super(initializer);
  }

  @Override
  public List<SphericalGaussianModel> buildInitialModels(List<? extends CFInterface> cfs, int k, CFTree<?> tree) {
    final int dim = cfs.get(0).getDimensionality();
    double[][] initialMeans = initializer.chooseInitialMeans(tree, cfs, k);
    assert (initialMeans.length == k);
    double varsum = 0.;
    for(int d = 0; d < dim; d++) {
      varsum += tree.root.variance(d);
    }
    varsum *= FastMath.pow(k, -2. / dim); // Initial variance estimate

    List<SphericalGaussianModel> models = new ArrayList<>(k);
    for(double[] nv : initialMeans) {
      models.add(new SphericalGaussianModel(1. / k, nv, varsum));
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
    public EMSphericalInitializer make() {
      return new EMSphericalInitializer(initializer);
    }
  }
}
