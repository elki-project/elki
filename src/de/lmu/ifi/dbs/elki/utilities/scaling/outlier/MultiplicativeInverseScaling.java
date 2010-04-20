package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;

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
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public MultiplicativeInverseScaling() {
    super();
  }

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
  public void prepare(Database<?> db, OutlierResult or) {
    scaleval = getScaleValue(db, or);
  }

  /**
   * Compute the scaling value in a linear scan over the annotation.
   * 
   * @param db Database
   * @param or Outlier result
   * @return Scaling value.
   */
  private static double getScaleValue(Database<?> db, OutlierResult or) {
    double max = Double.MIN_VALUE;
    for(Integer id : db) {
      double val = or.getScores().getValueFor(id);
      double inv = Math.abs(1.0 / val);
      if(!Double.isInfinite(inv) && !Double.isNaN(inv)) {
        max = Math.max(max, inv);
      }
    }
    return max;
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