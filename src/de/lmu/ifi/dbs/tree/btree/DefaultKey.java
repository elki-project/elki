package de.lmu.ifi.dbs.tree.btree;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Default class for an object representing a key in a B-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DefaultKey implements Comparable<DefaultKey>, Externalizable {
  /**
   * The key.
   */
  private Integer key;

  /**
   * Empty constructor for serialization purposes.
   */
  public DefaultKey() {
	  // empty constructor
  }

  /**
   * Provides an object representing a key in a B-Tree.
   *
   * @param key the key
   */
  public DefaultKey(Integer key) {
    this.key = key;
  }

  /**
   * Compares this object with the specified object for order.  Returns a
   * negative integer, zero, or a positive integer as this object is less
   * than, equal to, or greater than the specified object.
   *
   * @param o the Object to be compared.
   * @return a negative integer, zero, or a positive integer as this object
   *         is less than, equal to, or greater than the specified object.
   */
  public int compareTo(DefaultKey o) {
    return this.key - o.key;
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
    out.writeInt(key);
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
    key = in.readInt();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the reference object with which to compare.
   * @return <code>true</code> if this object has the same key as the obj
   *         argument; <code>false</code> otherwise.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final DefaultKey that = (DefaultKey) o;
    return key.equals(that.key);
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for the object
   */
  public int hashCode() {
    return key.hashCode();
  }

  /**
   * Returns the integer value of this key.
   * @return the integer value of this key
   */
  public Integer value() {
    return key;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return key.toString();
  }
}
