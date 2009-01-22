package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Pair storing an integer and a double. For efficiency reasons this class is made final
 * and it thus is also sane to allow direct (public) access to the values.
 * 
 * Since double and int are native types, this can't be done via the {@link ComparablePair} generic.
 * 
 * @author Erich Schubert
 */
public final class IntDoublePair implements Comparable<IntDoublePair>, ComparableSwapped<IntDoublePair> {
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
   * @param first
   * @param second
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
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;

    IntDoublePair other = (IntDoublePair) obj;
    return (this.first == other.first) && (this.second == other.second);
  }

  /**
   * Trivial hashCode implementation mixing the two integers.
   */
  @Override
  public int hashCode() {
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
   */
  public int compareTo(IntDoublePair other) {
    int fdiff = this.first - other.first;
    if (fdiff != 0) return fdiff;
    return Double.compare(this.second, other.second);
  }

  /**
   * Implementation of comparableSwapped interface, sorting by second then first.
   * 
   * @param other Object to compare to
   */
  public int compareSwappedTo(IntDoublePair other) {
    int fdiff = Double.compare(this.second, other.second);
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
   * @param first new value
   */
  public void setFirst(int first) {
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
   * @param second new value
   */
  public void setSecond(double second) {
    this.second = second;
  }
}