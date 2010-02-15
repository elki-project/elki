package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.OnlyOneIsAllowedToBeSetGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Scaling that can map arbitrary values to a probability in the range of [0:1].
 * 
 * Transformation is done by linear mapping onto 0:1 using the minimum and
 * maximum values.
 * 
 * @author Erich Schubert
 * 
 */
public class OutlierLinearScaling extends AbstractLoggable implements OutlierScalingFunction {
  /**
   * OptionID for {@link #MIN_PARAM}
   */
  public static final OptionID MIN_ID = OptionID.getOrCreateOptionID("linearscale.min", "Fixed minimum to use in lienar scaling.");

  /**
   * Parameter to specify a fixed minimum to use.
   * <p>
   * Key: {@code -linearscale.min}
   * </p>
   */
  private final DoubleParameter MIN_PARAM = new DoubleParameter(MIN_ID, true);

  /**
   * OptionID for {@link #MAX_PARAM}
   */
  public static final OptionID MAX_ID = OptionID.getOrCreateOptionID("linearscale.max", "Fixed maximum to use in linear scaling.");

  /**
   * Parameter to specify the lambda value
   * <p>
   * Key: {@code -linearscale.max}
   * </p>
   */
  private final DoubleParameter MAX_PARAM = new DoubleParameter(MAX_ID, true);

  /**
   * OptionID for {@link #MEAN_FLAG}
   */
  public static final OptionID MEAN_ID = OptionID.getOrCreateOptionID("linearscale.usemean", "Use the mean as minimum for scaling.");

  /**
   * Parameter to specify the lambda value
   * <p>
   * Key: {@code -linearscale.max}
   * </p>
   */
  private final Flag MEAN_FLAG = new Flag(MEAN_ID);

  /**
   * Field storing the Minimum to use
   */
  protected Double min = null;

  /**
   * Field storing the Maximum value
   */
  protected Double max = null;

  /**
   * Scaling factor to use (1/ max - min)
   */
  double factor;

  /**
   * Use the mean for scaling
   */
  boolean usemean = false;

  /**
   * Constructor.
   */
  public OutlierLinearScaling(Parameterization config) {
    super();
    if(config.grab(this, MIN_PARAM)) {
      min = MIN_PARAM.getValue();
    }
    if(config.grab(this, MAX_PARAM)) {
      max = MAX_PARAM.getValue();
    }
    if(config.grab(this, MEAN_FLAG)) {
      usemean = MEAN_FLAG.getValue();
    }

    // Use-Mean and Minimum value must not be set at the same time!
    ArrayList<Parameter<?,?>> minmean = new ArrayList<Parameter<?,?>>();
    minmean.add(MIN_PARAM);
    minmean.add(MEAN_FLAG);
    GlobalParameterConstraint gpc = new OnlyOneIsAllowedToBeSetGlobalConstraint(minmean);
    config.checkConstraint(gpc);
  }

  @Override
  public double getScaled(double value) {
    if(value <= min) {
      return 0;
    }
    return Math.min(1, ((value - min) / factor));
  }

  @Override
  public void prepare(Database<?> db, @SuppressWarnings("unused") Result result, OutlierResult or) {
    if(usemean) {
      MeanVariance mv = new MeanVariance();
      MinMax<Double> mm = (max == null) ? new MinMax<Double>() : null;
      for(Integer id : db) {
        double val = or.getScores().getValueFor(id);
        mv.put(val);
        if(max == null) {
          mm.put(val);
        }
      }
      min = mv.getMean();
      if(max == null) {
        max = mm.getMax();
      }
    }
    else {
      if(min == null || max == null) {
        MinMax<Double> mm = new MinMax<Double>();
        for(Integer id : db) {
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
    }
    factor = (max - min);
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