package experimentalcode.erich.histogram;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.Result;

public interface OutlierScalingFunction {
  public void prepare(Database<?> db, Result result, AnnotationResult<Double> ann);
  public double getScaled(double value);
}
