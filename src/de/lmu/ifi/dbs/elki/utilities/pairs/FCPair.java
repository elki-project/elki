package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Pair that can only be compared by it's first component.
 * 
 * Note: this cannot be a superclass of {@link CPair}, because of the Comparable
 * interface, which cannot be implemented with different generics.
 * 
 * @author Erich Schubert
 * 
 * @param <FIRST> first type (comparable)
 * @param <SECOND> second type
 */
public class FCPair<FIRST extends Comparable<FIRST>, SECOND> extends Pair<FIRST, SECOND> implements Comparable<FCPair<FIRST, SECOND>> {
  /**
   * Initialize pair
   * 
   * @param first first parameter
   * @param second second parameter
   */
  public FCPair(FIRST first, SECOND second) {
    super(first, second);
  }

  /**
   * Generic derived compare function.
   * 
   * @param other Object to compare to
   * @return comparison result
   */
  public int compareTo(FCPair<FIRST, SECOND> other) {
    // try comparing by first
    if(this.first != null) {
      if(other.first == null) {
        return -1;
      }
      int delta1 = this.first.compareTo(other.first);
      if(delta1 != 0) {
        return delta1;
      }
    }
    else if(other.first != null) {
      return +1;
    }
    return 0;
  }

  /**
   * Array constructor for generics
   * 
   * @param <F> First type
   * @param <S> Second type
   * @param size Size of array to be constructed
   * @return New array of requested size
   */
  @SuppressWarnings("unchecked")
  public static final <F extends Comparable<F>, S> FCPair<F, S>[] newArray(int size) {
    return new FCPair[size];
  }
}