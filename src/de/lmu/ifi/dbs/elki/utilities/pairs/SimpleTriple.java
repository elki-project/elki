package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Triple without comparison
 * 
 * @author Erich Schubert
 *
 * @param <FIRST> first type
 * @param <SECOND> second type
 * @param <THIRD> second type
 */
public final class SimpleTriple<FIRST,SECOND,THIRD> implements TripleInterface<FIRST, SECOND, THIRD> {
  /* these are public by intention, Pair<> is supposed to be a simple wrapper and *final* */
  /**
   * First value
   */
  public FIRST first;
  /**
   * Second value
   */
  public SECOND second;
  /**
   * Third value
   */
  public THIRD third;

  /**
   * Constructor with fields
   * 
   * @param first Value of first component
   * @param second Value of second component
   * @param third Value of third component
   */
  public SimpleTriple(FIRST first, SECOND second, THIRD third) {
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
   * @return first element in triple
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
   * Getter for second element in triple
   * 
   * @return second element in triple
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
   * @return third element in triple
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
   * Array constructor for generics
   * 
   * @param size Size of array to be constructed.
   */
  @SuppressWarnings("unchecked")
  public static final <F,S,T> SimpleTriple<F,S,T>[] newArray(int size) {
    return new SimpleTriple[size];
  }

  /**
   * Canonical equals function
   * 
   * @param obj Object to compare to
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (!(obj instanceof SimpleTriple)) return false;
    SimpleTriple<FIRST,SECOND,THIRD> other = (SimpleTriple<FIRST,SECOND,THIRD>) obj;
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
