package de.lmu.ifi.dbs.elki.utilities.scaling;

/**
 * Scaling function to invert values by computing -1 * Math.log(x)
 * 
 * @author Erich Schubert
 */
public class MinusLogScaling implements StaticScalingFunction {
  @Override
  public double getScaled(double value) {
    return - Math.log(value);
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