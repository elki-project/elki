package de.lmu.ifi.dbs.elki.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.regex.Pattern;

/**
 * Provides a Distance for a float-valued distance.
 * 
 * @author Elke Achtert
 */
public class FloatDistance extends NumberDistance<FloatDistance, Float> {
  /**
   * The distance value.
   */
  private float value;

  /**
   * Generated serialVersionUID.
   */
  private static final long serialVersionUID = -5702250266358369075L;
  
  /**
   * Empty constructor for serialization purposes.
   */
  public FloatDistance() {
    super();
  }

  /**
   * Constructs a new FloatDistance object that represents the float argument.
   * 
   * @param value the value to be represented by the FloatDistance.
   */
  public FloatDistance(float value) {
    super();
    this.value = value;
  }

  @Override
  public FloatDistance plus(FloatDistance distance) {
    return new FloatDistance(this.getValue() + distance.getValue());
  }

  @Override
  public FloatDistance minus(FloatDistance distance) {
    return new FloatDistance(this.getValue() - distance.getValue());
  }

  /**
   * Returns a new distance as the product of this distance and the given
   * distance.
   * 
   * @param distance the distance to be multiplied with this distance
   * @return a new distance as the product of this distance and the given
   *         distance
   */
  public FloatDistance times(FloatDistance distance) {
    return new FloatDistance(this.getValue() * distance.getValue());
  }

  /**
   * Returns a new distance as the product of this distance and the given float
   * value.
   * 
   * @param lambda the float value this distance should be multiplied with
   * @return a new distance as the product of this distance and the given double
   *         value
   */
  public FloatDistance times(float lambda) {
    return new FloatDistance(this.getValue() * lambda);
  }

  /**
   * Writes the float value of this FloatDistance to the specified stream.
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeFloat(getValue());
  }

  /**
   * Reads the float value of this FloatDistance from the specified stream.
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException {
    setValue(in.readFloat());
  }

  /**
   * Returns the number of Bytes this distance uses if it is written to an
   * external file.
   * 
   * @return 4 (4 Byte for a float value)
   */
  @Override
  public int externalizableSize() {
    return 4;
  }

  @Override
  public Float getValue() {
    return this.value;
  }

  @Override
  void setValue(Float value) {
    this.value = value;
  }
  
  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public float floatValue() {
    return value;
  }

  @Override
  public long longValue() {
    return (long) value;
  }

  @Override
  public int compareTo(FloatDistance other) {
    return Float.compare(this.value, other.value);
  }

  /**
   * An infinite FloatDistance is based on {@link Float#POSITIVE_INFINITY
   * Float.POSITIVE_INFINITY}.
   */
  @Override
  public FloatDistance infiniteDistance() {
    return new FloatDistance(Float.POSITIVE_INFINITY);
  }

  /**
   * A null FloatDistance is based on 0.
   */
  @Override
  public FloatDistance nullDistance() {
    return new FloatDistance(0.0F);
  }

  /**
   * An undefined FloatDistance is based on {@link Float#NaN Float.NaN}.
   */
  @Override
  public FloatDistance undefinedDistance() {
    return new FloatDistance(Float.NaN);
  }

  /**
   * As pattern is required a String defining a Float.
   */
  @Override
  public FloatDistance parseString(String val) throws IllegalArgumentException {
    if(val.equals(INFINITY_PATTERN)) {
      return infiniteDistance();
    }
  
    if(DoubleDistance.DOUBLE_PATTERN.matcher(val).matches()) {
      return new FloatDistance(Float.parseFloat(val));
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