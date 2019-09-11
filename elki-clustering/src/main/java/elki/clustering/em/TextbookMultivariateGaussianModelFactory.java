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
package elki.clustering.em;

import static elki.math.linearalgebra.VMath.copy;
import static elki.math.linearalgebra.VMath.timesEquals;

import java.util.ArrayList;
import java.util.List;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.math.linearalgebra.CovarianceMatrix;

import net.jafama.FastMath;

/**
 * Factory for EM with multivariate Gaussian model, using the textbook
 * algorithm. There is no reason to use this in practice, it is only useful to
 * study the reliability of the textbook approach.
 * <p>
 * "Textbook" refers to the E[XY]-E[X]E[Y] equation for covariance, that is
 * numerically not reliable with floating point math, but popular in textbooks.
 * <p>
 * Again, do not use this. Always prefer {@link MultivariateGaussianModelFactory}.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - TextbookMultivariateGaussianModel
 *
 * @param <V> vector type
 */
public class TextbookMultivariateGaussianModelFactory<V extends NumberVector> extends AbstractEMModelFactory<V, EMModel> {
  /**
   * Constructor.
   *
   * @param initializer Class for choosing the initial seeds.
   */
  public TextbookMultivariateGaussianModelFactory(KMeansInitialization initializer) {
    super(initializer);
  }

  @Override
  public List<TextbookMultivariateGaussianModel> buildInitialModels(Database database, Relation<V> relation, int k, NumberVectorDistance<? super V> df) {
    double[][] initialMeans = initializer.chooseInitialMeans(relation, k, df);
    assert (initialMeans.length == k);
    // Compute the global covariance matrix for better starting conditions:
    double[][] covmat = CovarianceMatrix.make(relation).destroyToSampleMatrix();
    timesEquals(covmat, FastMath.pow(k, -2. / covmat.length));

    List<TextbookMultivariateGaussianModel> models = new ArrayList<>(k);
    for(double[] nv : initialMeans) {
      models.add(new TextbookMultivariateGaussianModel(1. / k, nv, copy(covmat)));
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
  public static class Par<V extends NumberVector> extends AbstractEMModelFactory.Par<V> {
    @Override
    public TextbookMultivariateGaussianModelFactory<V> make() {
      return new TextbookMultivariateGaussianModelFactory<>(initializer);
    }
  }
}
