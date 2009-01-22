package de.lmu.ifi.dbs.elki.utilities.pairs;

import java.util.Comparator;

/**
 * Sort objects ascending by their second component.
 * 
 * @author Erich Schubert
 */
public final class CompareSwapped<P extends ComparableSwapped<P>> implements Comparator<P> {
  /**
   * Compare by second component, using the ComparableSwapped interface.
   * 
   * @param o1 First object
   * @param o2 Second object
   */
  @Override
  public int compare(P o1, P o2) {
    return o1.compareSwappedTo(o2);
  }

}
