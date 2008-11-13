package de.lmu.ifi.dbs.elki.result;

import java.util.Iterator;

/**
 * Interface of an "iterable" result (e.g. a list, table) that can be printed one-by-one.
 * (At least when the class O is TextWriteable)
 * 
 * @author Erich Schubert
 *
 * @param <O> object class.
 */
public interface IterableResult<O> extends Result {
  /**
   * Retrieve an iterator for the result.
   * 
   * @return iterator
   */
  public Iterator<O> iter();
}
