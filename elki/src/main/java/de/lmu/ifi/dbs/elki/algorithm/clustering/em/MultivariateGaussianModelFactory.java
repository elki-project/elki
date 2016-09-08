package de.lmu.ifi.dbs.elki.algorithm.clustering.em;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.EMModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;

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
 * @apiviz.has MultivariateGaussianModel
 *
 * @param <V> vector type
 */
public class MultivariateGaussianModelFactory<V extends NumberVector> extends AbstractEMModelFactory<V, EMModel> {
  /**
   * Constructor.
   *
   * @param initializer Class for choosing the initial seeds.
   */
  public MultivariateGaussianModelFactory(KMeansInitialization<V> initializer) {
    super(initializer);
  }

  @Override
  public List<MultivariateGaussianModel> buildInitialModels(Database database, Relation<V> relation, int k, NumberVectorDistanceFunction<? super V> df) {
    double[][] initialMeans = initializer.chooseInitialMeans(database, relation, k, df);
    assert (initialMeans.length == k);
    final int dimensionality = initialMeans[0].length;
    final double norm = MathUtil.powi(MathUtil.TWOPI, dimensionality);
    // Compute the global covariance matrix for better starting conditions:
    double[][] covmat = CovarianceMatrix.make(relation).destroyToSampleMatrix();
    VMath.times(covmat, 1. / k);

    List<MultivariateGaussianModel> models = new ArrayList<>(k);
    for(double[] nv : initialMeans) {
      models.add(new MultivariateGaussianModel(1. / k, nv, norm, VMath.copy(covmat)));
    }
    return models;
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
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
