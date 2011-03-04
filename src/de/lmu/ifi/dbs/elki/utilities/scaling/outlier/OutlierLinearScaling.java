package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.MinMax;
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
public class OutlierLinearScaling implements OutlierScalingFunction {
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
   * Parameter to specify the maximum value
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
   * Flag to use the mean as minimum for scaling.
   * 
   * <p>
   * Key: {@code -linearscale.usemean}
   * </p>
   */
  private final Flag MEAN_FLAG = new Flag(MEAN_ID);

  /**
   * OptionID for {@link #NOZEROS_FLAG}
   */
  public static final OptionID NOZEROS_ID = OptionID.getOrCreateOptionID("linearscale.ignorezero", "Ignore zero entries when computing the minimum and maximum.");

  /**
   * Flag to use ignore zeros when computing the min and max.
   * 
   * <p>
   * Key: {@code -linearscale.ignorezero}
   * </p>
   */
  private final Flag NOZEROS_FLAG = new Flag(NOZEROS_ID);

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
   * Ignore zero values
   */
  boolean nozeros = false;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public OutlierLinearScaling(Parameterization config) {
    super();
    config = config.descend(this);
    if(config.grab(MIN_PARAM)) {
      min = MIN_PARAM.getValue();
    }
    if(config.grab(MAX_PARAM)) {
      max = MAX_PARAM.getValue();
    }
    if(config.grab(MEAN_FLAG)) {
      usemean = MEAN_FLAG.getValue();
    }
    if(config.grab(NOZEROS_FLAG)) {
      nozeros = NOZEROS_FLAG.getValue();
    }

    // Use-Mean and Minimum value must not be set at the same time!
    ArrayList<Parameter<?, ?>> minmean = new ArrayList<Parameter<?, ?>>();
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
  public void prepare(DBIDs ids, OutlierResult or) {
    if(usemean) {
      MeanVariance mv = new MeanVariance();
      MinMax<Double> mm = (max == null) ? new MinMax<Double>() : null;
      boolean skippedzeros = false;
      for(DBID id : ids) {
        double val = or.getScores().getValueFor(id);
        if(nozeros && val == 0.0) {
          skippedzeros = true;
          continue;
        }
        mv.put(val);
        if(max == null) {
          mm.put(val);
        }
      }
      if(skippedzeros && mm.getMin() == mm.getMax()) {
        mm.put(0.0);
        mv.put(0.0);
      }
      min = mv.getMean();
      if(max == null) {
        max = mm.getMax();
      }
    }
    else {
      if(min == null || max == null) {
        boolean skippedzeros = false;
        MinMax<Double> mm = new MinMax<Double>();
        for(DBID id : ids) {
          double val = or.getScores().getValueFor(id);
          if(nozeros && val == 0.0) {
            skippedzeros = true;
            continue;
          }
          mm.put(val);
        }
        if(skippedzeros && mm.getMin() == mm.getMax()) {
          mm.put(0.0);
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