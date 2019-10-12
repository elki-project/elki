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
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.random.RandomFactory;

/**
 * Initialize k-means by generating random vectors (uniform, within the value
 * range of the data set).
 * <p>
 * This is attributed to Jancey, but who wrote little more details but
 * "introduced into known but arbitrary positions". This class assumes this
 * refers to uniform positions within the value domain. For a normal distributed
 * variant, see {@link RandomNormalGenerated}.
 * <p>
 * <b>Warning:</b> this tends to produce empty clusters, and is one of the least
 * effective initialization strategies, not recommended for use.
 * <p>
 * Reference:
 * <p>
 * R. C. Jancey<br>
 * Multidimensional group analysis<br>
 * Australian Journal of Botany 14(1)
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Priority(Priority.SUPPLEMENTARY - 1)
@Reference(authors = "R. C. Jancey", //
    title = "Multidimensional group analysis", //
    booktitle = "Australian Journal of Botany 14(1)", //
    url = "https://doi.org/10.1071/BT9660127", //
    bibkey = "doi:10.1071/BT9660127")
public class RandomUniformGenerated extends AbstractKMeansInitialization {
  /**
   * Constructor.
   *
   * @param rnd Random generator.
   */
  public RandomUniformGenerated(RandomFactory rnd) {
    super(rnd);
  }

  @Override
  public double[][] chooseInitialMeans(Relation<? extends NumberVector> relation, int k, NumberVectorDistance<?> distance) {
    double[][] minmax = RelationUtil.computeMinMax(relation);
    final int dim = minmax[0].length;
    double[] min = minmax[0], scale = minmax[1];
    for(int d = 0; d < dim; d++) {
      scale[d] = scale[d] - min[d];
    }
    double[][] means = new double[k][];
    final Random random = rnd.getSingleThreadedRandom();
    for(int i = 0; i < k; i++) {
      double[] r = new double[dim];
      for(int d = 0; d < dim; d++) {
        r[d] = min[d] + scale[d] * random.nextDouble();
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
    public RandomUniformGenerated make() {
      return new RandomUniformGenerated(rnd);
    }
  }
}
