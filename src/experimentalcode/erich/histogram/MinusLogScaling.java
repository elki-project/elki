package experimentalcode.erich.histogram;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.Result;

/**
 * Scaling function to invert values by computing -1 * Math.log(x)
 * 
 * @author Erich Schubert
 */
public class MinusLogScaling implements OutlierScalingFunction {
  @Override
  public double getScaled(double value) {
    return - Math.log(value);
  }

  @Override
  public void prepare(@SuppressWarnings("unused") Database<?> db, @SuppressWarnings("unused") Result result, @SuppressWarnings("unused") AnnotationResult<Double> ann) {
    // Nothing to do.
  }
}