package de.lmu.ifi.dbs.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * The CorrelationDistance is a special Distance that indicates the
 * dissimilarity between correlation connected objects. The CorrelationDistance
 * beween two points is a pair consisting of the correlation dimension of two
 * points and the euclidean distance between the two points.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CorrelationDistance<D extends CorrelationDistance<D>> extends AbstractDistance<D> {

  /**
   * Generated SerialVersionUID.
   */
  private static final long serialVersionUID = 2829135841596857929L;

  /**
   * The correlation dimension.
   */
  private int correlationValue;

  /**
   * The euclidean distance.
   */
  private double euklideanValue;

  /**
   * Empty constructor for serialization purposes.
   */
  public CorrelationDistance() {
    // for serialization
  }

  /**
   * Constructs a new CorrelationDistance object.
   *
   * @param correlationValue the correlation dimension to be represented by the
   *                         CorrelationDistance
   * @param euklideanValue   the euclidean distance to be represented by the
   *                         CorrelationDistance
   */
  public CorrelationDistance(int correlationValue, double euklideanValue) {
    this.correlationValue = correlationValue;
    this.euklideanValue = euklideanValue;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    int result;
    long temp;
    result = correlationValue;
    temp = euklideanValue != +0.0d ? Double.doubleToLongBits(euklideanValue) : 0l;
    result = 29 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  /**
   * @see de.lmu.ifi.dbs.distance.Distance#plus(Distance)
   */
  public D plus(D distance) {
    return (D) new CorrelationDistance<D>(this.correlationValue + distance.correlationValue, this.euklideanValue + distance.euklideanValue);
  }

  /**
   * @see de.lmu.ifi.dbs.distance.Distance#minus(Distance)
   */
  public D minus(D distance) {
    return (D) new CorrelationDistance<D>(this.correlationValue - distance.correlationValue, this.euklideanValue - distance.euklideanValue);
  }

  /**
   * @see de.lmu.ifi.dbs.distance.Distance#description()
   */
  public String description() {
    return "CorrelationDistance.correlationValue CorrelationDistance.euklideanValue";
  }

  /**
   * @see Comparable#compareTo(Object)
   */
  public int compareTo(D other) {

    if (this.correlationValue < other.correlationValue) {
      return -1;
    }
    if (this.correlationValue > other.correlationValue) {
      return +1;
    }
    return Double.compare(this.euklideanValue, other.euklideanValue);
  }

  /**
   * The object implements the writeExternal method to save its contents by
   * calling the methods of DataOutput for its primitive values or calling the
   * writeObject method of ObjectOutput for objects, strings, and arrays.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   * @serialData Overriding methods should use this tag to describe the data
   * layout of this Externalizable object. List the sequence of
   * element types and, if possible, relate the element to a
   * public/protected field and/or method of this Externalizable
   * class.
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(correlationValue);
    out.writeDouble(euklideanValue);
  }

  /**
   * The object implements the readExternal method to restore its contents by
   * calling the methods of DataInput for primitive types and readObject for
   * objects, strings and arrays. The readExternal method must read the values
   * in the same sequence and with the same types as were written by
   * writeExternal.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    correlationValue = in.readInt();
    euklideanValue = in.readDouble();
  }

  /**
   * Retuns the number of Bytes this distance uses if it is written to an
   * external file.
   *
   * @return 12 (4 Byte for an integer, 8 Byte for a double value)
   */
  public int externalizableSize() {
    return 12;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return Integer.toString(correlationValue) + " " + Double.toString(euklideanValue);
  }

  /**
   * Returns the correlation dimension between the objects.
   * @return the correlation dimension
   */
  public int getCorrelationValue() {
    return correlationValue;
  }

  /**
   * Returns the euclidean distance between the objects.
   * @return the euclidean distance 
   */
  public double getEuklideanValue() {
    return euklideanValue;
  }

}
