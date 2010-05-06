package de.lmu.ifi.dbs.elki.utilities.datastructures;

import java.util.Iterator;

/**
 * Empty iterator, that never returns any data.
 * 
 * @author Erich Schubert
 *
 * @param <T> Data type
 */
public final class EmptyIterator<T> implements Iterator<T> {
  @Override
  public boolean hasNext() {
    return false;
  }

  @Override
  public T next() {
    return null;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Static instance
   */
  protected final static EmptyIterator<?> STATIC_INSTANCE = new EmptyIterator<Object>();
  
  /**
   * Access the static instance.
   * 
   * @param <T> type to (not) iterate over
   * @return Cast static instance. 
   */
  @SuppressWarnings("unchecked")
  public static <T> Iterator<T> STATIC() {
    return (Iterator<T>) STATIC_INSTANCE;
  }
}
