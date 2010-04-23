package de.lmu.ifi.dbs.elki.utilities;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Iterator that joins multiple existing iterators into one.
 * 
 * @author Erich Schubert
 *
 * @param <E> Entry type
 */
public class MergedIterator<E> implements Iterator<E> {
  /**
   * All the iterators we process
   */
  final Iterator<Iterator<E>> iterators;
  
  /**
   * The iterator we are currently processing
   */
  Iterator<E> current = null;

  /**
   * The last iterator we returned an object for, for remove()
   */
  Iterator<E> last = null;
  
  /**
   * Main constructor.
   * 
   * @param iterators Iterators
   */
  public MergedIterator(Iterator<Iterator<E>> iterators) {
    super();
    this.iterators = iterators;
  }

  /**
   * Auxillary constructor with Collections
   * 
   * @param iterators Iterators
   */
  public MergedIterator(Collection<Iterator<E>> iterators) {
    this(iterators.iterator());
  }

  /**
   * Auxillary constructor with arrays
   * 
   * @param iterators Iterators
   */
  public MergedIterator(Iterator<E>... iterators) {
    this(Arrays.asList(iterators).iterator());
  }

  @Override
  public boolean hasNext() {
    while((current != null && current.hasNext()) || iterators.hasNext()) {
      // Next element in current iterator?
      if (current != null && current.hasNext()) {
        return true;
      }
      // advance master iterator and retry
      current = iterators.next();
    }
    return false;
  }

  @Override
  public E next() {
    while (!current.hasNext()) {
      current = iterators.next();
    }
    last = current;
    return current.next();
  }

  @Override
  public void remove() {
    if (last == null) {
      throw new RuntimeException("Iterator.remove() called without next()");
    }
    last.remove();
  }
}
