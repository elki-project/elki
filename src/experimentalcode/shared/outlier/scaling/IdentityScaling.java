package experimentalcode.shared.outlier.scaling;

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
