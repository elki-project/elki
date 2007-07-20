package de.lmu.ifi.dbs.utilities;

/**
 * Encapsulates the attributes of a pair containing two integer ids and a double value.
 *
 * @author Elke Achtert 
 */

public class IDIDDoubleTriple implements Comparable<IDIDDoubleTriple> {

  /**
   * The first id.
   */
  private final int id1;

  /**
   * The second id.
   */
  private final int id2;

  /**
   * The value.
   */
  private final double value;

  /**
   * Constructs a triple of the two given ids and a given value.
   *
   * @param id1   the first id
   * @param id2   the second id
   * @param value the value
   */
  public IDIDDoubleTriple(int id1, int id2, double value) {
    this.id1 = id1;
    this.id2 = id2;
    this.value = value;
  }

  /**
   * Returns the first id of this triple.
   *
   * @return the first id of this pair
   */
  public int getId1() {
    return this.id1;
  }

  /**
   * Returns the second id of this triple.
   *
   * @return the second id of this pair
   */
  public int getId2() {
    return id2;
  }

  /**
   * Returns the value of this triple.
   *
   * @return the value of this triple
   */
  public double getValue() {
    return this.value;
  }

  /**
   * Compares this object with the specified object for order. Returns a
   * negative integer or a positive integer as the value of this object is less
   * than or greater than the the value of the specified object. If both values are equal
   * the ids of both objects are compared.
   *
   * @param o the Object to be compared.
   * @return a negative integer, zero, or a positive integer as this object
   *         is less than, equal to, or greater than the specified object.
   * @throws ClassCastException if the specified object's type prevents it
   *                            from being compared to this Object.
   */
  public int compareTo(IDIDDoubleTriple o) {
    if (this.value < o.value) return -1;
    if (this.value > o.value) return +1;

    if (this.id1 < o.id1) return -1;
    if (this.id1 > o.id1) return +1;

    return (this.id2 - o.id2);
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return id1 + " " + id2 + " " + value;
  }

}
