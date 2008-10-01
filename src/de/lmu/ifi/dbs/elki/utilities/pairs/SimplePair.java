package de.lmu.ifi.dbs.elki.utilities.pairs;


/**
 * Generic SimplePair<FIRST,SECOND> class.
 * 
 * Does not implement any "special" interfaces such as Comparable
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 *
 * @param <FIRST> first type
 * @param <SECOND> second type
 */
public final class SimplePair<FIRST, SECOND> implements PairInterface<FIRST, SECOND> {
  /* these are public by intention, Pair<> is supposed to be a simple wrapper */
  public FIRST first;
  public SECOND second;

  /**
   * Initialize pair
   * 
   * @param first first parameter
   * @param second second parameter
   */
  public SimplePair(FIRST first, SECOND second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Canonical toString operator
   */
  public String toString() {
    return "Pair(" + first.toString() + ", " + second.toString() + ")";
  }

  /**
   * Getter for first
   * 
   * @return first element in pair
   */
  public FIRST getFirst() {
    return first;
  }

  /**
   * Setter for first
   * 
   * @param first new value for first element
   */
  public void setFirst(FIRST first) {
    this.first = first;
  }

  /**
   * Getter for second element in pair
   * 
   * @return second element in pair
   */
  public SECOND getSecond() {
    return second;
  }

  /**
   * Setter for second
   * 
   * @param second new value for second element
   */
  public void setSecond(SECOND second) {
    this.second = second;
  }
  
  /**
   * Simple equals statement
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof PairInterface)) return false;
    PairInterface<FIRST,SECOND> other = (PairInterface<FIRST,SECOND>) obj;
    return (this.first.equals(other.getFirst())) && (this.second.equals(other.getSecond()));
  }

  /**
   * Canonical hash function, mixing the two hash values.
   */
  @Override
  public int hashCode() {
    // primitive hash function mixing the two integers.
    // this number does supposedly not have any factors in common with 2^32
    return (int) (this.first.hashCode() * 2654435761L + second.hashCode());
  }
}