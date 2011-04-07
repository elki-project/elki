package de.lmu.ifi.dbs.elki.data.images;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Compute a (rather naive) RGB color histogram.
 * 
 * @author Erich Schubert
 */
public class ComputeNaiveRGBColorHistogram extends AbstractComputeColorHistogram {
  /**
   * Parameter that specifies the number of bins (per plane) to use.
   * 
   * <p>
   * Key: {@code -rgbhist.bpp}
   * </p>
   */
  public static final OptionID BINSPERPLANE_ID = OptionID.getOrCreateOptionID("rgbhist.bpp", "Bins per plane for RGB histogram. This will result in bpp ** 3 bins.");

  /**
   * Number of bins in each dimension to use.
   */
  int quant;

  /**
   * Constructor.
   * 
   * @param quant Number of bins to use.
   */
  public ComputeNaiveRGBColorHistogram(int quant) {
    super();
    this.quant = quant;
  }

  @Override
  protected int getBinForColor(int rgb) {
    int r = (rgb & 0xFF0000) >> 16;
    int g = (rgb & 0x00FF00) >> 8;
    int b = (rgb & 0x0000FF);
    r = (int) Math.floor(quant * r / 256.);
    g = (int) Math.floor(quant * g / 256.);
    b = (int) Math.floor(quant * b / 256.);
    return r * quant * quant + g * quant + b;
  }

  @Override
  protected int getNumBins() {
    return quant * quant * quant;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    protected int quant = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter param = new IntParameter(BINSPERPLANE_ID);
      if(config.grab(param)) {
        quant = param.getValue();
      }
    }

    @Override
    protected ComputeNaiveRGBColorHistogram makeInstance() {
      return new ComputeNaiveRGBColorHistogram(quant);
    }
  }
}