package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.math.ErrorFunctions;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Scaling that can map arbitrary values to a probability in the range of [0:1].
 * 
 * Transformation is done using the formulas
 * 
 * {@code y = sqrt(x - min)}
 * 
 * {@code newscore = max(0, erf(lambda * (y - mean) / (stddev * sqrt(2))))}
 * 
 * Where min and mean can be fixed to a given value, and stddev is then computed
 * against this mean.
 * 
 * @author Erich Schubert
 * 
 */
public class SqrtStandardDeviationScaling extends AbstractLoggable implements OutlierScalingFunction {
  /**
   * OptionID for {@link #MIN_PARAM}
   */
  public static final OptionID MIN_ID = OptionID.getOrCreateOptionID("sqrtstddevscale.min", "Fixed minimum to use in sqrt scaling.");

  /**
   * Parameter to specify the fixed minimum to use.
   * <p>
   * Key: {@code -sqrtstddevscale.min}
   * </p>
   */
  private final DoubleParameter MIN_PARAM = new DoubleParameter(MIN_ID, true);

  /**
   * OptionID for {@link #MEAN_PARAM}
   */
  public static final OptionID MEAN_ID = OptionID.getOrCreateOptionID("sqrtstddevscale.mean", "Fixed mean to use in standard deviation scaling.");

  /**
   * Parameter to specify a fixed mean to use.
   * <p>
   * Key: {@code -sqrtstddevscale.mean}
   * </p>
   */
  private final DoubleParameter MEAN_PARAM = new DoubleParameter(MEAN_ID, true);

  /**
   * OptionID for {@link #LAMBDA_PARAM}
   */
  public static final OptionID LAMBDA_ID = OptionID.getOrCreateOptionID("sqrtstddevscale.lambda", "Significance level to use for error function.");

  /**
   * Parameter to specify the lambda value
   * <p>
   * Key: {@code -sqrtstddevscale.lambda}
   * </p>
   */
  private final DoubleParameter LAMBDA_PARAM = new DoubleParameter(LAMBDA_ID, 3.0);

  /**
   * Field storing the lambda value
   */
  protected Double lambda = null;

  /**
   * Min to use
   */
  Double min = null;

  /**
   * Mean to use
   */
  Double mean = null;

  /**
   * Scaling factor to use (usually: Lambda * Stddev * Sqrt(2))
   */
  double factor;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public SqrtStandardDeviationScaling(Parameterization config) {
    super();
    if(config.grab(MIN_PARAM)) {
      min = MIN_PARAM.getValue();
    }
    if(config.grab(MEAN_PARAM)) {
      mean = MEAN_PARAM.getValue();
    }
    if(config.grab(LAMBDA_PARAM)) {
      lambda = LAMBDA_PARAM.getValue();
    }
  }

  @Override
  public double getScaled(double value) {
    if(value <= min) {
      return 0;
    }
    value = (value <= min) ? 0 : Math.sqrt(value - min);
    if(value <= mean) {
      return 0;
    }
    return Math.max(0, ErrorFunctions.erf((value - mean) / factor));
  }

  @Override
  public void prepare(Database<?> db, OutlierResult or) {
    if(min == null) {
      MinMax<Double> mm = new MinMax<Double>();
      for(Integer id : db) {
        double val = or.getScores().getValueFor(id);
        mm.put(val);
      }
      min = mm.getMin();
    }
    if(mean == null) {
      MeanVariance mv = new MeanVariance();
      for(Integer id : db) {
        double val = or.getScores().getValueFor(id);
        val = (val <= min) ? 0 : Math.sqrt(val - min);
        mv.put(val);
      }
      mean = mv.getMean();
      factor = lambda * mv.getStddev() * Math.sqrt(2);
    }
    else {
      double sqsum = 0;
      int cnt = 0;
      for(Integer id : db) {
        double val = or.getScores().getValueFor(id);
        val = (val <= min) ? 0 : Math.sqrt(val - min);
        sqsum += (val - mean) * (val - mean);
        cnt += 1;
      }
      factor = lambda * Math.sqrt(sqsum / cnt) * Math.sqrt(2);
    }
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