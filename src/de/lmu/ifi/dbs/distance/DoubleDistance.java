package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.utilities.Util;

/**
 * Provides a Distance for a double-valued distance.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
@SuppressWarnings("serial")
class DoubleDistance extends AbstractDistance {
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
  public DoubleDistance(double value) {
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
  public Distance plus(Distance distance) {
    DoubleDistance other = (DoubleDistance) distance;
    return new DoubleDistance(this.value + other.value);
  }

  /**
   * @see de.lmu.ifi.dbs.distance.Distance#minus(Distance)
   */
  public Distance minus(Distance distance) {
    DoubleDistance other = (DoubleDistance) distance;
    return new DoubleDistance(this.value - other.value);
  }

  /**
   * @see de.lmu.ifi.dbs.distance.Distance#times(Distance)
   */
  public Distance times(Distance distance) {
    DoubleDistance other = (DoubleDistance) distance;
    return new DoubleDistance(this.value * other.value);
  }

  /**
   * @see de.lmu.ifi.dbs.distance.Distance#times(Distance)
   */
  public Distance times(double lambda) {
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
  public int compareTo(Distance o) {
    DoubleDistance other = (DoubleDistance) o;
    return Double.compare(this.value, other.value);
  }

  /**
   * Returns a string representation of this distance.
   *
   * @return a string representation of this distance.
   * todo wieder raus
   */
  public String toString() {
    return Util.format(value, 6);
//    return Double.toString(value);
  }
}
