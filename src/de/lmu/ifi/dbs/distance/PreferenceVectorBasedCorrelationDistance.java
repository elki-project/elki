package de.lmu.ifi.dbs.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.BitSet;

/**
 * A PreferenceVectorBasedCorrelationDistance holds additionally to the
 * CorrelationDistance the common preference vector of
 * the two objects defining the distance.
 *
 * @author Elke Achtert 
 */
public class PreferenceVectorBasedCorrelationDistance extends CorrelationDistance<PreferenceVectorBasedCorrelationDistance> {
  private BitSet commonPreferenceVector;

  /**
   * Empty constructor for serialization purposes.
   */
  public PreferenceVectorBasedCorrelationDistance() {
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
  public PreferenceVectorBasedCorrelationDistance(int correlationValue, double euklideanValue, BitSet commonPreferenceVector) {
    super(correlationValue, euklideanValue);
    this.commonPreferenceVector = commonPreferenceVector;
  }

  /**
   * Returns the common preference vector of
   * the two objects defining this distance.
   *
   * @return the common preference vector
   */
  public BitSet getCommonPreferenceVector() {
    return commonPreferenceVector;
  }

  /**
   * @see de.lmu.ifi.dbs.distance.Distance#description()
   */
  public String description() {
    return "PreferenceVectorBasedCorrelationDistance.correlationValue, " +
           "PreferenceVectorBasedCorrelationDistance.euklideanValue, " +
           "PreferenceVectorBasedCorrelationDistance.commonPreferenceVector";
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
    // todo
    super.writeExternal(out);
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
    // todo
    super.readExternal(in);
  }

  /**
   * Retuns the number of Bytes this distance uses if it is written to an
   * external file.
   *
   * @return 12 (4 Byte for an integer, 8 Byte for a double value)
   */
  public int externalizableSize() {
    // todo
    return super.externalizableSize();
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return super.toString() + " " + commonPreferenceVector.toString();
  }

  /**
   * @see Distance#plus(Distance)
   *
   */
  public PreferenceVectorBasedCorrelationDistance plus(PreferenceVectorBasedCorrelationDistance distance) {
    return new PreferenceVectorBasedCorrelationDistance(getCorrelationValue() + distance.getCorrelationValue(),
                                                        getEuklideanValue() + distance.getEuklideanValue(),
                                                        new BitSet());
  }

  /**
   * @see Distance#minus(Distance)
   *
   */
  public PreferenceVectorBasedCorrelationDistance minus(PreferenceVectorBasedCorrelationDistance distance) {
   return new PreferenceVectorBasedCorrelationDistance(getCorrelationValue() - distance.getCorrelationValue(),
                                                        getEuklideanValue() - distance.getEuklideanValue(), 
                                                        new BitSet());
  }

  /**
   * @see Comparable#compareTo(Object)
   *
   * @throws UnsupportedOperationException
   */
  public int compareTo(PreferenceVectorBasedCorrelationDistance o) {
    return super.compareTo(o);
  }
}
