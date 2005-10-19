package de.lmu.ifi.dbs.distance;

/**
 * Provides a Distance for a double-valued distance.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
@SuppressWarnings("serial")
public class DoubleDistance extends AbstractDistance {
  /**
   * The double value of this distance.
   */
  private double value;

  /**
   * Constructs a new DoubleDistance object that represents the double
   * argument.
   *
   * @param value the value to be represented by the DoubleDistance.
   */
  DoubleDistance(double value) {
    this.value = value;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    long bits = Double.doubleToLongBits(value);
    return (int) (bits ^ (bits >>> 32));
  }

  /**
   * @see de.lmu.ifi.dbs.distance.Distance#plus(Distance)
   */
  public DoubleDistance plus(Distance distance) {
    DoubleDistance other = (DoubleDistance) distance;
    return new DoubleDistance(this.value + other.value);
  }

  /**
   * @see de.lmu.ifi.dbs.distance.Distance#minus(Distance)
   */
  public DoubleDistance minus(Distance distance) {
    DoubleDistance other = (DoubleDistance) distance;
    return new DoubleDistance(this.value - other.value);
  }

  /**
   * Returns a new distance as the product of this distance and the given distance.
   *
   * @param distance the distancce to be multiplied with this distance
   * @return a new distance as the product of this distance and the given distance
   */
  public DoubleDistance times(Distance distance) {
    DoubleDistance other = (DoubleDistance) distance;
    return new DoubleDistance(this.value * other.value);
  }

  /**
   * Returns a new distance as the product of this distance and the given double value.
   *
   * @param lambda the double value this distance should be multiplied with
   * @return a new distance as the product of this distance and the given double value
   */
  public DoubleDistance times(double lambda) {
    return new DoubleDistance(this.value * lambda);
  }

  /**
   * @see de.lmu.ifi.dbs.distance.Distance
   */
  public String description() {
    return "distance";
  }

  /**
   * @see Comparable#compareTo(Object)
   */
  public int compareTo(Object o) {
    DoubleDistance other = (DoubleDistance) o;
    return Double.compare(this.value, other.value);
  }

  /**
   * Returns a string representation of this distance.
   *
   * @return a string representation of this distance.
   */
  public String toString() {
    return Double.toString(value);
  }
}
