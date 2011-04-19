package de.lmu.ifi.dbs.elki.utilities.iterator;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract class to build filtered views on Iterables.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype decorator
 * @apiviz.composedOf Iterator
 * 
 * @param <IN> Input type
 * @param <OUT> Output type
 */
public abstract class AbstractFilteredIterator<IN, OUT extends IN> implements Iterator<OUT> {
  /**
   * The iterator to use.
   */
  Iterator<IN> itr = null;

  /**
   * The next object to return.
   */
  OUT nextobj = null;

  /**
   * Constructor.
   */
  public AbstractFilteredIterator() {
    super();
  }

  /**
   * Init the iterators
   */
  protected void init() {
    this.itr = getParentIterator();
    if (this.itr == null) {
      throw new AbortException("Filtered iterator has 'null' parent.");
    }
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
    if(itr == null) {
      init();
    }
    nextobj = null;
    while(itr.hasNext()) {
      IN v = itr.next();
      nextobj = testFilter(v);
      if(nextobj != null) {
        break;
      }
    }
  }

  @Override
  public boolean hasNext() {
    if(itr == null) {
      updateNext();
    }
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