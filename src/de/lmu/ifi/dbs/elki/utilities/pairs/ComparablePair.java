package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Pair with canonical comparison function.
 * 
 * This class cannot be derived from SimplePair (although it should be)
 * Because SimplePair has been declared as "final" for performance reasons.
 * 
 * @author Erich Schubert
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
  @Override
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
    // try comparing by first
    if (this.first != null) {
      if (other.first == null) return -1;
      int delta1 = this.first.compareTo(other.first);
      if (delta1 != 0) return delta1;
    } else
      if (other.first != null) return +1;
    // try comparing by second
    if (this.second != null) {
      if (other.second == null) return -1;
      int delta2 = this.second.compareTo(other.second);
      if (delta2 != 0) return delta2;
    } else
      if (other.second != null) return +1;
    return 0;
  }
  
  /**
   * Generic derived compare function, with swapped components.
   */
  public int compareSwappedTo(ComparablePair<FIRST, SECOND> other) {
    // try comparing by second
    if (this.second != null) {
      if (other.second == null) return -1;
      int delta2 = this.second.compareTo(other.second);
      if (delta2 != 0) return delta2;
    } else
      if (other.second != null) return +1;
    // try comparing by first
    if (this.first != null) {
      if (other.first == null) return -1;
      int delta1 = this.first.compareTo(other.first);
      if (delta1 != 0) return delta1;
    } else
      if (other.first != null) return +1;
    return 0;
  }
  
  /**
   * Array constructor for generics
   */
  @SuppressWarnings("unchecked")
  public static final <F extends Comparable<F>,S extends Comparable<S>> ComparablePair<F,S>[] newArray(int size) {
    return new ComparablePair[size];
  }
  
  /**
   * canonical equals implementation
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (!(obj instanceof PairInterface)) return false;
    PairInterface<FIRST,SECOND> other = (PairInterface<FIRST,SECOND>) obj;
    if (this.first == null) {
      if (other.getFirst() != null) return false;
    } else {
      if (other.getFirst() == null) return false;
      if (!this.first.equals(other.getFirst())) return false;
    }
    if (this.second == null) {
      if (other.getSecond() != null) return false;
    } else {
      if (other.getSecond() == null) return false;
      if (!this.second.equals(other.getSecond())) return false;
    }
    return true;
  }

  /**
   * Canonical Hash function via mixing
   */
  @Override
  public int hashCode() {
    // primitive hash function mixing the two integers.
    // this number does supposedly not have any factors in common with 2^32
    final long prime = 2654435761L;
    long result = 1;
    result = prime * result + ((first == null) ? 0 : first.hashCode());
    result = prime * result + ((second == null) ? 0 : second.hashCode());
    return (int) result;
  }
}
