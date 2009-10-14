package experimentalcode.shared.outlier.scaling;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Scale implementing a simple clipping. Values less than the specified minimum
 * will be set to the minimum, values larger than the maximum will be set to the
 * maximum.
 * 
 * @author Erich Schubert
 */
public class ClipScaling extends AbstractParameterizable implements StaticScalingFunction {
  /**
   * OptionID for {@link #MIN_PARAM}
   */
  public static final OptionID MIN_ID = OptionID.getOrCreateOptionID("clipscale.min", "Minimum value to allow.");

  /**
   * Parameter to specify a fixed minimum to use.
   * <p>
   * Key: {@code -clipscale.min}
   * </p>
   */
  private final DoubleParameter MIN_PARAM = new DoubleParameter(MIN_ID, true);

  /**
   * OptionID for {@link #MAX_PARAM}
   */
  public static final OptionID MAX_ID = OptionID.getOrCreateOptionID("clipscale.max", "Maximum value to allow.");

  /**
   * Parameter to specify the maximum value
   * <p>
   * Key: {@code -clipscale.max}
   * </p>
   */
  private final DoubleParameter MAX_PARAM = new DoubleParameter(MAX_ID, true);

  /**
   * Field storing the minimum to use
   */
  private Double min = null;

  /**
   * Field storing the maximum to use
   */
  private Double max = null;

  /**
   * Constructor.
   */
  public ClipScaling() {
    super();
    addOption(MIN_PARAM);
    addOption(MAX_PARAM);
  }

  @Override
  public double getScaled(double value) {
    if(min != null && value < min) {
      return min;
    }
    if(max != null && value > max) {
      return max;
    }
    return value;
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    if(MIN_PARAM.isSet()) {
      min = MIN_PARAM.getValue();
    }
    if(MAX_PARAM.isSet()) {
      max = MAX_PARAM.getValue();
    }
    return remainingParameters;
  }
}