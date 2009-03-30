package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Pair storing two doubles.
 * 
 * Since double is a native type, this can't be done via the {@link ComparablePair} generic.
 * 
 * @author Erich Schubert
 */
public class DoubleDoublePair implements Comparable<DoubleDoublePair> {
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
   * 
   * @param obj Object to compare to
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
  public final int hashCode() {
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
   * 
   * @param other Object to compare to
   */
  public int compareTo(DoubleDoublePair other) {
    int fdiff = Double.compare(this.first, other.first);
    if (fdiff != 0) return fdiff;
    return Double.compare(this.second, other.second);
  }

  /**
   * Implementation of comparableSwapped interface, sorting by second then first.
   * 
   * @param other Object to compare to
   */
  public int compareSwappedTo(DoubleDoublePair other) {
    int fdiff = Double.compare(this.second, other.second);
    if (fdiff != 0) return fdiff;
    return Double.compare(this.first, other.first);
  }

  /**
   * Get first value
   * 
   * @return first value
   */
  public final double getFirst() {
    return first;
  }

  /**
   * Set first value
   * 
   * @param first new value
   */
  public final void setFirst(double first) {
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
}