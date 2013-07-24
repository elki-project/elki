package de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Random projections using Cauchy distributions (1-stable).
 * 
 * Reference:
 * <p>
 * M. Datar and N. Immorlica and P. Indyk and V. S. Mirrokni<br />
 * Locality-sensitive hashing scheme based on p-stable distributions.<br />
 * In Proc. 20th annual symposium on Computational geometry
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(title = "Locality-sensitive hashing scheme based on p-stable distributions", authors = "M. Datar and N. Immorlica and P. Indyk and V. S. Mirrokni", booktitle = "Proceedings of the 20th annual symposium on Computational geometry", url = "http://dx.doi.org/10.1145/997817.997857")
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
    Matrix projectionMatrix = new Matrix(odim, idim);
    for (int i = 0; i < odim; ++i) {
      for (int j = 0; j < idim; ++j) {
        final double value = random.nextGaussian();
        projectionMatrix.set(i, j, value);
      }
    }
    return new MatrixProjection(projectionMatrix);
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
    protected GaussianRandomProjectionFamily makeInstance() {
      return new GaussianRandomProjectionFamily(random);
    }
  }
}
