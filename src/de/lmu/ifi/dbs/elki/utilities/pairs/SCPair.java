package de.lmu.ifi.dbs.elki.utilities.pairs;

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Pair that can <em>only</em> be compared by it's second component.
 * 
 * @author Erich Schubert
 * 
 * @param <FIRST> first type (comparable)
 * @param <SECOND> second type
 */
public class SCPair<FIRST, SECOND extends Comparable<SECOND>> extends Pair<FIRST, SECOND> implements Comparable<SCPair<FIRST, SECOND>> {
  /**
   * Initialize pair
   * 
   * @param first first parameter
   * @param second second parameter
   */
  public SCPair(FIRST first, SECOND second) {
    super(first, second);
  }

  /**
   * Generic derived compare function.
   * 
   * @param other Object to compare to
   * @return comparison result
   */
  public int compareTo(SCPair<FIRST, SECOND> other) {
    // try comparing by first
    if(this.second != null) {
      if(other.second == null) {
        return -1;
      }
      int delta1 = this.second.compareTo(other.second);
      if(delta1 != 0) {
        return delta1;
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
  public static final <F, S extends Comparable<S>> SCPair<F, S>[] newArray(int size) {
    Class<SCPair<F,S>> paircls = ClassGenericsUtil.uglyCastIntoSubclass(SCPair.class);    
    return ClassGenericsUtil.newArrayOfNull(size, paircls);
  }
}