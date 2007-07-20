package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.DoubleDistance;

import java.util.regex.Pattern;

/**
 * Provides an abstract superclass for DistanceFunctions that are based on DoubleDistance.
 *
 * @author Arthur Zimek 
 */
public abstract class AbstractDoubleDistanceFunction<O extends DatabaseObject> extends
    AbstractDistanceFunction<O, DoubleDistance> {

  /**
   * Provides a AbstractDoubleDistanceFunction with a pattern defined to accept
   * Strings that define a non-negative Double.
   */
  protected AbstractDoubleDistanceFunction() {
    super(Pattern.compile("\\d+(\\.\\d+)?([eE][-]?\\d+)?"));
  }

  /**
   * An infinite DoubleDistance is based on
   * {@link Double#POSITIVE_INFINITY Double.POSITIVE_INFINITY}.
   *
   * @see DistanceFunction#infiniteDistance()
   */
  public DoubleDistance infiniteDistance() {
    return new DoubleDistance(1.0 / 0.0);
  }

  /**
   * A null DoubleDistance is based on 0.
   *
   * @see DistanceFunction#nullDistance()
   */
  public DoubleDistance nullDistance() {
    return new DoubleDistance(0);
  }

  /**
   * An undefined DoubleDistance is based on {@link Double#NaN Double.NaN}.
   *
   * @see DistanceFunction#undefinedDistance()
   */
  public DoubleDistance undefinedDistance() {
    return new DoubleDistance(0.0 / 0.0);
  }

  /**
   * As pattern is required a String defining a Double.
   *
   * @see DistanceFunction#valueOf(String)
   */
  public DoubleDistance valueOf(String pattern)
      throws IllegalArgumentException {
    if (pattern.equals(INFINITY_PATTERN))
      return infiniteDistance();

    if (matches(pattern)) {
      return new DoubleDistance(Double.parseDouble(pattern));
    }
    else {
      throw new IllegalArgumentException("Given pattern \"" + pattern
                                         + "\" does not match required pattern \""
                                         + requiredInputPattern() + "\"");
    }
  }
}
