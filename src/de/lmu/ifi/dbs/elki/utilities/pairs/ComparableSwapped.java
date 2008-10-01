package de.lmu.ifi.dbs.elki.utilities.pairs;

/**
 * Interface for Comparable pairs that compares by the second component first.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 *
 * @param <O> Object class.
 */
public interface ComparableSwapped<O> {
  public int compareSwappedTo(O other);
}
