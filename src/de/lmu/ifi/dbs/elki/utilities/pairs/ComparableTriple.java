package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Triple with canonical comparison function.
 * 
 * @author Erich Schubert
 *
 * @param <FIRST> first type
 * @param <SECOND> second type
 * @param <THIRD> second type
 */
public final class ComparableTriple<FIRST extends Comparable<FIRST>,SECOND extends Comparable<SECOND>, THIRD extends Comparable<THIRD>> implements Comparable<ComparableTriple<FIRST,SECOND,THIRD>> {
  /* these are public by intention, Pair<> is supposed to be a simple wrapper */
  public FIRST first;
  public SECOND second;
  public THIRD third;

  /**
   * Constructor with fields
   * 
   * @param first
   * @param second
   * @param third
   */
  public ComparableTriple(FIRST first, SECOND second, THIRD third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  /**
   * Canonical toString operator
   */
  @Override
  public String toString() {
    return "Triple(" + first.toString() + ", " + second.toString() + ", " + third.toString() + ")";
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
   * Getter for third
   * 
   * @return third element in pair
   */
  public THIRD getThird() {
    return third;
  }

  /**
   * Setter for third
   * 
   * @param third new value for third element
   */
  public void setThird(THIRD third) {
    this.third = third;
  }

  /**
   * Generic derived compare function.
   */
  public int compareTo(ComparableTriple<FIRST, SECOND, THIRD> other) {
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
    // try comparing by third
    if (this.third != null) {
      if (other.third == null) return -1;
      int delta3 = this.third.compareTo(other.third);
      if (delta3 != 0) return delta3;
    } else
      if (other.third != null) return +1;
    return 0;
  }
  
  /**
   * Array constructor for generics
   */
  @SuppressWarnings("unchecked")
  public static final <F extends Comparable<F>,S extends Comparable<S>, T extends Comparable<T>> ComparableTriple<F,S,T>[] newArray(int size) {
    return new ComparableTriple[size];
  }

  /**
   * Canonical equals function
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (!(obj instanceof ComparableTriple)) return false;
    ComparableTriple<FIRST,SECOND,THIRD> other = (ComparableTriple<FIRST,SECOND,THIRD>) obj;
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
    if (this.third == null) {
      if (other.getThird() != null) return false;
    } else {
      if (other.getThird() == null) return false;
      if (!this.third.equals(other.getThird())) return false;
    }
    return true;
  }

  /**
   * Canonical derived hash function
   */
  @Override
  public int hashCode() {
    // primitive hash function mixing the three integer hash values.
    // this number does supposedly not have any factors in common with 2^32
    final long prime = 2654435761L;
    long result = 1;
    result = prime * result + ((first == null) ? 0 : first.hashCode());
    result = prime * result + ((second == null) ? 0 : second.hashCode());
    result = prime * result + ((third == null) ? 0 : third.hashCode());
    return (int) result;
  }
}
