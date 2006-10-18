package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.FloatDistance;

import java.util.regex.Pattern;

/**
 * Provides a DistanceFunction that is based on FloatDistance.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class FloatDistanceFunction<O extends DatabaseObject> extends
    AbstractDistanceFunction<O, FloatDistance> {

  /**
   * Provides a FloatDistanceFunction with a pattern defined to accept Strings
   * that define a non-negative Float.
   */
  protected FloatDistanceFunction() {
    super(Pattern.compile("\\d+(\\.\\d+)?([eE][-]?\\d+)?"));
  }

  /**
   * An infinite FloatDistance is based on
   * {@link Float#POSITIVE_INFINITY Float.POSITIVE_INFINITY}.
   *
   * @see DistanceFunction#infiniteDistance()
   */
  public FloatDistance infiniteDistance() {
    return new FloatDistance(1.0F / 0.0F);
  }

  /**
   * A null FloatDistance is based on 0.
   *
   * @see DistanceFunction#nullDistance()
   */
  public FloatDistance nullDistance() {
    return new FloatDistance(0);
  }

  /**
   * An undefined FloatDistance is based on {@link Float#NaN Float.NaN}.
   *
   * @see DistanceFunction#undefinedDistance()
   */
  public FloatDistance undefinedDistance() {
    return new FloatDistance(0.0F / 0.0F);
  }

  /**
   * As pattern is required a String defining a Float.
   *
   * @see DistanceFunction#valueOf(String)
   */
  public FloatDistance valueOf(String pattern)
      throws IllegalArgumentException {
    if (pattern.equals(INFINITY_PATTERN))
      return infiniteDistance();

    if (matches(pattern)) {
      return new FloatDistance(Float.parseFloat(pattern));
    }
    else {
      throw new IllegalArgumentException("Given pattern \"" + pattern
                                         + "\" does not match required pattern \""
                                         + requiredInputPattern() + "\"");
    }
  }
}
