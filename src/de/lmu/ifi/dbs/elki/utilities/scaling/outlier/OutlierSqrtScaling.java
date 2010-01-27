package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Scaling that can map arbitrary positive values to a value in the range of
 * [0:1].
 * 
 * Transformation is done by taking the square root, then doing a linear linear
 * mapping onto 0:1 using the minimum values seen.
 * 
 * @author Erich Schubert
 * 
 */
public class OutlierSqrtScaling extends AbstractParameterizable implements OutlierScalingFunction {
  /**
   * OptionID for {@link #MIN_PARAM}
   */
  public static final OptionID MIN_ID = OptionID.getOrCreateOptionID("sqrtscale.min", "Fixed minimum to use in sqrt scaling.");

  /**
   * Parameter to specify the fixed minimum to use.
   * <p>
   * Key: {@code -sqrtscale.min}
   * </p>
   */
  private final DoubleParameter MIN_PARAM = new DoubleParameter(MIN_ID, true);

  /**
   * OptionID for {@link #MAX_PARAM}
   */
  public static final OptionID MAX_ID = OptionID.getOrCreateOptionID("sqrtscale.max", "Fixed maximum to use in sqrt scaling.");

  /**
   * Parameter to specify the fixed maximum to use.
   * <p>
   * Key: {@code -sqrtscale.max}
   * </p>
   */
  private final DoubleParameter MAX_PARAM = new DoubleParameter(MAX_ID, true);

  /**
   * Field storing the minimum value
   */
  protected Double min = null;

  /**
   * Field storing the Maximum value
   */
  protected Double max = null;

  /**
   * Scaling factor
   */
  protected double factor;

  /**
   * Constructor.
   */
  public OutlierSqrtScaling() {
    super();
    addOption(MIN_PARAM);
    addOption(MAX_PARAM);
  }

  @Override
  public double getScaled(double value) {
    if(value <= min) {
      return 0;
    }
    return Math.min(1, (Math.sqrt(value - min) / factor));
  }

  @Override
  public void prepare(Database<?> db, @SuppressWarnings("unused") Result result, AnnotationResult<Double> ann) {
    if(min == null || max == null) {
      MinMax<Double> mm = new MinMax<Double>();
      for(Integer id : db) {
        double val = ann.getValueFor(id);
        mm.put(val);
      }
      if(min == null) {
        min = mm.getMin();
      }
      if(max == null) {
        max = mm.getMax();
      }
    }
    factor = Math.sqrt(max - min);
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

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  @Override
  public double getMin() {
    return 0.0;
  }

  @Override
  public double getMax() {
    return 1.0;
  }
}