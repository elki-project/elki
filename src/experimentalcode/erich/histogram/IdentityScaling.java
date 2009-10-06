package experimentalcode.erich.histogram;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.Result;

public class IdentityScaling implements OutlierScalingFunction {
  @Override
  public double getScaled(double value) {
    return value;
  }

  @Override
  public void prepare(@SuppressWarnings("unused") Database<?> db, @SuppressWarnings("unused") Result result, @SuppressWarnings("unused") AnnotationResult<Double> ann) {
    // Nothing to do here.
  }
}
