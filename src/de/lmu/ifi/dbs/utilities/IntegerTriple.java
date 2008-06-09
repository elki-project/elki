package de.lmu.ifi.dbs.utilities;

/**
 * Provides a key of three integers. Two IntegerTriple-Objects are equal, if
 * they consist of the same three integers defined in the same order.
 * 
 * @author Noemi Andor
 */
public class IntegerTriple implements Comparable<IntegerTriple> {

	/**
	 * First integer.
	 */
	private Integer first;

	/**
	 * Second integer.
	 */
	private Integer second;

	/**
	 * Third integer.
	 */
	private Integer last;

	/**
	 * @return the first integer-value of the IntegerTriple-Object.
	 */
	public int getFirst() {
		return this.first;
	}

	/**
	 * @return the second integer-value of the IntegerTriple-Object.
	 */
	public int getSecond() {
		return this.second;
	}

	/**
	 * @return the third integer-value of the IntegerTriple-Object.
	 */
	public int getLast() {
		return this.last;
	}

	/**
	 * Constructor which initiates an IntegerTriple Object with three integers.
	 * 
	 * @param first
	 *            first integer-value
	 * @param second
	 *            second integer-value
	 * @param last
	 *            third integer-value
	 */
	public IntegerTriple(int first, int second, int last) {
		this.first = first;
		this.second = second;
		this.last = last;
	}

	/**
	 * Empty constructor creating an IntegerTriple-Object.
	 */
	public IntegerTriple() {

	}

	/**
	 * Compares this IntegerTriple with an IntegerTriple o. If this Object is
	 * greater than the other object, then 1 is returned, else if the two
	 * Objects are equal, 0 is returned. If IntegerTriple o is greater then this
	 * IntegerTriple, then -1 is returned.
	 */
	public int compareTo(IntegerTriple o) {
		if (this.getFirst() > o.getFirst()) {
			return 1;
		} else if (this.getFirst() < o.getFirst()) {
			return -1;
		} else {// this.getFirst() == o.getFirst()
			if (this.getSecond() > o.getSecond()) {
				return 1;
			} else if (this.getSecond() < o.getSecond()) {
				return -1;
			} else {// this.getSecond() == o.getSecond()
				if (this.getLast() > o.getLast()) {
					return 1;
				} else if (this.getLast() < o.getLast()) {
					return -1;
				} else {
					return 0;
				}
			}
		}
	}

	/**
	 * Creates the hashCode dependent on the three integers this Object consist.
	 * @return the hashCode of this Object
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((last == null) ? 0 : last.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	/**
	 * Verifies if this Object is equals to Object obj.
	 * @param obj   object to be compared with this object
	 * @return true  if this Object is equals to Object obj, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final IntegerTriple other = (IntegerTriple) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (last == null) {
			if (other.last != null)
				return false;
		} else if (!last.equals(other.last))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}

	/**
	 * Sets the first Argument of this Object.
	 * @param first
	 */
	public void setFirst(int first) {
		this.first = first;
	}

	/**
	 * Sets the second Argument of this Object.
	 * @param second
	 */
	public void setSecond(int second) {
		this.second = second;
	}

	/**
	 * Sets the third Argument of this Object.
	 * @param last
	 */
	public void setLast(int last) {
		this.last = last;
	}

}
