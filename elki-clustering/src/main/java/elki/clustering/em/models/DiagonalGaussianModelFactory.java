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
package elki.clustering.em.models;

import java.util.ArrayList;
import java.util.List;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.database.relation.Relation;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.math.MeanVariance;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Factory for EM with multivariate gaussian models using diagonal matrixes.
 * <p>
 * These models have individual variances, but no covariance, so this
 * corresponds to the {@code 'VVI'} model in Mclust (R).
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - DiagonalGaussianModel
 */
public class DiagonalGaussianModelFactory implements EMClusterModelFactory<NumberVector, EMModel> {
  /**
   * Class to choose the initial means
   */
  protected KMeansInitialization initializer;

  /**
   * Constructor.
   *
   * @param initializer Class for choosing the initial seeds.
   */
  public DiagonalGaussianModelFactory(KMeansInitialization initializer) {
    super();
    this.initializer = initializer;
  }

  @Override
  public List<DiagonalGaussianModel> buildInitialModels(Relation<? extends NumberVector> relation, int k) {
    double[][] initialMeans = initializer.chooseInitialMeans(relation, k, SquaredEuclideanDistance.STATIC);
    assert (initialMeans.length == k);
    MeanVariance[] mvs = MeanVariance.of(relation);
    double[] variances = new double[mvs.length];
    final double f = FastMath.pow(k, -2. / variances.length);
    for(int d = 0; d < mvs.length; d++) {
      final double v = mvs[d].getPopulationVariance();
      variances[d] = v > 0 ? v * f : 1e-10;
    }

    List<DiagonalGaussianModel> models = new ArrayList<>(k);
    for(double[] nv : initialMeans) {
      models.add(new DiagonalGaussianModel(1. / k, nv, variances));
    }
    return models;
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @hidden
   */
  public static class Par implements Parameterizer {
    /**
     * Initialization method
     */
    protected KMeansInitialization initializer;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<KMeansInitialization>(INIT_ID, KMeansInitialization.class, RandomlyChosen.class) //
          .grab(config, x -> initializer = x);
    }

    @Override
    public DiagonalGaussianModelFactory make() {
      return new DiagonalGaussianModelFactory(initializer);
    }
  }
}
