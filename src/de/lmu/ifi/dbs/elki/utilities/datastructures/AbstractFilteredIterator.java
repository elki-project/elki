package de.lmu.ifi.dbs.elki.utilities.datastructures;

import java.util.Iterator;

/**
 * Abstract class to build filtered views on Iterables.
 * 
 * @author Erich Schubert
 *
 * @param <T>
 */
public abstract class AbstractFilteredIterator<T> implements Iterator<T> {
  /**
   * The iterator to use.
   */
  Iterator<T> itr = null;

  /**
   * The next visualizer to return.
   */
  T nextobj = null;

  /**
   * Constructor.
   */
  public AbstractFilteredIterator() {
    super();
    this.itr = getParentIterator();
    updateNext();
  }

  /**
   * Get an iterator for the actual data. Used in initialization.
   * 
   * @return iterator
   */
  abstract protected Iterator<T> getParentIterator();

  /**
   * Test the filter predicate for a new object.
   * 
   * @param nextobj Object to test
   * @return true if the object is accepted.
   */
  abstract protected boolean testFilter(T nextobj);
  
  /**
   * Find the next visualizer.
   */
  private void updateNext() {
    nextobj = null;
    while(itr.hasNext()) {
      T v = itr.next();
      if(testFilter(nextobj)) {
        nextobj = v;
        return;
      }
    }
  }

  @Override
  public boolean hasNext() {
    return (nextobj != null);
  }

  @Override
  public T next() {
    T ret = this.nextobj;
    updateNext();
    return ret;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}