package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Scaling that can map arbitrary positive values to a value in the range of
 * [0:1].
 * 
 * Transformation is done by taking the square root, then doing a linear linear
 * mapping onto 0:1 using the minimum values seen.
 * 
 * @author Erich Schubert
 */
public class OutlierSqrtScaling implements OutlierScalingFunction {
  /**
   * Parameter to specify the fixed minimum to use.
   * <p>
   * Key: {@code -sqrtscale.min}
   * </p>
   */
  public static final OptionID MIN_ID = OptionID.getOrCreateOptionID("sqrtscale.min", "Fixed minimum to use in sqrt scaling.");

  /**
   * Parameter to specify the fixed maximum to use.
   * <p>
   * Key: {@code -sqrtscale.max}
   * </p>
   */
  public static final OptionID MAX_ID = OptionID.getOrCreateOptionID("sqrtscale.max", "Fixed maximum to use in sqrt scaling.");

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
   * 
   * @param min
   * @param max
   */
  public OutlierSqrtScaling(Double min, Double max) {
    super();
    this.min = min;
    this.max = max;
  }

  @Override
  public double getScaled(double value) {
    assert (factor != 0) : "prepare() was not run prior to using the scaling function.";
    if(value <= min) {
      return 0;
    }
    return Math.min(1, (Math.sqrt(value - min) / factor));
  }

  @Override
  public void prepare(DBIDs ids, OutlierResult or) {
    if(min == null || max == null) {
      DoubleMinMax mm = new DoubleMinMax();
      for(DBID id : ids) {
        double val = or.getScores().getValueFor(id);
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
  public double getMin() {
    return 0.0;
  }

  @Override
  public double getMax() {
    return 1.0;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    protected double min;

    protected double max;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter minP = new DoubleParameter(MIN_ID, true);
      if(config.grab(minP)) {
        min = minP.getValue();
      }
      DoubleParameter maxP = new DoubleParameter(MAX_ID, true);
      if(config.grab(maxP)) {
        max = maxP.getValue();
      }
    }

    @Override
    protected OutlierSqrtScaling makeInstance() {
      return new OutlierSqrtScaling(min, max);
    }
  }
}