package de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike;

import java.util.List;

/**
 * Static adapter class to use a {@link java.util.List} in an array API.
 * 
 * Use the static instance from {@link ArrayLikeUtil}!
 * 
 * @author Erich Schubert
 * 
 * @param <T> Data object type.
 */
public class ListArrayAdapter<T> implements ArrayAdapter<T, List<? extends T>> {
  /**
   * Constructor.
   *
   * Use the static instance from {@link ArrayLikeUtil}!
   */
  protected ListArrayAdapter() {
    super();
  }

  @Override
  public int size(List<? extends T> array) {
    return array.size();
  }

  @Override
  public T get(List<? extends T> array, int off) throws IndexOutOfBoundsException {
    return array.get(off);
  }
}