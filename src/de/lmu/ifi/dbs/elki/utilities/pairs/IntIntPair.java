package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Pair storing two integers. For efficiency reasons this class is made final
 * and it thus is also sane to allow direct (public) access to the values.
 * 
 * Since int is a native type, this can't be done via the {@link ComparablePair} generic.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public final class IntIntPair implements Comparable<IntIntPair>, ComparableSwapped<IntIntPair> {
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
   * @param first
   * @param second
   */
  public IntIntPair(int first, int second) {
    super();
    this.first = first;
    this.second = second;
  }

  /**
   * Trivial equals implementation
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;

    IntIntPair other = (IntIntPair) obj;
    return (this.first == other.first) && (this.second == other.second);
  }

  /**
   * Trivial hashCode implementation mixing the two integers.
   */
  @Override
  public int hashCode() {
    // primitive hash function mixing the two integers.
    // this number does supposedly not have any factors in common with 2^32
    return (int) (first * 2654435761L + second);
  }

  /**
   * Implementation of comparable interface, sorting by first then second.
   */
  public int compareTo(IntIntPair other) {
    int fdiff = this.first - other.first;
    if (fdiff != 0) return fdiff;
    return this.second - other.second;
  }

  /**
   * Implementation of comparableSwapped interface, sorting by second then first.
   */
  public int compareSwappedTo(IntIntPair other) {
    int fdiff = this.second - other.second;
    if (fdiff != 0) return fdiff;
    return this.first - other.first;
  }

  /**
   * Get first value
   * 
   * @return first value
   */
  public int getFirst() {
    return first;
  }

  /**
   * Set first value
   * 
   * @param second
   */
  public void setFirst(int first) {
    this.first = first;
  }

  /**
   * Get second value
   * 
   * @return second value
   */
  public int getSecond() {
    return second;
  }

  /**
   * Set second value
   * 
   * @param second
   */
  public void setSecond(int second) {
    this.second = second;
  }
}