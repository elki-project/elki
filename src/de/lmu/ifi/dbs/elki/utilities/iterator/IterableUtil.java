package de.lmu.ifi.dbs.elki.utilities.iterator;

import java.util.Iterator;

/**
 * Utility function to wrap an Iterator as iterable.
 * 
 * @author Erich Schubert
 */
public final class IterableUtil {
  /**
   * Wrap an iterator as Iterable.
   * 
   * @param <C> Type restriction
   * @param iter Iterator
   * @return Iterable wrapper
   */
  public static <C> IterableIterator<C> fromIterator(Iterator<C> iter) {
    return new IterableIteratorAdapter<C>(iter);
  }

  /**
   * Wrap an Iterable as IterableIterator
   * 
   * @param <C> Type restriction
   * @param iter Iterator
   * @return Iterable wrapper
   */
  public static <C> IterableIterator<C> fromIterable(Iterable<C> iter) {
    return new IterableIteratorAdapter<C>(iter);
  }
}
