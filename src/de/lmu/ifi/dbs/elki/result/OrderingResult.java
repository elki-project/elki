package de.lmu.ifi.dbs.elki.result;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.utilities.IterableIterator;

/**
 * Interface for a result providing an object ordering.
 * 
 * @author Erich Schubert
 */
public interface OrderingResult extends Result {
  /**
   * Sort the given ids according to this ordering and return an iterator.
   * 
   * @param ids Collection of ids.
   * @return iterator for sorted array of ids
   */
  public IterableIterator<Integer> iter(Collection<Integer> ids);
}
