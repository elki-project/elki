package de.lmu.ifi.dbs.elki.utilities.iterator;

import java.util.Iterator;

/**
 * Iterator proxy that does not allow modifications.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype decorator
 * @apiviz.composedOf Iterator oneway - - decorates
 * 
 * @param <T>
 */
public final class UnmodifiableIterator<T> implements Iterator<T> {
  /**
   * Real iterator
   */
  private Iterator<T> inner;

  /**
   * Constructor.
   * 
   * @param inner Real iterator to proxy.
   */
  public UnmodifiableIterator(Iterator<T> inner) {
    super();
    this.inner = inner;
  }

  @Override
  public boolean hasNext() {
    return inner.hasNext();
  }

  @Override
  public T next() {
    return inner.next();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}