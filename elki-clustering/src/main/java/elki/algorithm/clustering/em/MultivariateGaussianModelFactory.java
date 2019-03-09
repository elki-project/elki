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
package elki.algorithm.clustering.em;

import static elki.math.linearalgebra.VMath.copy;
import static elki.math.linearalgebra.VMath.timesEquals;

import java.util.ArrayList;
import java.util.List;

import elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.distance.distancefunction.NumberVectorDistanceFunction;
import elki.math.linearalgebra.CovarianceMatrix;

import net.jafama.FastMath;

/**
 * Factory for EM with multivariate Gaussian models (with covariance; also known
 * as Gaussian Mixture Modeling, GMM).
 *
 * These models have individual covariance matrixes, so this corresponds to the
 * {@code 'VVV'} model in Mclust (R).
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - MultivariateGaussianModel
 *
 * @param <V> vector type
 */
public class MultivariateGaussianModelFactory<V extends NumberVector> extends AbstractEMModelFactory<V, EMModel> {
  /**
   * Constructor.
   *
   * @param initializer Class for choosing the initial seeds.
   */
  public MultivariateGaussianModelFactory(KMeansInitialization initializer) {
    super(initializer);
  }

  @Override
  public List<MultivariateGaussianModel> buildInitialModels(Database database, Relation<V> relation, int k, NumberVectorDistanceFunction<? super V> df) {
    double[][] initialMeans = initializer.chooseInitialMeans(database, relation, k, df);
    assert (initialMeans.length == k);
    // Compute the global covariance matrix for better starting conditions:
    double[][] covmat = CovarianceMatrix.make(relation).destroyToSampleMatrix();
    timesEquals(covmat, FastMath.pow(k, -2. / covmat.length));

    List<MultivariateGaussianModel> models = new ArrayList<>(k);
    for(double[] nv : initialMeans) {
      models.add(new MultivariateGaussianModel(1. / k, nv, copy(covmat)));
    }
    return models;
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <V> Vector type
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractEMModelFactory.Parameterizer<V> {
    @Override
    protected MultivariateGaussianModelFactory<V> makeInstance() {
      return new MultivariateGaussianModelFactory<>(initializer);
    }
  }
}
