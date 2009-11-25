package de.lmu.ifi.dbs.elki.utilities.scaling;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.Result;

/**
 * Scaling function to invert values basically by computing 1/x, but in a variation
 * that maps the values to the [0:1] interval and avoiding division by 0.
 * 
 * The exact formula can be written as
 * 
 * 1 / (value * max_{x!=0}(1 / abs(x))) = min_{x != 0}(abs(x)) / x
 * 
 * with 1 / 0 := 1
 * 
 * @author Erich Schubert
 */
public class MultiplicativeInverseScaling implements OutlierScalingFunction {
  /**
   * Scaling value, set by {@link #prepare}.
   */
  double scaleval;

  @Override
  public double getScaled(double value) {
    try {
      return 1.0 / (value * scaleval);
    }
    catch(ArithmeticException e) {
      return 1.0;
    }
  }

  @Override
  public void prepare(Database<?> db, @SuppressWarnings("unused") Result result, AnnotationResult<Double> ann) {
    scaleval = getScaleValue(db, ann);
  }

  /**
   * Compute the scaling value in a linear scan over the annotation.
   * 
   * @param db Database
   * @param ann Annotation to use.
   * @return Scaling value.
   */
  private static double getScaleValue(Database<?> db, AnnotationResult<Double> ann) {
    double max = Double.MIN_VALUE;
    for(Integer id : db) {
      double val = ann.getValueFor(id);
      double inv = Math.abs(1.0 / val);
      if(!Double.isInfinite(inv) && !Double.isNaN(inv)) {
        max = Math.max(max, inv);
      }
    }
    return max;
  }
}