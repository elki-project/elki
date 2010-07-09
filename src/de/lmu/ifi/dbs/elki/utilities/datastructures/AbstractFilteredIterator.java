package de.lmu.ifi.dbs.elki.utilities.datastructures;

import java.util.Iterator;

/**
 * Abstract class to build filtered views on Iterables.
 * 
 * @author Erich Schubert
 *
 * @param <T>
 */
public abstract class AbstractFilteredIterator<IN,OUT extends IN> implements Iterator<OUT> {
  /**
   * The iterator to use.
   */
  Iterator<IN> itr = null;

  /**
   * The next visualizer to return.
   */
  OUT nextobj = null;

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
  abstract protected Iterator<IN> getParentIterator();

  /**
   * Test the filter predicate for a new object.
   * 
   * @param nextobj Object to test
   * @return cast object when true, {@code null} otherwise
   */
  abstract protected OUT testFilter(IN nextobj);
  
  /**
   * Find the next visualizer.
   */
  private void updateNext() {
    nextobj = null;
    while(itr.hasNext()) {
      IN v = itr.next();
      nextobj = testFilter(v);
      if(nextobj != null) {
        return;
      }
    }
  }

  @Override
  public boolean hasNext() {
    return (nextobj != null);
  }

  @Override
  public OUT next() {
    OUT ret = this.nextobj;
    updateNext();
    return ret;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}