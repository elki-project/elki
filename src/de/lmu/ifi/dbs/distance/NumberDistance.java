package de.lmu.ifi.dbs.distance;

/**
 * Provides a Distance for a number-valued distance.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
@SuppressWarnings("serial")
public abstract class NumberDistance extends AbstractDistance {

  /**
   * Empty constructor for serialization purposes.
   */
  public NumberDistance() {
  }

  /**
   * @see Distance
   */
  public String description() {
    return "distance";
  }

  /**
   * Returns the double value of this distance.
   *
   * @return the double value of this distance
   */
  public abstract double getDoubleValue();
}
