package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Triple without comparison.
 * 
 * See also {@link CTriple}
 * 
 * @author Erich Schubert
 * 
 * @param <FIRST> first type
 * @param <SECOND> second type
 * @param <THIRD> second type
 */
public class Triple<FIRST, SECOND, THIRD> {
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
  public Triple(FIRST first, SECOND second, THIRD third) {
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
   * Getter for second element in triple
   * 
   * @return second element in triple
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
   * Getter for third
   * 
   * @return third element in triple
   */
  public final THIRD getThird() {
    return third;
  }

  /**
   * Setter for third
   * 
   * @param third new value for third element
   */
  public final void setThird(THIRD third) {
    this.third = third;
  }

  /**
   * Array constructor for generics
   * @param <F> First type
   * @param <S> Second type
   * @param <T> Third type
   * 
   * @param size Size of array to be constructed.
   * @return new array of the requested size.
   */
  @SuppressWarnings("unchecked")
  public static final <F, S, T> Triple<F, S, T>[] newArray(int size) {
    return new Triple[size];
  }

  /**
   * Canonical equals function
   * 
   * @param obj Object to compare to
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(!(obj instanceof Triple)) {
      return false;
    }
    Triple<FIRST, SECOND, THIRD> other = (Triple<FIRST, SECOND, THIRD>) obj;
    if(this.first == null) {
      if(other.getFirst() != null) {
        return false;
      }
    }
    else {
      if(other.getFirst() == null) {
        return false;
      }
      if(!this.first.equals(other.getFirst())) {
        return false;
      }
    }
    if(this.second == null) {
      if(other.getSecond() != null) {
        return false;
      }
    }
    else {
      if(other.getSecond() == null) {
        return false;
      }
      if(!this.second.equals(other.getSecond())) {
        return false;
      }
    }
    if(this.third == null) {
      if(other.getThird() != null) {
        return false;
      }
    }
    else {
      if(other.getThird() == null) {
        return false;
      }
      if(!this.third.equals(other.getThird())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Canonical derived hash function
   */
  @Override
  public final int hashCode() {
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
