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
   * @deprecated use pair.first to avoid boxing!
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

  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof DoubleObjPair)) {
      // TODO: allow comparison with arbitrary pairs?
      return false;
    }
    DoubleObjPair<?> other = (DoubleObjPair<?>) obj;
    if(first != other.first) {
      return false;
    }
    if(second == null) {
      return (other.second == null);
    }
    return second.equals(other.second);
  }

  /**
   * Canonical toString operator
   */
  @Override
  public String toString() {
    return "Pair(" + first + ", " + (second != null ? second.toString() : "null") + ")";
  }
}