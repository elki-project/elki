package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Pair storing an integer and a double. For efficiency reasons this class is made final
 * and it thus is also sane to allow direct (public) access to the values.
 * 
 * Since double and int are native types, this can't be done via the {@link ComparablePair} generic.
 * 
 * @author Erich Schubert
 */
public final class DoubleIntPair implements Comparable<DoubleIntPair>, ComparableSwapped<DoubleIntPair> {
  /**
   * first value
   */
  public double first;
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
  public DoubleIntPair(double first, int second) {
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

    DoubleIntPair other = (DoubleIntPair) obj;
    return (this.first == other.first) && (this.second == other.second);
  }

  /**
   * Trivial hashCode implementation mixing the two integers.
   */
  @Override
  public int hashCode() {
    long firsthash = Double.doubleToLongBits(first);
    firsthash = firsthash ^ (firsthash >> 32); 
    // primitive hash function mixing the two integers.
    // this number does supposedly not have any factors in common with 2^32
    return (int) (firsthash * 2654435761L + second);
  }

  /**
   * Implementation of comparable interface, sorting by first then second.
   */
  public int compareTo(DoubleIntPair other) {
    int fdiff = Double.compare(this.first, other.first);
    if (fdiff != 0) return fdiff;
    return this.second - other.second;
  }

  /**
   * Implementation of comparableSwapped interface, sorting by second then first.
   */
  public int compareSwappedTo(DoubleIntPair other) {
    int fdiff = this.second - other.second;
    if (fdiff != 0) return fdiff;
    return Double.compare(this.second, other.second);
  }

  /**
   * Get first value
   * 
   * @return first value
   */
  public double getFirst() {
    return first;
  }

  /**
   * Set first value
   * 
   * @param second
   */
  public void setFirst(double first) {
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