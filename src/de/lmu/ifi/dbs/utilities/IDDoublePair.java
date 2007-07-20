package de.lmu.ifi.dbs.utilities;

/**
 * Encapsulates the attributes of a pair containing an integer id and a double value.
 *
 * @author Peer Kro&uml;ger 
 */

public class IDDoublePair implements Comparable<IDDoublePair> {

	/**
	 * The id.
	 */
	private final int id;
	
	/**
	 * The value.
	 */
	private final double value;

	/**
	 * Constructs a pair of a given id and a given value.
	 * 
	 * @param id      the id
	 * @param value   the value
	 */
	public IDDoublePair(int id, double value) {
		this.id = id;
		this.value = value;
	}
	
	/**
	 * Returns the id of this pair.
	 * 
	 * @return the id of this pair
	 */
	public int getID() {
		return this.id;
	}
	
	/**
	 * Returns the value of this pair.
	 * 
	 * @return the value of this pair
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
  public int compareTo(IDDoublePair o) {
    if (this.value < o.value) return -1;
    if (this.value > o.value) return +1;

    return (this.id - o.id);
  }

}
