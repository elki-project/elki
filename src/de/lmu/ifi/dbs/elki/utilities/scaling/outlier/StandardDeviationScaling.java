package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.ErrorFunctions;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Scaling that can map arbitrary values to a probability in the range of [0:1].
 * 
 * Transformation is done using the formula max(0, erf(lambda * (x - mean) /
 * (stddev * sqrt(2))))
 * 
 * Where mean can be fixed to a given value, and stddev is then computed against
 * this mean.
 * 
 * @author Erich Schubert
 * 
 */
public class StandardDeviationScaling extends AbstractParameterizable implements OutlierScalingFunction {
  /**
   * OptionID for {@link #MEAN_PARAM}
   */
  public static final OptionID MEAN_ID = OptionID.getOrCreateOptionID("stddevscale.mean", "Fixed mean to use in standard deviation scaling.");

  /**
   * Parameter to specify a fixed mean to use.
   * <p>
   * Key: {@code -stddevscale.mean}
   * </p>
   */
  private final DoubleParameter MEAN_PARAM = new DoubleParameter(MEAN_ID, true);

  /**
   * OptionID for {@link #LAMBDA_PARAM}
   */
  public static final OptionID LAMBDA_ID = OptionID.getOrCreateOptionID("stddevscale.lambda", "Significance level to use for error function.");

  /**
   * Parameter to specify the lambda value
   * <p>
   * Key: {@code -stddevscale.lambda}
   * </p>
   */
  private final DoubleParameter LAMBDA_PARAM = new DoubleParameter(LAMBDA_ID, 3.0);

  /**
   * Field storing the fixed mean to use
   */
  protected Double fixedmean = null;

  /**
   * Field storing the lambda value
   */
  protected Double lambda = null;

  /**
   * Mean to use
   */
  double mean;

  /**
   * Scaling factor to use (usually: Lambda * Stddev * Sqrt(2))
   */
  double factor;

  /**
   * Constructor.
   */
  public StandardDeviationScaling(Parameterization config) {
    super();
    if(config.grab(this, MEAN_PARAM)) {
      fixedmean = MEAN_PARAM.getValue();
    }
    if(config.grab(this, LAMBDA_PARAM)) {
      lambda = LAMBDA_PARAM.getValue();
    }
  }

  @Override
  public double getScaled(double value) {
    if(value <= mean) {
      return 0;
    }
    return Math.max(0, ErrorFunctions.erf((value - mean) / factor));
  }

  @Override
  public void prepare(Database<?> db, @SuppressWarnings("unused") Result result, OutlierResult or) {
    if(fixedmean == null) {
      MeanVariance mv = new MeanVariance();
      for(Integer id : db) {
        double val = or.getScores().getValueFor(id);
        mv.put(val);
      }
      mean = mv.getMean();
      factor = lambda * mv.getStddev() * Math.sqrt(2);
    }
    else {
      mean = fixedmean;
      double sqsum = 0;
      int cnt = 0;
      for(Integer id : db) {
        double val = or.getScores().getValueFor(id);
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