package de.lmu.ifi.dbs.elki.utilities.iterator;

import java.util.Iterator;

/**
 * Dummy iterator/iterable that returns a single object, once.
 * 
 * Note: a "null" object is not supported.
 * 
 * @author Erich Schubert
 *
 * @param <T> Object type to return
 */
public class OneItemIterator<T> implements IterableIterator<T> {
  /**
   * Object to return.
   */
  private T object = null;
  
  /**
   * Constructor.
   * 
   * @param object Object to return once.
   */
  public OneItemIterator(T object) {
    super();
    this.object = object;
  }

  @Override
  public boolean hasNext() {
    return (object != null);
  }

  @Override
  public T next() {
    T ret = object;
    object = null;
    return ret;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<T> iterator() {
    return this;
  }
}