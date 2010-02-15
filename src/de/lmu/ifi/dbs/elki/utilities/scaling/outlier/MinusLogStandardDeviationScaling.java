package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.ErrorFunctions;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

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
public class MinusLogStandardDeviationScaling extends StandardDeviationScaling implements OutlierScalingFunction {
  /**
   * Constructor.
   */
  public MinusLogStandardDeviationScaling(Parameterization config) {
    super(config);
  }

  @Override
  public double getScaled(double value) {
    final double mlogv = -Math.log(value);
    if (mlogv < mean || Double.isNaN(mlogv)) {
      return 0.0;
    }
    return Math.max(0.0, ErrorFunctions.erf((mlogv - mean) / factor));
  }

  @Override
  public void prepare(Database<?> db, @SuppressWarnings("unused") Result result, OutlierResult or) {
    if(fixedmean == null) {
      MeanVariance mv = new MeanVariance();
      for(Integer id : db) {
        double val = -Math.log(or.getScores().getValueFor(id));
        if(!Double.isNaN(val) && !Double.isInfinite(val)) {
          mv.put(val);
        }
      }
      mean = mv.getMean();
      factor = lambda * mv.getStddev() * Math.sqrt(2);
    }
    else {
      mean = fixedmean;
      double sqsum = 0;
      int cnt = 0;
      for(Integer id : db) {
        double val = -Math.log(or.getScores().getValueFor(id));
        if(!Double.isNaN(val) && !Double.isInfinite(val)) {
          sqsum += (val - mean) * (val - mean);
          cnt += 1;
        }
      }
      factor = lambda * Math.sqrt(sqsum / cnt) * Math.sqrt(2);
    }
  }
}