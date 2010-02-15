package de.lmu.ifi.dbs.elki.utilities;

/**
 * MBR class allowing modifications (as opposed to {@link HyperBoundingBox}).
 * 
 * @author Marisa Thoma
 * 
 */
public class ModifiableHyperBoundingBox extends HyperBoundingBox {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1;

  public ModifiableHyperBoundingBox() {
    super();
  }

  /**
   * Uses the references to the fields in <code>hbb</code> as <code>min</code>,
   * <code>max</code> fields. Thus, this constructor indirectly provides a way
   * to modify the fields of a {@link HyperBoundingBox}.
   * 
   * @param hbb
   */
  public ModifiableHyperBoundingBox(HyperBoundingBox hbb) {
    super(hbb.min, hbb.max);
  }

  /**
   * Creates a ModifiableHyperBoundingBox for the given hyper points.
   * 
   * @param min - the coordinates of the minimum hyper point
   * @param max - the coordinates of the maximum hyper point
   */
  public ModifiableHyperBoundingBox(double[] min, double[] max) {
    if(min.length != max.length) {
      throw new IllegalArgumentException("min/max need same dimensionality");
    }
    this.min = min;
    this.max = max;
  }

  /**
   * Set the maximum bound in dimension <code>dimension</code> to value
   * <code>value</code>.
   * 
   * @param dimension the dimension for which the coordinate should be set,
   *        where 1 &le; dimension &le; <code>this.getDimensionality()</code>
   * @param value the coordinate to set as upper bound for dimension
   *        <code>dimension</code>
   */
  public void setMax(int dimension, double value) {
    max[dimension - 1] = value;
  }

  /**
   * Set the minimum bound in dimension <code>dimension</code> to value
   * <code>value</code>.
   * 
   * @param dimension the dimension for which the lower bound should be set,
   *        where 1 &le; dimension &le; <code>this.getDimensionality()</code>
   * @param value the coordinate to set as lower bound for dimension
   *        <code>dimension</code>
   */
  public void setMin(int dimension, double value) {
    max[dimension - 1] = value;
  }

  /**
   * Returns a reference to the minimum hyper point.
   * 
   * @return the minimum hyper point
   */
  public double[] getMinRef() {
    return min;
  }

  /**
   * Returns the reference to the maximum hyper point.
   * 
   * @return the maximum hyper point
   */
  public double[] getMaxRef() {
    return max;
  }
}
