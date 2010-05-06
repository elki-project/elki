package de.lmu.ifi.dbs.elki.utilities.datastructures;

import java.util.ListIterator;

/**
 * Iterator proxy that does not allow modifications.
 * 
 * @author Erich Schubert
 * 
 * @param <T>
 */
public final class UnmodifiableListIterator<T> implements ListIterator<T> {
  /**
   * Real iterator
   */
  private ListIterator<T> inner;

  /**
   * Constructor.
   * 
   * @param inner Real iterator to proxy.
   */
  public UnmodifiableListIterator(ListIterator<T> inner) {
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
  public boolean hasPrevious() {
    return inner.hasPrevious();
  }

  @Override
  public T previous() {
    return inner.previous();
  }

  @Override
  public int nextIndex() {
    return inner.nextIndex();
  }

  @Override
  public int previousIndex() {
    return inner.previousIndex();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(@SuppressWarnings("unused") T e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(@SuppressWarnings("unused") T e) {
    throw new UnsupportedOperationException();
  }
}