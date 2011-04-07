package de.lmu.ifi.dbs.elki.data.images;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Compute color histograms in a Hue-Saturation-Brightness model.
 * 
 * @author Erich Schubert
 */
public class ComputeNaiveHSBColorHistogram extends ComputeHSBColorHistogram {
  /**
   * Parameter that specifies the number of bins (per plane) to use.
   * 
   * <p>
   * Key: {@code -hsbhist.bpp}
   * </p>
   */
  public static final OptionID BINSPERPLANE_ID = OptionID.getOrCreateOptionID("hsbhist.bpp", "Bins per plane for HSV/HSB histogram. This will result in bpp ** 3 bins.");

  /**
   * Constructor.
   * 
   * @param quant Number of bins to use.
   */
  public ComputeNaiveHSBColorHistogram(int quant) {
    super(quant, quant, quant);
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
    protected ComputeNaiveHSBColorHistogram makeInstance() {
      return new ComputeNaiveHSBColorHistogram(quant);
    }
  }
}