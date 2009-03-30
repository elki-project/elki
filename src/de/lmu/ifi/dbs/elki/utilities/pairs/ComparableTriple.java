package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Triple with canonical comparison function.
 * 
 * @author Erich Schubert
 *
 * @param <FIRST> first type
 * @param <SECOND> second type
 * @param <THIRD> second type
 */
public final class ComparableTriple<FIRST extends Comparable<FIRST>,SECOND extends Comparable<SECOND>, THIRD extends Comparable<THIRD>> extends SimpleTriple<FIRST, SECOND, THIRD> implements Comparable<ComparableTriple<FIRST,SECOND,THIRD>> {
  /**
   * Constructor with fields
   * 
   * @param first Value of first component
   * @param second Value of second component
   * @param third Value of third component
   */
  public ComparableTriple(FIRST first, SECOND second, THIRD third) {
    super(first, second, third);
  }

  /**
   * Canonical toString operator
   */
  @Override
  public String toString() {
    return "Triple(" + first.toString() + ", " + second.toString() + ", " + third.toString() + ")";
  }

  /**
   * Generic derived compare function.
   * 
   * @param other Object to compare to
   */
  public int compareTo(ComparableTriple<FIRST, SECOND, THIRD> other) {
    // try comparing by first
    if (this.first != null) {
      if (other.first == null) return -1;
      int delta1 = this.first.compareTo(other.first);
      if (delta1 != 0) return delta1;
    } else
      if (other.first != null) return +1;
    // try comparing by second
    if (this.second != null) {
      if (other.second == null) return -1;
      int delta2 = this.second.compareTo(other.second);
      if (delta2 != 0) return delta2;
    } else
      if (other.second != null) return +1;
    // try comparing by third
    if (this.third != null) {
      if (other.third == null) return -1;
      int delta3 = this.third.compareTo(other.third);
      if (delta3 != 0) return delta3;
    } else
      if (other.third != null) return +1;
    return 0;
  }
  
  /**
   * Array constructor for generics
   * 
   * @param size Size of array to be constructed.
   */
  @SuppressWarnings("unchecked")
  public static final <F extends Comparable<F>,S extends Comparable<S>, T extends Comparable<T>> ComparableTriple<F,S,T>[] newArray(int size) {
    return new ComparableTriple[size];
  }
}
