package de.lmu.ifi.dbs.elki.utilities.scaling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
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
   * Parameter to specify a fixed minimum to use.
   * <p>
   * Key: {@code -clipscale.min}
   * </p>
   */
  public static final OptionID MIN_ID = OptionID.getOrCreateOptionID("clipscale.min", "Minimum value to allow.");

  /**
   * Parameter to specify the maximum value
   * <p>
   * Key: {@code -clipscale.max}
   * </p>
   */
  public static final OptionID MAX_ID = OptionID.getOrCreateOptionID("clipscale.max", "Maximum value to allow.");

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
   *
   * @param min Minimum, may be null
   * @param max Maximum, may be null
   */
  public ClipScaling(Double min, Double max) {
    super();
    this.min = min;
    this.max = max;
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
  
  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    protected Double min = null;
    protected Double max = null;
    
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter minP = new DoubleParameter(MIN_ID, true);
      if(config.grab(minP)) {
        min = minP.getValue();
      }
      DoubleParameter maxP = new DoubleParameter(MAX_ID, true);
      if (config.grab(maxP)) {
        max = maxP.getValue();
      }
    }

    @Override
    protected ClipScaling makeInstance() {
      return new ClipScaling(min, max);
    }
  }
}