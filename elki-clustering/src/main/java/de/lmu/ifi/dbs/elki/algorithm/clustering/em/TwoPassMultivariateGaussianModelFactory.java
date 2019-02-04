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
package de.lmu.ifi.dbs.elki.algorithm.clustering.em;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.copy;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.timesEquals;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;

import net.jafama.FastMath;

/**
 * Factory for EM with multivariate Gaussian models (with covariance; also known
 * as Gaussian Mixture Modeling, GMM).
 *
 * These models have individual covariance matrixes, so this corresponds to the
 * {@code 'VVV'} model in Mclust (R).
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - TwoPassMultivariateGaussianModel
 *
 * @param <V> vector type
 */
public class TwoPassMultivariateGaussianModelFactory<V extends NumberVector> extends AbstractEMModelFactory<V, EMModel> {
  /**
   * Constructor.
   *
   * @param initializer Class for choosing the initial seeds.
   */
  public TwoPassMultivariateGaussianModelFactory(KMeansInitialization initializer) {
    super(initializer);
  }

  @Override
  public List<TwoPassMultivariateGaussianModel> buildInitialModels(Database database, Relation<V> relation, int k, NumberVectorDistanceFunction<? super V> df) {
    double[][] initialMeans = initializer.chooseInitialMeans(database, relation, k, df);
    assert (initialMeans.length == k);
    // Compute the global covariance matrix for better starting conditions:
    double[][] covmat = CovarianceMatrix.make(relation).destroyToSampleMatrix();
    timesEquals(covmat, FastMath.pow(k, -2. / covmat.length));

    List<TwoPassMultivariateGaussianModel> models = new ArrayList<>(k);
    for(double[] nv : initialMeans) {
      models.add(new TwoPassMultivariateGaussianModel(1. / k, nv, copy(covmat)));
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
    protected TwoPassMultivariateGaussianModelFactory<V> makeInstance() {
      return new TwoPassMultivariateGaussianModelFactory<>(initializer);
    }
  }
}
