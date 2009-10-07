package experimentalcode.erich.histogram;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.Result;

/**
 * Scaling function to invert values by computing 1/(1+x)
 * 
 * @author Erich Schubert
 */
public class ABODSimpleScaling implements OutlierScalingFunction {
  @Override
  public double getScaled(double value) {
    return 1.0 / (1.0 + value);
  }

  @Override
  public void prepare(@SuppressWarnings("unused") Database<?> db, @SuppressWarnings("unused") Result result, @SuppressWarnings("unused") AnnotationResult<Double> ann) {
    // Nothing to do.
  }
}