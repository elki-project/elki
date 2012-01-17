package de.lmu.ifi.dbs.elki.distance.distancefunction.colorhistogram;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.distance.distancefunction.WeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListGreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListSizeConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;

/**
 * Distance function for HSB color histograms based on a quadratic form and
 * color similarity.
 * 
 * The matrix is filled according to:
 * <p>
 * VisualSEEk: a fully automated content-based image query system<br />
 * Smith, J.R. and Chang, S.F.<br />
 * Proceedings of the fourth ACM international conference on Multimedia 1997
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "J. R. Smith, S. F. Chang", title = "VisualSEEk: a fully automated content-based image query system", booktitle = "Proceedings of the fourth ACM international conference on Multimedia 1997", url = "http://dx.doi.org/10.1145/244130.244151")
public class HSBHistogramQuadraticDistanceFunction extends WeightedDistanceFunction {
  /**
   * Parameter for the kernel dimensionality.
   */
  public static final OptionID BPP_ID = OptionID.getOrCreateOptionID("hsbhist.bpp", "The dimensionality of the histogram in hue, saturation and brightness.");

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
  public static Matrix computeWeightMatrix(final int quanth, final int quants, final int quantb) {
    final int dim = quanth * quants * quantb;
    assert (dim > 0);
    final Matrix m = new Matrix(dim, dim);
    for(int x = 0; x < dim; x++) {
      final int hx = x / (quantb * quants);
      final int sx = (x / quantb) % quants;
      final int bx = x % quantb;
      for(int y = 0; y < dim; y++) {
        final int hy = y / (quantb * quants);
        final int sy = (y / quantb) % quants;
        final int by = y % quantb;

        final double cos = Math.cos((hx + .5) / quanth * MathUtil.TWOPI) * (sx + .5) / quants - Math.cos((hy + .5) / quanth * MathUtil.TWOPI) * (sy + .5) / quants;
        final double sin = Math.sin((hx + .5) / quanth * MathUtil.TWOPI) * (sx + .5) / quants - Math.sin((hy + .5) / quanth * MathUtil.TWOPI) * (sy + .5) / quants;
        final double db = (bx - by) / (double) quantb;
        final double val = 1. - Math.sqrt((db * db + sin * sin + cos * cos) / 5);
        m.set(x, y, val);
      }
    }
    return m;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if (!this.getClass().equals(obj.getClass())) {
      return false;
    }
    return this.weightMatrix.equals(((HSBHistogramQuadraticDistanceFunction)obj).weightMatrix);
  }
  
  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    int quanth = 0;

    int quants = 0;

    int quantb = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final List<ParameterConstraint<List<Integer>>> bppConstraints = new ArrayList<ParameterConstraint<List<Integer>>>(2);
      bppConstraints.add(new ListSizeConstraint<Integer>(3));
      bppConstraints.add(new ListGreaterEqualConstraint<Integer>(1));
      IntListParameter param = new IntListParameter(BPP_ID, bppConstraints, false);
      if(config.grab(param)) {
        List<Integer> quant = param.getValue();
        assert (quant.size() == 3);
        quanth = quant.get(0);
        quants = quant.get(1);
        quantb = quant.get(2);
      }
    }

    @Override
    protected HSBHistogramQuadraticDistanceFunction makeInstance() {
      return new HSBHistogramQuadraticDistanceFunction(quanth, quants, quantb);
    }
  }
}