package de.lmu.ifi.dbs.elki.database.ids.integer;

import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;

/**
 * DBID pair using two ints for storage.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf IntegerDBID
 */
public class IntegerDBIDPair implements DBIDPair {
  /**
   * First value in pair
   */
  public int first;

  /**
   * Second value in pair
   */
  public int second;

  /**
   * Initialize pair
   * 
   * @param first first parameter
   * @param second second parameter
   */
  public IntegerDBIDPair(int first, int second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Canonical toString operator
   */
  @Override
  public String toString() {
    return "Pair(" + first + ", " + second + ")";
  }

  /**
   * Getter for first
   * 
   * @return first element in pair
   */
  @Override
  public final IntegerDBID getFirst() {
    return new IntegerDBID(first);
  }

  /**
   * Getter for second element in pair
   * 
   * @return second element in pair
   */
  @Override
  public final IntegerDBID getSecond() {
    return new IntegerDBID(second);
  }

  /**
   * Simple equals statement.
   * 
   * This Pair equals another Object if they are identical or if the other
   * Object is also a Pair and the {@link #first} and {@link #second} element of
   * this Pair equal the {@link #first} and {@link #second} element,
   * respectively, of the other Pair.
   * 
   * @param obj Object to compare to
   */
  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(!(obj instanceof IntegerDBIDPair)) {
      return false;
    }
    IntegerDBIDPair other = (IntegerDBIDPair) obj;
    return (this.first == other.first) && (this.second == other.second);
  }

  /**
   * Canonical hash function, mixing the two hash values.
   */
  @Override
  public final int hashCode() {
    // primitive hash function mixing the two integers.
    // this number does supposedly not have any factors in common with 2^32
    final long prime = 2654435761L;
    long result = 1;
    result = prime * result + first;
    result = prime * result + second;
    return (int) result;
  }
}