package de.lmu.ifi.dbs.elki.utilities.scaling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Scale implementing a simple clipping. Values less than the specified minimum
 * will be set to the minimum, values larger than the maximum will be set to the
 * maximum.
 * 
 * @author Erich Schubert
 */
public class ClipScaling implements StaticScalingFunction {
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
  public ClipScaling(Parameterization config) {
    super();
    if(config.grab(MIN_PARAM)) {
      min = MIN_PARAM.getValue();
    }
    if (config.grab(MAX_PARAM)) {
      max = MAX_PARAM.getValue();
    }
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
  public double getMin() {
    return (min != null) ? min : Double.NEGATIVE_INFINITY;
  }

  @Override
  public double getMax() {
    return (max != null) ? max : Double.POSITIVE_INFINITY;
  }
}