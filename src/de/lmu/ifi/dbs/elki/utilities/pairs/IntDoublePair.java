package de.lmu.ifi.dbs.elki.utilities.pairs;

import java.util.Comparator;

/**
 * Pair storing an integer and a double.
 * 
 * Since double and int are native types, this can't be done via the
 * {@link CPair} generic.
 * 
 * @author Erich Schubert
 */
public class IntDoublePair implements Comparable<IntDoublePair> {
  /**
   * first value
   */
  public int first;

  /**
   * second value
   */
  public double second;

  /**
   * Constructor
   * 
   * @param first First value
   * @param second Second value
   */
  public IntDoublePair(int first, double second) {
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

    IntDoublePair other = (IntDoublePair) obj;
    return (this.first == other.first) && (this.second == other.second);
  }

  /**
   * Trivial hashCode implementation mixing the two integers.
   */
  @Override
  public final int hashCode() {
    long secondhash = Double.doubleToLongBits(second);
    secondhash = secondhash ^ (secondhash >> 32);
    // primitive hash function mixing the two integers.
    // this number does supposedly not have any factors in common with 2^32
    return (int) (first * 2654435761L + secondhash);
  }

  /**
   * Implementation of comparable interface, sorting by first then second.
   * 
   * @param other Object to compare to
   * @return comparison result
   */
  public int compareTo(IntDoublePair other) {
    int fdiff = this.first - other.first;
    if(fdiff != 0) {
      return fdiff;
    }
    return Double.compare(this.second, other.second);
  }

  /**
   * Implementation of comparableSwapped interface, sorting by second then
   * first.
   * 
   * @param other Object to compare to
   * @return comparison result
   */
  public int compareSwappedTo(IntDoublePair other) {
    int fdiff = Double.compare(this.second, other.second);
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
  public final double getSecond() {
    return second;
  }

  /**
   * Set second value
   * 
   * @param second new value
   */
  public final void setSecond(double second) {
    this.second = second;
  }

  /**
   * Returns first and second value.
   */
  @Override
  public String toString() {
    return "(" + first + "," + second + ")";
  }

  /**
   * Comparator to compare by second component only
   */
  public static final Comparator<IntDoublePair> BYFIRST_COMPARATOR = new Comparator<IntDoublePair>() {
    @Override
    public int compare(IntDoublePair o1, IntDoublePair o2) {
      return o1.first - o2.first;
    }
  };

  /**
   * Comparator to compare by second component only
   */
  public static final Comparator<IntDoublePair> BYSECOND_COMPARATOR = new Comparator<IntDoublePair>() {
    @Override
    public int compare(IntDoublePair o1, IntDoublePair o2) {
      return Double.compare(o1.second, o2.second);
    }
  };

  /**
   * Comparator to compare by swapped components
   */
  public static final Comparator<IntDoublePair> SWAPPED_COMPARATOR = new Comparator<IntDoublePair>() {
    @Override
    public int compare(IntDoublePair o1, IntDoublePair o2) {
      return o1.compareSwappedTo(o2);
    }
  };
}