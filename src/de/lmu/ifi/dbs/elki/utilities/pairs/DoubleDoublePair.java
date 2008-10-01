package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Pair storing two doubles. For efficiency reasons this class is made final
 * and it thus is also sane to allow direct (public) access to the values.
 * 
 * Since double is a native type, this can't be done via the {@link ComparablePair} generic.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public final class DoubleDoublePair implements Comparable<DoubleDoublePair> {
  /**
   * first value
   */
  public double first;
  /**
   * second value
   */
  public double second;

  /**
   * Constructor
   * 
   * @param first
   * @param second
   */
  public DoubleDoublePair(double first, double second) {
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

    DoubleDoublePair other = (DoubleDoublePair) obj;
    return (this.first == other.first) && (this.second == other.second);
  }

  /**
   * Trivial hashCode implementation mixing the two integers.
   */
  @Override
  public int hashCode() {
    // convert to longs
    long firsthash = Double.doubleToLongBits(first);
    firsthash = firsthash ^ (firsthash >> 32); 
    long secondhash = Double.doubleToLongBits(second);
    secondhash = secondhash ^ (secondhash >> 32); 
    // primitive hash function mixing the two integers.
    // this number does supposedly not have any factors in common with 2^32
    return (int) (firsthash * 2654435761L + secondhash);
  }

  /**
   * Implementation of comparable interface, sorting by first then second.
   */
  public int compareTo(DoubleDoublePair other) {
    int fdiff = Double.compare(this.first, other.first);
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
  public double getSecond() {
    return second;
  }

  /**
   * Set second value
   * 
   * @param second
   */
  public void setSecond(double second) {
    this.second = second;
  }
}