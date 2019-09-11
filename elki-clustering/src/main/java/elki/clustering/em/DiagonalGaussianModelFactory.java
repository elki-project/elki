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

import java.util.ArrayList;
import java.util.List;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.math.MeanVariance;

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
 *
 * @param <V> vector type
 */
public class DiagonalGaussianModelFactory<V extends NumberVector> extends AbstractEMModelFactory<V, EMModel> {
  /**
   * Constructor.
   *
   * @param initializer Class for choosing the inital seeds.
   */
  public DiagonalGaussianModelFactory(KMeansInitialization initializer) {
    super(initializer);
  }

  @Override
  public List<DiagonalGaussianModel> buildInitialModels(Database database, Relation<V> relation, int k, NumberVectorDistance<? super V> df) {
    double[][] initialMeans = initializer.chooseInitialMeans(relation, k, df);
    assert (initialMeans.length == k);
    final int dim = RelationUtil.dimensionality(relation);
    MeanVariance[] mvs = MeanVariance.newArray(dim);
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      NumberVector v = relation.get(it);
      for(int d = 0; d < dim; d++) {
        mvs[d].put(v.doubleValue(d));
      }
    }
    double[] variances = new double[dim];
    final double f = FastMath.pow(k, -2. / variances.length);
    for(int d = 0; d < dim; d++) {
      variances[d] = mvs[d].getSampleVariance() * f;
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
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <V> Vector type
   */
  public static class Par<V extends NumberVector> extends AbstractEMModelFactory.Par<V> {
    @Override
    public DiagonalGaussianModelFactory<V> make() {
      return new DiagonalGaussianModelFactory<>(initializer);
    }
  }
}
