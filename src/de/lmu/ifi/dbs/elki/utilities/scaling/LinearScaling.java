package de.lmu.ifi.dbs.elki.utilities.scaling;

import de.lmu.ifi.dbs.elki.math.DoubleMinMax;

/**
 * Simple linear scaling function.
 * 
 * @author Erich Schubert
 */
public class LinearScaling implements StaticScalingFunction {
  /**
   * Scaling factor
   */
  private double factor;

  /**
   * Shift
   */
  private double shift;

  /**
   * Constructor with defaults resulting in identity.
   */
  public LinearScaling() {
    this(1.0, 0.0);
  }

  /**
   * Constructor with scaling only.
   * 
   * @param factor Scaling factor
   */
  public LinearScaling(double factor) {
    this(factor, 0.0);
  }

  /**
   * Full constructor.
   * 
   * @param factor Scaling factor
   * @param shift Shift value
   */
  public LinearScaling(double factor, double shift) {
    this.factor = factor;
    this.shift = shift;
  }

  /**
   * Constructor from a double minmax.
   *
   * @param minmax Minimum and Maximum
   */
  public LinearScaling(DoubleMinMax minmax) {
    this.factor = 1.0 / (minmax.getMax() - minmax.getMin());
    this.shift = -minmax.getMin() / this.factor;
  }

  @Override
  public double getScaled(double d) {
    return factor * d + shift;
  }

  @Override
  public double getMin() {
    return Double.NEGATIVE_INFINITY;
  }

  @Override
  public double getMax() {
    return Double.POSITIVE_INFINITY;
  }

  /**
   * Make a linear scaling from a given minimum and maximum. The minimum will be
   * mapped to zero, the maximum to one.
   * 
   * @param min Minimum
   * @param max Maximum
   * @return New linear scaling.
   */
  public static LinearScaling fromMinMax(double min, double max) {
    double zoom = 1.0 / (max - min);
    return new LinearScaling(zoom, -min / zoom);
  }
}