package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Pair storing a native double value and an arbitrary object.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class DoubleObjPair<O> implements PairInterface<Double, O>, Comparable<DoubleObjPair<O>> {
  /**
   * Double value
   */
  public double first;

  /**
   * Second object value
   */
  public O second;

  /**
   * Constructor.
   *
   * @param first First value
   * @param second Second value
   */
  public DoubleObjPair(double first, O second) {
    this.first = first;
    this.second = second;
  }

  /**
   * @deprecation use pair.first to avoid boxing!
   */
  @Override
  @Deprecated
  public Double getFirst() {
    return first;
  }

  @Override
  public O getSecond() {
    return second;
  }

  @Override
  public int compareTo(DoubleObjPair<O> o) {
    return Double.compare(first, o.first);
  }
}