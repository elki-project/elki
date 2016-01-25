package de.lmu.ifi.dbs.elki.algorithm.clustering.em;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Factory for EM with multivariate gaussian models using diagonal matrixes.
 *
 * These models have individual variances, but no covariance, so this
 * corresponds to the {@code 'VVI'} model in Mclust (R).
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @apiviz.has SphericalGaussianModel
 *
 * @param <V> vector type
 */
public class SphericalGaussianModelFactory<V extends NumberVector> extends AbstractEMModelFactory<V, EMModel> {
  /**
   * Constructor.
   *
   * @param initializer Class for choosing the inital seeds.
   */
  public SphericalGaussianModelFactory(KMeansInitialization<V> initializer) {
    super(initializer);
  }

  @Override
  public List<SphericalGaussianModel> buildInitialModels(Database database, Relation<V> relation, int k, NumberVectorDistanceFunction<? super V> df) {
    final List<Vector> initialMeans = initializer.chooseInitialMeans(database, relation, k, df, Vector.FACTORY);
    assert (initialMeans.size() == k);
    final int dimensionality = initialMeans.get(0).getDimensionality();
    final double norm = MathUtil.powi(MathUtil.TWOPI, dimensionality);
    List<SphericalGaussianModel> models = new ArrayList<>(k);
    for(Vector nv : initialMeans) {
      models.add(new SphericalGaussianModel(1. / k, nv, norm));
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
    protected SphericalGaussianModelFactory<V> makeInstance() {
      return new SphericalGaussianModelFactory<>(initializer);
    }
  }
}
