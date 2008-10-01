package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Pair with canonical comparison function.
 * 
 * This class cannot be derived from SimplePair (although it should be)
 * Because SimplePair has been declared as "final" for performance reasons.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 *
 * @param <FIRST> first type
 * @param <SECOND> second type
 */
public final class ComparablePair<FIRST extends Comparable<FIRST>,SECOND extends Comparable<SECOND>> implements PairInterface<FIRST,SECOND>, Comparable<ComparablePair<FIRST,SECOND>>, ComparableSwapped<ComparablePair<FIRST,SECOND>> {
  /* these are public by intention, Pair<> is supposed to be a simple wrapper */
  public FIRST first;
  public SECOND second;

  public ComparablePair(FIRST first, SECOND second) {
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
   * Generic derived compare function.
   */
  public int compareTo(ComparablePair<FIRST, SECOND> other) {
    if (this.first == null) {
      if (other.first != null) return +1;
      if (this.second == null) {
        if (other.second != null) return +1;
        else return 0;
      }
    } else {
      if (other.first == null) return -1;
      int delta1 = this.first.compareTo(other.first);
      if (delta1 != 0) return delta1;
    }
    if (other.second == null) return -1;
    return this.second.compareTo(other.second);
  }
  
  /**
   * Generic derived compare function, with swapped components.
   */
  public int compareSwappedTo(ComparablePair<FIRST, SECOND> other) {
    if (this.second == null) {
      if (other.second != null) return +1;
      if (this.first == null) {
        if (other.first != null) return +1;
        else return 0;
      }
    } else {
      if (other.second == null) return -1;
      int delta1 = this.second.compareTo(other.second);
      if (delta1 != 0) return delta1;
    }
    if (other.first == null) return -1;
    return this.first.compareTo(other.first);
  }
  
  /**
   * Array constructor for generics
   */
  @SuppressWarnings("unchecked")
  public static final <F extends Comparable<F>,S extends Comparable<S>> ComparablePair<F,S>[] newArray(int size) {
    return new ComparablePair[size];
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PairInterface)) return false;
    PairInterface<FIRST,SECOND> other = (PairInterface<FIRST,SECOND>) obj;
    return (this.first.equals(other.getFirst())) && (this.second.equals(other.getSecond()));
  }

  @Override
  public int hashCode() {
    // primitive hash function mixing the two integers.
    // this number does supposedly not have any factors in common with 2^32
    return (int) (this.first.hashCode() * 2654435761L + second.hashCode());
  }
}
