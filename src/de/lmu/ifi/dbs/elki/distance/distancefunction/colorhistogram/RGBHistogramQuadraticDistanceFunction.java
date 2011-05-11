package de.lmu.ifi.dbs.elki.distance.distancefunction.colorhistogram;

import de.lmu.ifi.dbs.elki.distance.distancefunction.WeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Distance function for RGB color histograms based on a quadratic form and
 * color similarity.
 * 
 * This is (unverified) attributed to
 * <p>
 * James Hafner, Harpreet S.Sawhney, Will Equits, Myron Flickner and Wayne
 * Niblack<br />
 * Efficient Color Histogram Indexing for Quadratic Form Distance Functions<br />
 * IEEE Trans. on Pattern Analysis and Machine Intelligence, Vol. 17, No. 7,
 * July 1995
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "J. Hafner, H. S.Sawhney, W. Equits, M. Flickner, W. Niblack", title = "Efficient Color Histogram Indexing for Quadratic Form Distance Functions", booktitle = "IEEE Trans. on Pattern Analysis and Machine Intelligence, Vol. 17, No. 7, July 1995", url = "http://dx.doi.org/10.1109/34.391417")
public class RGBHistogramQuadraticDistanceFunction extends WeightedDistanceFunction {
  /**
   * Parameter for the kernel dimensionality.
   */
  public static final OptionID BPP_ID = OptionID.getOrCreateOptionID("rgbhist.bpp", "The dimensionality of the histogram in each color");

  /**
   * Constructor.
   * 
   * @param bpp bins per plane.
   */
  public RGBHistogramQuadraticDistanceFunction(int bpp) {
    super(computeWeightMatrix(bpp));
  }

  /**
   * Compute weight matrix for a RGB color histogram
   * 
   * @param bpp bins per plane
   * @return Weight matrix
   */
  public static Matrix computeWeightMatrix(int bpp) {
    final int dim = bpp * bpp * bpp;

    final Matrix m = new Matrix(dim, dim);
    // maximum occurring distance in manhattan between bins:
    final double max = 3. * (bpp - 1.);
    for(int x = 0; x < dim; x++) {
      final int rx = (x / bpp) / bpp;
      final int gx = (x / bpp) % bpp;
      final int bx = x % bpp;
      for(int y = 0; y < dim; y++) {
        final int ry = (y / bpp) / bpp;
        final int gy = (y / bpp) % bpp;
        final int by = y % bpp;

        final double dr = Math.abs(rx - ry);
        final double dg = Math.abs(gx - gy);
        final double db = Math.abs(bx - by);

        final double val = 1 - (dr + dg + db) / max;
        m.set(x, y, val);
      }
    }
    return m;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    int bpp = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter param = new IntParameter(BPP_ID);
      if(config.grab(param)) {
        bpp = param.getValue();
      }
    }

    @Override
    protected RGBHistogramQuadraticDistanceFunction makeInstance() {
      return new RGBHistogramQuadraticDistanceFunction(bpp);
    }
  }
}