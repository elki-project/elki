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
}
