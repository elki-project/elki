package de.lmu.ifi.dbs.elki.distance.distancefunction.colorhistogram;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.WeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListGreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListSizeConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;

/**
 * Distance function for HSB color histograms based on a quadratic form and
 * color similarity.
 * 
 * The matrix is filled according to VisualSEEk: a fully automated content-based
 * image query system Smith, J.R. and Chang, S.F. Proceedings of the fourth ACM
 * international conference on Multimedia 1997
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class HSBHistogramQuadraticDistanceFunction<V extends NumberVector<V, ?>> extends WeightedDistanceFunction<V> {
  /**
   * OptionID for {@link #BPP_PARAM}
   */
  public static final OptionID BPP_ID = OptionID.getOrCreateOptionID("hsbhist.bpp", "The dimensionality of the histogram in hue, saturation and brightness.");

  /**
   * Parameter list constraints.
   */
  private static final List<ParameterConstraint<List<Integer>>> bppConstraints = new ArrayList<ParameterConstraint<List<Integer>>>(2);
  static {
    bppConstraints.add(new ListSizeConstraint<Integer>(3));
    bppConstraints.add(new ListGreaterEqualConstraint<Integer>(1));
  }

  /**
   * Parameter for the kernel dimensionality.
   */
  IntListParameter BPP_PARAM = new IntListParameter(BPP_ID, bppConstraints, false);

  /**
   * Stores the (full = multiplied) dimensionality
   */
  int dim;

  /**
   * Constructor, AbstractParameterizable style.
   */
  public HSBHistogramQuadraticDistanceFunction(Parameterization config) {
    super(null);
    if(config.grab(this, BPP_PARAM)) {
      List<Integer> quant = BPP_PARAM.getValue();
      if(quant.size() != 3) {
        config.reportError(new WrongParameterValueException(BPP_PARAM, "I need exactly three values for the bpp parameter."));
      }
      else {
        final int quanth = quant.get(0);
        final int quants = quant.get(1);
        final int quantb = quant.get(2);
        dim = quanth * quants * quantb;

        Matrix m = new Matrix(dim, dim);
        for(int x = 0; x < dim; x++) {
          final int hx = x / (quantb * quants);
          final int sx = (x / quantb) % quants;
          final int bx = x % quantb;
          for(int y = 0; y < dim; y++) {
            final int hy = y / (quantb * quants);
            final int sy = (y / quantb) % quants;
            final int by = y % quantb;

            final double cos = Math.cos((hx + .5) / quanth * 2 * Math.PI) * (sx + .5) / quants - Math.cos((hy + .5) / quanth * 2 * Math.PI) * (sy + .5) / quants;
            final double sin = Math.sin((hx + .5) / quanth * 2 * Math.PI) * (sx + .5) / quants - Math.sin((hy + .5) / quanth * 2 * Math.PI) * (sy + .5) / quants;
            final double db = (bx - by) / (double) quantb;
            final double val = 1. - Math.sqrt((db * db + sin * sin + cos * cos) / 5);
            m.set(x, y, val);
          }
        }
        weightMatrix = m;
        // logger.warning("Weight matrix:\n"+m.toString());
      }
    }
  }
}
