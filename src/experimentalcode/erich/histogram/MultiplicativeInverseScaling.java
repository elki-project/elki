package experimentalcode.erich.histogram;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.Result;

public class MultiplicativeInverseScaling implements OutlierScalingFunction {
  double scaleval;

  @Override
  public double getScaled(double value) {
    try {
      return 1.0 / (value * scaleval);
    }
    catch(ArithmeticException e) {
      return 0;
    }
  }

  @Override
  public void prepare(Database<?> db, @SuppressWarnings("unused") Result result, AnnotationResult<Double> ann) {
    scaleval = getScaleValue(db, ann);
  }

  private static double getScaleValue(Database<?> db, AnnotationResult<Double> ann) {
    double max = Double.MIN_VALUE;
    for(Integer id : db) {
      double val = ann.getValueFor(id);
      double inv = Math.abs(1.0 / val);
      if(!Double.isInfinite(inv) && !Double.isNaN(inv)) {
        max = Math.max(max, inv);
      }
    }
    LoggingUtil.warning(""+max);
    return max;
  }
}