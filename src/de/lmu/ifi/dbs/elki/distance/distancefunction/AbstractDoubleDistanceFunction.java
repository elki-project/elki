package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;

import java.util.regex.Pattern;

/**
 * Provides an abstract superclass for DistanceFunctions that are based on
 * DoubleDistance.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject to compute the distances in between
 */
public abstract class AbstractDoubleDistanceFunction<O extends DatabaseObject> extends AbstractDistanceFunction<O, DoubleDistance> {
  /**
   * Pattern for parsing and validating double values
   */
  public static final Pattern DOUBLE_PATTERN = Pattern.compile("(\\d+|\\d*\\.\\d+)?([eE][-]?\\d+)?");

  /**
   * Provides a AbstractDoubleDistanceFunction with a pattern defined to accept
   * Strings that define a non-negative Double.
   */
  protected AbstractDoubleDistanceFunction() {
    super(DOUBLE_PATTERN);
  }

  /**
   * An infinite DoubleDistance is based on {@link Double#POSITIVE_INFINITY
   * Double.POSITIVE_INFINITY}.
   */
  public DoubleDistance infiniteDistance() {
    return new DoubleDistance(Double.POSITIVE_INFINITY);
    // return new DoubleDistance(1.0 / 0.0);
  }

  /**
   * A null DoubleDistance is based on 0.
   */
  public DoubleDistance nullDistance() {
    return new DoubleDistance(0.0);
  }

  /**
   * An undefined DoubleDistance is based on {@link Double#NaN Double.NaN}.
   */
  public DoubleDistance undefinedDistance() {
    return new DoubleDistance(Double.NaN);
    // return new DoubleDistance(0.0 / 0.0);
  }

  /**
   * As pattern is required a String defining a Double.
   */
  public DoubleDistance valueOf(String pattern) throws IllegalArgumentException {
    if(pattern.equals(INFINITY_PATTERN)) {
      return infiniteDistance();
    }

    if(matches(pattern)) {
      return new DoubleDistance(Double.parseDouble(pattern));
    }
    else {
      throw new IllegalArgumentException("Given pattern \"" + pattern + "\" does not match required pattern \"" + requiredInputPattern() + "\"");
    }
  }
}