package de.lmu.ifi.dbs.distance;

import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;

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
   * Empty constructor for serialization purposes.
   */
  public DoubleDistance() {
  }

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
  public DoubleDistance times(DoubleDistance distance) {
    return new DoubleDistance(this.value * distance.value);
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
  public int compareTo(Distance d) {
    DoubleDistance other = (DoubleDistance) d;
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

  /**
   * The object implements the writeExternal method to save its contents
   * by calling the methods of DataOutput for its primitive values or
   * calling the writeObject method of ObjectOutput for objects, strings,
   * and arrays.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   * @serialData Overriding methods should use this tag to describe
   * the data layout of this Externalizable object.
   * List the sequence of element types and, if possible,
   * relate the element to a public/protected field and/or
   * method of this Externalizable class.
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeDouble(value);
  }

  /**
   * The object implements the readExternal method to restore its
   * contents by calling the methods of DataInput for primitive
   * types and readObject for objects, strings and arrays.  The
   * readExternal method must read the values in the same sequence
   * and with the same types as were written by writeExternal.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    value = in.readDouble();
  }

  /**
   * Retuns the number of Bytes this distance uses if it is written to
   * an external file.
   *
   * @return 8 (8 Byte for a double value)
   */
  public int externalizableSize() {
    return 8;
  }

  /**
   * Returns the double value of this distance.
   * @return the double value of this distance
   */
  public double getValue() {
    return value;
  }
}
