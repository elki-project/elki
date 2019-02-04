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
package de.lmu.ifi.dbs.elki.data.projection.random;

import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Random projections using Cauchy distributions (1-stable).
 * <p>
 * Reference:
 * <p>
 * M. Datar, N. Immorlica, P. Indyk, V. S. Mirrokni<br>
 * Locality-sensitive hashing scheme based on p-stable distributions.<br>
 * Proc. 20th Annual Symposium on Computational Geometry
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "M. Datar, N. Immorlica, P. Indyk, V. S. Mirrokni", //
    title = "Locality-sensitive hashing scheme based on p-stable distributions", //
    booktitle = "Proc. 20th Annual Symposium on Computational Geometry", //
    url = "https://doi.org/10.1145/997817.997857", //
    bibkey = "DBLP:conf/compgeom/DatarIIM04")
@Alias("de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections.GaussianRandomProjectionFamily")
public class GaussianRandomProjectionFamily extends AbstractRandomProjectionFamily {
  /**
   * Constructor.
   *
   * @param random Random number generator.
   */
  public GaussianRandomProjectionFamily(RandomFactory random) {
    super(random);
  }

  @Override
  public Projection generateProjection(int idim, int odim) {
    double[][] matrix = new double[odim][idim];
    for(int i = 0; i < odim; ++i) {
      double[] row = matrix[i];
      for(int j = 0; j < idim; ++j) {
        row[j] = random.nextGaussian();
      }
    }
    return new MatrixProjection(matrix);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractRandomProjectionFamily.Parameterizer {
    @Override
    protected GaussianRandomProjectionFamily makeInstance() {
      return new GaussianRandomProjectionFamily(random);
    }
  }
}
