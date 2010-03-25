package de.lmu.ifi.dbs.elki.utilities.pairs;

import java.util.Comparator;

/**
 * Pair storing two integers.
 * 
 * Since int is a native type, this can't be done via the {@link CPair} generic.
 * 
 * @author Erich Schubert
 */
public class IntIntPair implements Comparable<IntIntPair> {
  /**
   * first value
   */
  public int first;

  /**
   * second value
   */
  public int second;

  /**
   * Constructor
   * 
   * @param first First value
   * @param second Second value
   */
  public IntIntPair(int first, int second) {
    super();
    this.first = first;
    this.second = second;
  }

  /**
   * Trivial equals implementation
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
    if(getClass() != obj.getClass()) {
      return false;
    }

    IntIntPair other = (IntIntPair) obj;
    return (this.first == other.first) && (this.second == other.second);
  }

  /**
   * Trivial hashCode implementation mixing the two integers.
   */
  @Override
  public final int hashCode() {
    // primitive hash function mixing the two integers.
    // this number does supposedly not have any factors in common with 2^32
    return (int) (first * 2654435761L + second);
  }

  /**
   * Implementation of comparable interface, sorting by first then second.
   * 
   * @param other Object to compare to
   * @return comparison result
   */
  public int compareTo(IntIntPair other) {
    int fdiff = this.first - other.first;
    if(fdiff != 0) {
      return fdiff;
    }
    return this.second - other.second;
  }

  /**
   * Implementation of comparableSwapped interface, sorting by second then
   * first.
   * 
   * @param other Object to compare to
   * @return comparison result
   */
  public int compareSwappedTo(IntIntPair other) {
    int fdiff = this.second - other.second;
    if(fdiff != 0) {
      return fdiff;
    }
    return this.first - other.first;
  }

  /**
   * Get first value
   * 
   * @return first value
   */
  public final int getFirst() {
    return first;
  }

  /**
   * Set first value
   * 
   * @param first new value
   */
  public final void setFirst(int first) {
    this.first = first;
  }

  /**
   * Get second value
   * 
   * @return second value
   */
  public final int getSecond() {
    return second;
  }

  /**
   * Set second value
   * 
   * @param second new value
   */
  public final void setSecond(int second) {
    this.second = second;
  }
  
  /**
   * Comparator to compare by second component only 
   */
  public static final Comparator<IntIntPair> BYFIRST_COMPARATOR = new Comparator<IntIntPair>() {
    @Override
    public int compare(IntIntPair o1, IntIntPair o2) {
      return o1.first - o2.first;
    }
  };
  
  /**
   * Comparator to compare by second component only 
   */
  public static final Comparator<IntIntPair> BYSECOND_COMPARATOR = new Comparator<IntIntPair>() {
    @Override
    public int compare(IntIntPair o1, IntIntPair o2) {
      return o1.second - o2.second;
    }
  };
  
  /**
   * Comparator to compare by swapped components
   */
  public static final Comparator<IntIntPair> SWAPPED_COMPARATOR = new Comparator<IntIntPair>() {
    @Override
    public int compare(IntIntPair o1, IntIntPair o2) {
      return o1.compareSwappedTo(o2);
    }
  };
}