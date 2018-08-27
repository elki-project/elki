/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
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
import net.jafama.FastMath;

/**
 * Random projections using Cauchy distributions (1-stable).
 *
 * Reference:
 * <p>
 * M. Datar and N. Immorlica and P. Indyk and V. S. Mirrokni<br />
 * Locality-sensitive hashing scheme based on p-stable distributions.<br />
 * In Proc. 20th Symposium on Computational Geometry
 * </p>
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "M. Datar and N. Immorlica and P. Indyk and V. S. Mirrokni", //
    title = "Locality-sensitive hashing scheme based on p-stable distributions", //
    booktitle = "Proc. 20th Symposium on Computational Geometry", //
    url = "https://doi.org/10.1145/997817.997857", //
    bibkey = "DBLP:conf/compgeom/DatarIIM04")
@Alias("de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections.CauchyRandomProjectionFamily")
public class CauchyRandomProjectionFamily extends AbstractRandomProjectionFamily {
  /**
   * Constructor.
   *
   * @param random Random number generator.
   */
  public CauchyRandomProjectionFamily(RandomFactory random) {
    super(random);
  }

  @Override
  public Projection generateProjection(int idim, int odim) {
    double[][] matrix = new double[odim][idim];
    for(int i = 0; i < odim; ++i) {
      double[] row = matrix[i];
      for(int j = 0; j < idim; ++j) {
        row[j] = FastMath.tan(Math.PI * (random.nextDouble() - .5));
      }
    }
    return new MatrixProjection(matrix);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractRandomProjectionFamily.Parameterizer {
    @Override
    protected CauchyRandomProjectionFamily makeInstance() {
      return new CauchyRandomProjectionFamily(random);
    }
  }
}
