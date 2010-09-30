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
public interface IterableResult<O> extends AnyResult, Iterable<O> {
  /**
   * Retrieve an iterator for the result.
   * 
   * @return iterator
   */
  @Override
  public Iterator<O> iterator();
}