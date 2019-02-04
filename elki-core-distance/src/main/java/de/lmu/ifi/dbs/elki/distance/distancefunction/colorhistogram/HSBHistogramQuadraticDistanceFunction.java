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
package de.lmu.ifi.dbs.elki.distance.distancefunction.colorhistogram;

import de.lmu.ifi.dbs.elki.distance.distancefunction.MatrixWeightedQuadraticDistanceFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListSizeConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;

import net.jafama.DoubleWrapper;
import net.jafama.FastMath;

/**
 * Distance function for HSB color histograms based on a quadratic form and
 * color similarity.
 * <p>
 * The matrix is filled according to:
 * <p>
 * VisualSEEk: a fully automated content-based image query system<br>
 * J. R. Smith, S. F. Chang<br>
 * Proc. 4th ACM Int. Conf. on Multimedia 1997
 *
 * @author Erich Schubert
 * @since 0.3
 */
@Reference(authors = "J. R. Smith, S. F. Chang", //
    title = "VisualSEEk: a fully automated content-based image query system", //
    booktitle = "Proc. 4th ACM Int. Conf. on Multimedia 1997", //
    url = "https://doi.org/10.1145/244130.244151", //
    bibkey = "DBLP:conf/mm/SmithC96")
public class HSBHistogramQuadraticDistanceFunction extends MatrixWeightedQuadraticDistanceFunction {
  /**
   * Constructor.
   *
   * @param quanth Hue bins
   * @param quants Saturation bins
   * @param quantb Brightness bins
   */
  public HSBHistogramQuadraticDistanceFunction(int quanth, int quants, int quantb) {
    super(computeWeightMatrix(quanth, quants, quantb));
  }

  /**
   * Compute the weight matrix for HSB similarity.
   *
   * @param quanth H bins
   * @param quants S bins
   * @param quantb B bins
   * @return Weight matrix
   */
  public static double[][] computeWeightMatrix(final int quanth, final int quants, final int quantb) {
    final int dim = quanth * quants * quantb;
    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    assert (dim > 0);
    final double[][] m = new double[dim][dim];
    for(int x = 0; x < dim; x++) {
      final int hx = x / (quantb * quants);
      final int sx = (x / quantb) % quants;
      final int bx = x % quantb;
      for(int y = x; y < dim; y++) {
        final int hy = y / (quantb * quants);
        final int sy = (y / quantb) % quants;
        final int by = y % quantb;

        final double shx = FastMath.sinAndCos((hx + .5) / quanth * MathUtil.TWOPI, tmp);
        final double chx = tmp.value;
        final double shy = FastMath.sinAndCos((hy + .5) / quanth * MathUtil.TWOPI, tmp);
        final double chy = tmp.value;
        final double cos = chx * (sx + .5) / quants - chy * (sy + .5) / quants;
        final double sin = shx * (sx + .5) / quants - shy * (sy + .5) / quants;
        final double db = (bx - by) / (double) quantb;
        final double val = 1. - FastMath.sqrt((db * db + sin * sin + cos * cos) / 5);
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
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for the kernel dimensionality.
     */
    public static final OptionID BPP_ID = new OptionID("hsbhist.bpp", "The dimensionality of the histogram in hue, saturation and brightness.");

    int quanth = 0;

    int quants = 0;

    int quantb = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntListParameter param = new IntListParameter(BPP_ID) //
          .addConstraint(new ListSizeConstraint(3)) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT_LIST);
      if(config.grab(param)) {
        int[] quant = param.getValue();
        assert (quant.length == 3);
        quanth = quant[0];
        quants = quant[1];
        quantb = quant[2];
      }
    }

    @Override
    protected HSBHistogramQuadraticDistanceFunction makeInstance() {
      return new HSBHistogramQuadraticDistanceFunction(quanth, quants, quantb);
    }
  }
}
