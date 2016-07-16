package de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

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
url = "http://dx.doi.org/10.1145/997817.997857")
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
        row[j] = Math.tan(Math.PI * (random.nextDouble() - .5));
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
