package experimentalcode.erich.data.images;

import java.awt.Color;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;

public class ComputeHSBColorHistogram extends AbstractComputeColorHistogram {
  int quant;

  /**
   * OptionID for {@link #BINSPERPLANE_PARAM}
   */
  public static final OptionID BINSPERPLANE_ID = OptionID.getOrCreateOptionID("hsbhist.bpp", "Bins per plane for HSV/HSB histogram. This will result in bpp ** 3 bins.");

  /**
   * Parameter that specifies the number of bins (per plane) to use.
   * 
   * <p>
   * Key: {@code -rgbhist.bpp}
   * </p>
   */
  private final IntParameter BINSPERPLANE_PARAM = new IntParameter(BINSPERPLANE_ID, new IntervalConstraint(2, IntervalBoundary.CLOSE, 256, IntervalBoundary.CLOSE));

  public ComputeHSBColorHistogram() {
    super();
    addOption(BINSPERPLANE_PARAM);
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    quant = BINSPERPLANE_PARAM.getValue();

    return remainingParameters;
  }

  private final static float[] hsbbuf = new float[3];

  @Override
  protected int getBinForColor(int rgb) {
    int r = (rgb & 0xFF0000) >> 16;
    int g = (rgb & 0x00FF00) >> 8;
    int b = (rgb & 0x0000FF);

    synchronized(hsbbuf) {
      float[] hsbvals = Color.RGBtoHSB(r, g, b, hsbbuf);
      // The values returned by RGBtoHSB are all in [0:1]
      int h = (int) Math.floor(quant * hsbvals[0]);
      int s = (int) Math.floor(quant * hsbvals[1]);
      int v = (int) Math.floor(quant * hsbvals[2]);
      // Guard against the value of 1.0
      if(h >= quant) {
        h = quant - 1;
      }
      if(s >= quant) {
        s = quant - 1;
      }
      if(v >= quant) {
        v = quant - 1;
      }
      return h * quant * quant + s * quant + v;
    }
  }

  @Override
  protected int getNumBins() {
    return quant * quant * quant;
  }
}
