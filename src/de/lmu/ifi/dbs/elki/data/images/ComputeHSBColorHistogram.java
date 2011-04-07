package de.lmu.ifi.dbs.elki.data.images;

import java.awt.Color;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListGreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListSizeConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;

/**
 * Compute color histograms in a Hue-Saturation-Brightness model.
 * 
 * @author Erich Schubert
 */
public class ComputeHSBColorHistogram extends AbstractComputeColorHistogram {
  /**
   * Parameter that specifies the number of bins (per plane) to use.
   * 
   * <p>
   * Key: {@code -rgbhist.bpp}
   * </p>
   */
  public static final OptionID BINSPERPLANE_ID = OptionID.getOrCreateOptionID("hsbhist.bpp", "Bins per plane for HSV/HSB histogram. This will result in bpp ** 3 bins.");

  /**
   * Number of bins in hue to use.
   */
  int quanth;

  /**
   * Number of bins in saturation to use.
   */
  int quants;

  /**
   * Number of bins in brightness to use.
   */
  int quantb;

  /**
   * Constructor.
   * 
   * @param quanth Hue bins
   * @param quants Saturation bins
   * @param quantb Brightness bins
   */
  public ComputeHSBColorHistogram(int quanth, int quants, int quantb) {
    super();
    this.quanth = quanth;
    this.quants = quants;
    this.quantb = quantb;
  }

  @Override
  protected int getBinForColor(int rgb) {
    int r = (rgb & 0xFF0000) >> 16;
    int g = (rgb & 0x00FF00) >> 8;
    int b = (rgb & 0x0000FF);

    float[] hsbvals = Color.RGBtoHSB(r, g, b, null);
    // The values returned by RGBtoHSB are all in [0:1]
    int h = (int) Math.floor(quanth * hsbvals[0]);
    int s = (int) Math.floor(quants * hsbvals[1]);
    int v = (int) Math.floor(quantb * hsbvals[2]);
    // Guard against the value of 1.0
    if(h >= quanth) {
      h = quanth - 1;
    }
    if(s >= quants) {
      s = quants - 1;
    }
    if(v >= quantb) {
      v = quantb - 1;
    }
    return h * quants * quantb + s * quantb + v;
  }

  @Override
  protected int getNumBins() {
    return quanth * quants * quantb;
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
      final IntListParameter param = new IntListParameter(BINSPERPLANE_ID, false);
      param.addConstraint(new ListSizeConstraint<Integer>(3));
      param.addConstraint(new ListGreaterEqualConstraint<Integer>(1));

      if(config.grab(param)) {
        List<Integer> quant = param.getValue();
        if(quant.size() != 3) {
          config.reportError(new WrongParameterValueException(param, "I need exactly three values for the bpp parameter."));
        }
        else {
          quanth = quant.get(0);
          quants = quant.get(1);
          quantb = quant.get(2);
        }
      }
    }

    @Override
    protected ComputeHSBColorHistogram makeInstance() {
      return new ComputeHSBColorHistogram(quanth, quants, quantb);
    }
  }
}