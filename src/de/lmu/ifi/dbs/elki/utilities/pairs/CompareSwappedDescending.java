package de.lmu.ifi.dbs.elki.utilities.pairs;

import java.util.Comparator;

/**
 * Compare two objects descending by their second component.
 * 
 * @author Erich Schubert
 */
public final class CompareSwappedDescending<P extends ComparableSwapped<P>> implements Comparator<P> {
  /**
   * Compare by second component, using the ComparableSwapped interface.
   * 
   * Sort descending by comparing o2 with o1 instead of the other way round.
   */
  @Override
  public int compare(P o1, P o2) {
    return o2.compareSwappedTo(o1);
  }

}
