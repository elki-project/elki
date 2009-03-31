package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Pair storing an integer and a double.
 * 
 * Since double and int are native types, this can't be done via the {@link CPair} generic.
 * 
 * @author Erich Schubert
 */
public class DoubleIntPair implements Comparable<DoubleIntPair> {
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
   * 
   * @param obj Object to compare to
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
  public final int hashCode() {
    long firsthash = Double.doubleToLongBits(first);
    firsthash = firsthash ^ (firsthash >> 32); 
    // primitive hash function mixing the two integers.
    // this number does supposedly not have any factors in common with 2^32
    return (int) (firsthash * 2654435761L + second);
  }

  /**
   * Implementation of comparable interface, sorting by first then second.
   * 
   * @param other Object to compare to
   */
  public int compareTo(DoubleIntPair other) {
    int fdiff = Double.compare(this.first, other.first);
    if (fdiff != 0) return fdiff;
    return this.second - other.second;
  }

  /**
   * Implementation of comparableSwapped interface, sorting by second then first.
   * 
   * @param other Object to compare to
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
}