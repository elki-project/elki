package de.lmu.ifi.dbs.elki.utilities.pairs;

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Pair with canonical comparison function derived from the components comparable interfaces.
 * 
 * @author Erich Schubert
 * 
 * @param <FIRST> first type
 * @param <SECOND> second type
 */
public class CPair<FIRST extends Comparable<? super FIRST>, SECOND extends Comparable<? super SECOND>> extends Pair<FIRST, SECOND> implements Comparable<CPair<FIRST,SECOND>> {
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
   * Array constructor for generics
   * 
   * @param <F> First type
   * @param <S> Second type
   * @param size Size of array to be constructed
   * @return New array of requested size
   */
  public static final <F extends Comparable<? super F>, S extends Comparable<? super S>> CPair<F, S>[] newArray(int size) {
    Class<CPair<F,S>> paircls = ClassGenericsUtil.uglyCastIntoSubclass(CPair.class);    
    return ClassGenericsUtil.newArrayOfNull(size, paircls);
  }
}