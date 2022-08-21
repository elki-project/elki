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

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.database.relation.Relation;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.math.linearalgebra.CovarianceMatrix;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Factory for EM with multivariate Gaussian models (with covariance; also known
 * as Gaussian Mixture Modeling, GMM).
 * <p>
 * These models have individual covariance matrixes, so this corresponds to the
 * {@code 'VVV'} model in Mclust (R).
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - TwoPassMultivariateGaussianModel
 */
public class TwoPassMultivariateGaussianModelFactory implements EMClusterModelFactory<NumberVector, EMModel> {
  /**
   * Class to choose the initial means
   */
  protected KMeansInitialization initializer;

  /**
   * Constructor.
   *
   * @param initializer Class for choosing the initial seeds.
   */
  public TwoPassMultivariateGaussianModelFactory(KMeansInitialization initializer) {
    super();
    this.initializer = initializer;
  }

  @Override
  public List<TwoPassMultivariateGaussianModel> buildInitialModels(Relation<? extends NumberVector> relation, int k) {
    double[][] initialMeans = initializer.chooseInitialMeans(relation, k, SquaredEuclideanDistance.STATIC);
    assert initialMeans.length == k;
    // Compute the global covariance matrix for better starting conditions:
    double[][] covmat = CovarianceMatrix.make(relation).destroyToPopulationMatrix();
    timesEquals(covmat, FastMath.pow(k, -2. / covmat.length));

    List<TwoPassMultivariateGaussianModel> models = new ArrayList<>(k);
    for(double[] nv : initialMeans) {
      models.add(new TwoPassMultivariateGaussianModel(1. / k, nv, covmat));
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
    public TwoPassMultivariateGaussianModelFactory make() {
      return new TwoPassMultivariateGaussianModelFactory(initializer);
    }
  }
}
