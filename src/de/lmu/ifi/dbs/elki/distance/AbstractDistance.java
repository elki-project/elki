package de.lmu.ifi.dbs.elki.distance;

import java.util.regex.Pattern;

/**
 * An abstract distance implements equals conveniently for any extending class.
 * At the same time any extending class is to implement hashCode properly.
 * 
 * See {@link de.lmu.ifi.dbs.elki.distance.DistanceUtil} for related utility
 * functions such as <code>min</code>, <code>max</code>.
 * 
 * @author Arthur Zimek
 * @see de.lmu.ifi.dbs.elki.distance.DistanceUtil
 * @param <D> the (final) type of Distance used
 */
public abstract class AbstractDistance<D extends AbstractDistance<D>> implements Distance<D> {
  /**
   * Indicates an infinity pattern.
   */
  public static final String INFINITY_PATTERN = "inf";
  
  /**
   * Pattern for parsing and validating double values
   */
  public static final Pattern DOUBLE_PATTERN = Pattern.compile("(\\d+|\\d*\\.\\d+)?([eE][-]?\\d+)?");
  
  /**
   * Pattern for parsing and validating integer values
   */
  public static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

  /**
   * Any extending class should implement a proper hashCode method.
   */
  @Override
  public abstract int hashCode();

  /**
   * Returns true if <code>this == o</code> has the value <code>true</code> or o
   * is not null and o is of the same class as this instance and
   * <code>this.compareTo(o)</code> is 0, false otherwise.
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }

    if(o == null || getClass() != o.getClass()) {
      return false;
    }

    return this.compareTo((D) o) == 0;
  }

  /**
   * Get the pattern accepted by this distance
   * 
   * @return Pattern
   */
  abstract public Pattern getPattern();
  
  @Override
  public final String requiredInputPattern() {
    return getPattern().pattern();
  }
  
  /**
   * Test a string value against the input pattern.
   * 
   * @param value String value to test
   * @return Match result
   */
  public final boolean testInputPattern(String value) {
    return getPattern().matcher(value).matches();
  }

  @Override
  public boolean isInfiniteDistance() {
    return this.equals(infiniteDistance());
  }

  @Override
  public boolean isNullDistance() {
    return this.equals(nullDistance());
  }

  @Override
  public boolean isUndefinedDistance() {
    return this.equals(undefinedDistance());
  }
}