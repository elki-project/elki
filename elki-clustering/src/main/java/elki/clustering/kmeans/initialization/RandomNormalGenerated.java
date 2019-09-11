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
package elki.clustering.kmeans.initialization;

import java.util.Random;

import elki.data.NumberVector;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.math.MeanVariance;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.random.RandomFactory;

/**
 * Initialize k-means by generating random vectors (normal distributed
 * with \(N(\mu,\sigma)\) in each dimension).
 * <p>
 * This is a different interpretation of the work of Jancey, who wrote little
 * more details but "introduced into known but arbitrary positions"; but
 * seemingly worked with standardized scores. In contrast to
 * {@link RandomUniformGenerated} (which uses a uniform on the entire
 * value range), this class uses a normal distribution based on the estimated
 * parameters. The resulting means should be more central, and thus a bit less
 * likely to become empty (at least if you assume there is no correlation
 * amongst attributes... it is still not competitive with better methods).
 * <p>
 * <b>Warning:</b> this still tends to produce empty clusters in many
 * situations, and is one of the least effective initialization strategies, not
 * recommended for use.
 * <p>
 * Reference:
 * <p>
 * R. C. Jancey<br>
 * Multidimensional group analysis<br>
 * Australian Journal of Botany 14(1)
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
@Priority(Priority.SUPPLEMENTARY - 1)
@Reference(authors = "R. C. Jancey", //
    title = "Multidimensional group analysis", //
    booktitle = "Australian Journal of Botany 14(1)", //
    url = "https://doi.org/10.1071/BT9660127", //
    bibkey = "doi:10.1071/BT9660127")
public class RandomNormalGenerated extends AbstractKMeansInitialization {
  /**
   * Constructor.
   *
   * @param rnd Random generator.
   */
  public RandomNormalGenerated(RandomFactory rnd) {
    super(rnd);
  }

  @Override
  public double[][] chooseInitialMeans(Relation<? extends NumberVector> relation, int k, NumberVectorDistance<?> distanceFunction) {
    final int dim = RelationUtil.dimensionality(relation);
    MeanVariance[] mvs = MeanVariance.newArray(dim);
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      NumberVector v = relation.get(it);
      for(int i = 0; i < dim; i++) {
        mvs[i].put(v.doubleValue(i));
      }
    }
    double[] min = new double[dim], scale = new double[dim];
    for(int d = 0; d < dim; d++) {
      final double sigma = mvs[d].getSampleStddev();
      min[d] = mvs[d].getMean();
      scale[d] = sigma;
    }
    double[][] means = new double[k][];
    final Random random = rnd.getSingleThreadedRandom();
    for(int i = 0; i < k; i++) {
      double[] r = new double[dim];
      for(int d = 0; d < dim; d++) {
        r[d] = min[d] + scale[d] * random.nextGaussian();
      }
      means[i] = r;
    }
    return means;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par extends AbstractKMeansInitialization.Par {
    @Override
    public RandomNormalGenerated make() {
      return new RandomNormalGenerated(rnd);
    }
  }
}
