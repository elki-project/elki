package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Interface for Comparable pairs that compares by the second component first.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object class.
 */
public interface ComparableSwapped<O> {
  /**
   * Swapped comparison function.
   * 
   * @param other object to compare to
   * @return comparison result.
   */
  public int compareSwappedTo(O other);
}
