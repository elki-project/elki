package de.lmu.ifi.dbs.elki.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.regex.Pattern;

/**
 * Provides a Distance for a double-valued distance.
 * 
 * @author Elke Achtert
 */
public class DoubleDistance extends NumberDistance<DoubleDistance, Double> {
  /**
   * The actual value.
   */
  double value;

  /**
   * Generated serialVersionUID.
   */
  private static final long serialVersionUID = 3711413449321214862L;

  /**
   * Empty constructor for serialization purposes.
   */
  public DoubleDistance() {
    super();
  }

  /**
   * Constructs a new DoubleDistance object that represents the double argument.
   * 
   * @param value the value to be represented by the DoubleDistance.
   */
  public DoubleDistance(double value) {
    super();
    this.value = value;
  }

  @Override
  public DoubleDistance plus(DoubleDistance distance) {
    return new DoubleDistance(this.getValue() + distance.getValue());
  }

  @Override
  public DoubleDistance minus(DoubleDistance distance) {
    return new DoubleDistance(this.getValue() - distance.getValue());
  }

  /**
   * Returns a new distance as the product of this distance and the given
   * distance.
   * 
   * @param distance the distance to be multiplied with this distance
   * @return a new distance as the product of this distance and the given
   *         distance
   */
  public DoubleDistance times(DoubleDistance distance) {
    return new DoubleDistance(this.getValue() * distance.getValue());
  }

  /**
   * Returns a new distance as the product of this distance and the given double
   * value.
   * 
   * @param lambda the double value this distance should be multiplied with
   * @return a new distance as the product of this distance and the given double
   *         value
   */
  public DoubleDistance times(double lambda) {
    return new DoubleDistance(this.getValue() * lambda);
  }

  /**
   * Writes the double value of this DoubleDistance to the specified stream.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeDouble(this.getValue());
  }

  /**
   * Reads the double value of this DoubleDistance from the specified stream.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException {
    setValue(in.readDouble());
  }

  /**
   * Returns the number of Bytes this distance uses if it is written to an
   * external file.
   * 
   * @return 8 (8 Byte for a double value)
   */
  @Override
  public int externalizableSize() {
    return 8;
  }

  @Override
  public Double getValue() {
    return this.value;
  }

  @Override
  void setValue(Double value) {
    this.value = value;
  }

  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public long longValue() {
    return (long) value;
  }

  @Override
  public int compareTo(DoubleDistance other) {
    return Double.compare(this.value, other.value);
  }

  /**
   * An infinite DoubleDistance is based on {@link Double#POSITIVE_INFINITY
   * Double.POSITIVE_INFINITY}.
   */
  @Override
  public DoubleDistance infiniteDistance() {
    return new DoubleDistance(Double.POSITIVE_INFINITY);
  }

  /**
   * A null DoubleDistance is based on 0.
   */
  @Override
  public DoubleDistance nullDistance() {
    return new DoubleDistance(0.0);
  }

  /**
   * An undefined DoubleDistance is based on {@link Double#NaN Double.NaN}.
   */
  @Override
  public DoubleDistance undefinedDistance() {
    return new DoubleDistance(Double.NaN);
  }

  /**
   * As pattern is required a String defining a Double.
   */
  @Override
  public DoubleDistance parseString(String val) throws IllegalArgumentException {
    if(val.equals(INFINITY_PATTERN)) {
      return infiniteDistance();
    }
    if(testInputPattern(val)) {
      return new DoubleDistance(Double.parseDouble(val));
    }
    else {
      throw new IllegalArgumentException("Given pattern \"" + val + "\" does not match required pattern \"" + requiredInputPattern() + "\"");
    }
  }

  @Override
  public boolean isInfiniteDistance() {
    return Double.isInfinite(value);
  }

  @Override
  public boolean isNullDistance() {
    return (value == 0.0);
  }

  @Override
  public boolean isUndefinedDistance() {
    return Double.isNaN(value);
  }

  @Override
  public Pattern getPattern() {
    return DOUBLE_PATTERN;
  }
}