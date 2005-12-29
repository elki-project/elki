package de.lmu.ifi.dbs.utilities.heap;

import java.io.Serializable;

/**
 * Default implementation of the identifiable interface.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DefaultIdentifiable implements Identifiable, Serializable {
  /**
   * The unique id of this Identifiable.
   */
  private Integer id;

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
   * Compares this object with the specified object for order.  Returns a
   * negative integer, zero, or a positive integer as this object is less
   * than, equal to, or greater than the specified object.
   *
   * @param o the Object to be compared.
   * @return a negative integer, zero, or a positive integer as this object
   *         is less than, equal to, or greater than the specified object.
   */
  public int compareTo(Identifiable o) {
    return id - o.getID();
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return id.toString();
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param o the reference object with which to compare.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final DefaultIdentifiable that = (DefaultIdentifiable) o;

    return id.equals(that.id);
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for the object
   */
  public int hashCode() {
    return id.hashCode();
  }
}
