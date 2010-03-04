package de.lmu.ifi.dbs.elki.utilities.pairs;


import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Generic SimplePair<FIRST,SECOND> class.
 * 
 * Does not implement any "special" interfaces such as Comparable, use
 * {@link CPair} if you want comparable pairs.
 * 
 * @author Erich Schubert
 * 
 * @param <FIRST> first type
 * @param <SECOND> second type
 */
public class Pair<FIRST, SECOND> {
  /**
   * First value in pair
   */
  public FIRST first;

  /**
   * Second value in pair
   */
  public SECOND second;

  /**
   * Initialize pair
   * 
   * @param first first parameter
   * @param second second parameter
   */
  public Pair(FIRST first, SECOND second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Canonical toString operator
   */
  @Override
  public String toString() {
    return "Pair(" + (first != null ? first.toString() : "null") + ", " + (second != null ? second.toString() : "null") + ")";
  }

  /**
   * Getter for first
   * 
   * @return first element in pair
   */
  public final FIRST getFirst() {
    return first;
  }

  /**
   * Setter for first
   * 
   * @param first new value for first element
   */
  public final void setFirst(FIRST first) {
    this.first = first;
  }

  /**
   * Getter for second element in pair
   * 
   * @return second element in pair
   */
  public final SECOND getSecond() {
    return second;
  }

  /**
   * Setter for second
   * 
   * @param second new value for second element
   */
  public final void setSecond(SECOND second) {
    this.second = second;
  }

  /**
   * Create a new array of the given size (for generics)
   * 
   * @param <F> First class
   * @param <S> Second class
   * @param size array size
   * @return empty array of the new type.
   */
  public static final <F, S> Pair<F, S>[] newPairArray(int size) {
    Class<Pair<F,S>> paircls = ClassGenericsUtil.uglyCastIntoSubclass(Pair.class);
    return ClassGenericsUtil.newArrayOfNull(size, paircls);
  }

  /**
   * Simple equals statement.
   * 
   * This Pair equals another Object if they are identical or
   * if the other Object is also a Pair and the {@link #first} and {@link #second} element
   * of this Pair equal the {@link #first} and {@link #second} element, respectively, of the other Pair. 
   * 
   * @param obj Object to compare to
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(!(obj instanceof Pair)) {
      return false;
    }
    Pair<FIRST, SECOND> other = (Pair<FIRST, SECOND>) obj;
    return (this.first.equals(other.getFirst())) && (this.second.equals(other.getSecond()));
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
    result = prime * result + ((first == null) ? 0 : first.hashCode());
    result = prime * result + ((second == null) ? 0 : second.hashCode());
    return (int) result;
  }
}