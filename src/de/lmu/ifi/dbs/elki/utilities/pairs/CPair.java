package de.lmu.ifi.dbs.elki.utilities.pairs;

import java.util.Comparator;

/**
 * Pair with canonical comparison function derived from generics.
 * 
 * @author Erich Schubert
 * 
 * @param <FIRST> first type
 * @param <SECOND> second type
 */
public class CPair<FIRST extends Comparable<FIRST>, SECOND extends Comparable<SECOND>> extends Pair<FIRST, SECOND> implements Comparable<CPair<FIRST,SECOND>> {
  /**
   * Initialize pair
   * 
   * @param first first parameter
   * @param second second parameter
   */
  public CPair(FIRST first, SECOND second) {
    super(first, second);
  }

  /**
   * Generic derived compare function.
   * 
   * @param other Object to compare to
   * @return comparison result
   */
  public int compareTo(CPair<FIRST, SECOND> other) {
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
    // try comparing by second
    if(this.second != null) {
      if(other.second == null) {
        return -1;
      }
      int delta2 = this.second.compareTo(other.second);
      if(delta2 != 0) {
        return delta2;
      }
    }
    else if(other.second != null) {
      return +1;
    }
    return 0;
  }

  /**
   * Generic derived compare function, with swapped components.
   * 
   * @param other Object to compare to
   * @return comparison result
   */
  public int compareSwappedTo(CPair<FIRST, SECOND> other) {
    // try comparing by second
    if(this.second != null) {
      if(other.second == null) {
        return -1;
      }
      int delta2 = this.second.compareTo(other.second);
      if(delta2 != 0) {
        return delta2;
      }
    }
    else if(other.second != null) {
      return +1;
    }
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
  public static final <F extends Comparable<F>, S extends Comparable<S>> CPair<F, S>[] newArray(int size) {
    return new CPair[size];
  }

  /**
   * Class to do a canonical swapped comparison on this class.
   * 
   * @param <FIRST>
   * @param <SECOND>
   */
  public final static class CompareSwapped<FIRST extends Comparable<FIRST>, SECOND extends Comparable<SECOND>> implements Comparator<CPair<FIRST, SECOND>> {
    /**
     * Compare by second component, using the ComparableSwapped interface.
     * 
     * @param o1 First object
     * @param o2 Second object
     */
    @Override
    public int compare(CPair<FIRST, SECOND> o1, CPair<FIRST, SECOND> o2) {
      return o1.compareSwappedTo(o2);
    }

  }
}
