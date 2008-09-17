package de.lmu.ifi.dbs.elki.utilities.heap;

import de.lmu.ifi.dbs.elki.utilities.Identifiable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Default implementation of the identifiable interface.
 *
 * @author Elke Achtert 
 */
public class DefaultIdentifiable implements Identifiable<DefaultIdentifiable>, Comparable<DefaultIdentifiable>, Externalizable {
  private static final long serialVersionUID = 1;
  /**
   * The unique id of this Identifiable.
   */
  private Integer id;

  /**
   * Empty constructor for serialization purposes.
   */
  public DefaultIdentifiable() {
	  // empty constructor
  }

  /**
   * Creates a new Identifiable object.
   *
   * @param id the unique id of this Identifiable
   */
  public DefaultIdentifiable(Integer id) {
    this.id = id;
  }

  /**
   * Returns the unique id of this object.
   *
   * @return the unique id of this object
   */
  public Integer getID() {
    return id;
  }

  /**
   * Compares this object with the specified object for order. Returns a
   * negative integer, zero, or a positive integer as this object is less
   * than, equal to, or greater than the specified object.
   *
   * @param o the Object to be compared.
   * @return a negative integer, zero, or a positive integer as this object is
   *         less than, equal to, or greater than the specified object.
   */
  public int compareTo(DefaultIdentifiable o) {
    return id - o.getID();
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return id.toString();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the reference object with which to compare.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    final DefaultIdentifiable that = (DefaultIdentifiable) o;

    return id.equals(that.id);
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for the object
   */
  @Override
  public int hashCode() {
    return id.hashCode();
  }

  /**
   * The object implements the writeExternal method to save its contents
   * by calling the methods of DataOutput for its primitive values or
   * calling the writeObject method of ObjectOutput for objects, strings,
   * and arrays.
   *
   * @param out the stream to write the object to
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(id);
  }

  /**
   * The object implements the readExternal method to restore its
   * contents by calling the methods of DataInput for primitive
   * types and readObject for objects, strings and arrays.  The
   * readExternal method must read the values in the same sequence
   * and with the same types as were written by writeExternal.
   *
   * @param in the stream to read data from in order to restore the object
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    id = in.readInt();
  }
}
