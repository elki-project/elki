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
package elki.distance.colorhistogram;

import elki.distance.MatrixWeightedQuadraticDistance;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Distance function for RGB color histograms based on a quadratic form and
 * color similarity.
 * <p>
 * Reference:
 * <p>
 * J. Hafner, H. S. Sawhney, W. Equits, M. Flickner, W. Niblack<br>
 * Efficient Color Histogram Indexing for Quadratic Form Distance Functions<br>
 * IEEE Trans. on Pattern Analysis and Machine Intelligence 17(7)
 *
 * @author Erich Schubert
 * @since 0.3
 */
@Reference(authors = "J. Hafner, H. S. Sawhney, W. Equits, M. Flickner, W. Niblack", //
    title = "Efficient Color Histogram Indexing for Quadratic Form Distance Functions", //
    booktitle = "IEEE Trans. on Pattern Analysis and Machine Intelligence 17(7)", //
    url = "https://doi.org/10.1109/34.391417", //
    bibkey = "DBLP:journals/pami/HafnerSEFN95")
public class RGBHistogramQuadraticDistance extends MatrixWeightedQuadraticDistance {
  /**
   * Constructor.
   * 
   * @param bpp bins per plane.
   */
  public RGBHistogramQuadraticDistance(int bpp) {
    super(computeWeightMatrix(bpp));
  }

  /**
   * Compute weight matrix for a RGB color histogram
   * 
   * @param bpp bins per plane
   * @return Weight matrix
   */
  public static double[][] computeWeightMatrix(int bpp) {
    final int dim = bpp * bpp * bpp;

    final double[][] m = new double[dim][dim];
    // maximum occurring distance in manhattan between bins:
    final double max = 3. * (bpp - 1.);
    for(int x = 0; x < dim; x++) {
      final int rx = (x / bpp) / bpp;
      final int gx = (x / bpp) % bpp;
      final int bx = x % bpp;
      for(int y = x; y < dim; y++) {
        final int ry = (y / bpp) / bpp;
        final int gy = (y / bpp) % bpp;
        final int by = y % bpp;

        final double dr = Math.abs(rx - ry);
        final double dg = Math.abs(gx - gy);
        final double db = Math.abs(bx - by);

        final double val = 1 - (dr + dg + db) / max;
        m[x][y] = m[y][x] = val;
      }
    }
    return m;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter for the kernel dimensionality.
     */
    public static final OptionID BPP_ID = new OptionID("rgbhist.bpp", "The dimensionality of the histogram in each color");

    /**
     * Bits per color plane.
     */
    int bpp = 0;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(BPP_ID) //
          .grab(config, x -> bpp = x);
    }

    @Override
    public RGBHistogramQuadraticDistance make() {
      return new RGBHistogramQuadraticDistance(bpp);
    }
  }
}
