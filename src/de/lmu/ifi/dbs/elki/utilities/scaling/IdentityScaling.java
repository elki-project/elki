package de.lmu.ifi.dbs.elki.utilities.scaling;

/**
 * The trivial "identity" scaling function.
 * 
 * @author Erich Schubert
 */
public class IdentityScaling implements StaticScalingFunction {
  @Override
  public double getScaled(double value) {
    return value;
  }
  
  @Override
  public double getMin() {
    return Double.NEGATIVE_INFINITY;
  }
  
  @Override
  public double getMax() {
    return Double.POSITIVE_INFINITY;
  }
}
