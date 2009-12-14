package de.lmu.ifi.dbs.elki.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

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

  public String description() {
    return "FloatDistance.floatValue";
  }

  public FloatDistance plus(FloatDistance distance) {
    return new FloatDistance(this.getValue() + distance.getValue());
  }

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
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeFloat(getValue());
  }

  /**
   * Reads the float value of this FloatDistance from the specified stream.
   */
  public void readExternal(ObjectInput in) throws IOException {
    setValue(in.readFloat());
  }

  /**
   * Returns the number of Bytes this distance uses if it is written to an
   * external file.
   * 
   * @return 4 (4 Byte for a float value)
   */
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
}
