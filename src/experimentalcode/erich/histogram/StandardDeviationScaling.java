package experimentalcode.erich.histogram;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;

public class StandardDeviationScaling extends AbstractParameterizable implements OutlierScalingFunction {
  double mean;
  double stddev;

  @Override
  public double getScaled(double value) {
    return (value - mean) / stddev;
  }

  @Override
  public void prepare(Database<?> db, @SuppressWarnings("unused") Result result, AnnotationResult<Double> ann) {
    MeanVariance mv = new MeanVariance();
    for(Integer id : db) {
      double val = ann.getValueFor(id);
      mv.put(val);
    }
    mean = mv.getMean();
    stddev = mv.getStddev();
  }
}