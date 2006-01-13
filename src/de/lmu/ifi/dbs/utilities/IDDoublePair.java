package de.lmu.ifi.dbs.utilities;

/**
 * Encapsulates the attributes of a pair containing an integer id and a double value.
 *
 * @author Peer Kro&uml;ger (<a href="mailto:kroegerp@dbs.ifi.lmu.de">kroegerp@dbs.ifi.lmu.de</a>)
 */

public class IDDoublePair {

	/**
	 * the id
	 */
	private int id;
	
	/**
	 * the value
	 */
	private double value;

	/**
	 * constructs a pair of a given id and a given value.
	 * 
	 * @param id      the id
	 * @param value   the value
	 */
	public IDDoublePair(int id, double value) {
		this.id = id;
		this.value = value;
	}
	
	/**
	 * returns the id of this pair.
	 * 
	 * @return the id of this pair
	 */
	public int getID() {
		return this.id;
	}
	
	/**
	 * returns the value of this pair.
	 * 
	 * @return the value of this pair
	 */
	public double getValue() {
		return this.value;
	}

}
