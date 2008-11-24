package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Interface for Comparable pairs that compares by the second component first.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object class.
 */
public interface ComparableSwapped<O> {
  public int compareSwappedTo(O other);
}
