package experimentalcode.erich.data.images;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;

public class ComputeRGBColorHistogram extends AbstractComputeColorHistogram {
  int quant;

  /**
   * OptionID for {@link #BINSPERPLANE_PARAM}
   */
  public static final OptionID BINSPERPLANE_ID = OptionID.getOrCreateOptionID("rgbhist.bpp", "Bins per plane for RGB histogram. This will result in bpp ** 3 bins.");

  /**
   * Parameter that specifies the number of bins (per plane) to use.
   * 
   * <p>
   * Key: {@code -rgbhist.bpp}
   * </p>
   */
  private final IntParameter BINSPERPLANEPARAM = new IntParameter(BINSPERPLANE_ID, new IntervalConstraint(2, IntervalBoundary.CLOSE, 256, IntervalBoundary.CLOSE));  
  
  public ComputeRGBColorHistogram() {
    super();
    addOption(BINSPERPLANEPARAM);
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);
    
    quant = BINSPERPLANEPARAM.getValue();
    
    return remainingParameters;
  }



  @Override
  protected int getBinForColor(int rgb) {
    int r = (rgb & 0xFF0000) >> 16;
    int g = (rgb & 0x00FF00) >> 8;
    int b = (rgb & 0x0000FF);
    r = (int)Math.floor(quant * r / 256.);
    g = (int)Math.floor(quant * g / 256.);
    b = (int)Math.floor(quant * b / 256.);
    return r * quant * quant + g * quant + b;
  }

  @Override
  protected int getNumBins() {
    return quant * quant * quant;
  }
}
