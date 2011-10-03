package de.lmu.ifi.dbs.elki.utilities.datastructures;

import java.util.List;

/**
 * Utility class that allows plug-in use of various "array-like" types such as
 * lists in APIs that can take any kind of array to safe the cost of
 * reorganizing the objects into a real array.
 * 
 * @author Erich Schubert
 */
public final class ArrayUtil {
  /**
   * Static instance for lists
   */
  private static final ListArrayAdapter<Object> LISTADAPTER = new ListArrayAdapter<Object>();

  /**
   * Static instance for lists of numbers
   */
  private static final NumberListArrayAdapter<Number> NUMBERLISTADAPTER = new NumberListArrayAdapter<Number>();

  /**
   * Cast the static instance.
   * 
   * @param dummy Dummy variable, for type inference
   * @return Static instance
   */
  @SuppressWarnings("unchecked")
  public static <T> ArrayAdapter<T, List<? extends T>> listAdapter(List<? extends T> dummy) {
    return (ListArrayAdapter<T>) LISTADAPTER;
  }

  /**
   * Cast the static instance.
   * 
   * @param dummy Dummy variable, for type inference
   * @return Static instance
   */
  @SuppressWarnings("unchecked")
  public static <T extends Number> NumberArrayAdapter<T, List<? extends T>> numberListAdapter(List<? extends T> dummy) {
    return (NumberListArrayAdapter<T>) NUMBERLISTADAPTER;
  }

  /**
   * Static adapter class to use a {@link java.util.List} in an array API.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Data object type.
   */
  public static class ListArrayAdapter<T> implements ArrayAdapter<T, List<? extends T>> {
    @Override
    public int size(List<? extends T> array) {
      return array.size();
    }

    @Override
    public T get(List<? extends T> array, int off) throws IndexOutOfBoundsException {
      return array.get(off);
    }
  }

  /**
   * Static adapter class to use a {@link java.util.List} in an array of number
   * API.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Data object type.
   */
  public static class NumberListArrayAdapter<T extends Number> implements NumberArrayAdapter<T, List<? extends T>> {
    @Override
    public int size(List<? extends T> array) {
      return array.size();
    }

    @Override
    public T get(List<? extends T> array, int off) throws IndexOutOfBoundsException {
      return array.get(off);
    }

    @Override
    public double getDouble(List<? extends T> array, int off) throws IndexOutOfBoundsException {
      return array.get(off).doubleValue();
    }

    @Override
    public float getFloat(List<? extends T> array, int off) throws IndexOutOfBoundsException {
      return array.get(off).floatValue();
    }

    @Override
    public int getInteger(List<? extends T> array, int off) throws IndexOutOfBoundsException {
      return array.get(off).intValue();
    }

    @Override
    public short getShort(List<? extends T> array, int off) throws IndexOutOfBoundsException {
      return array.get(off).shortValue();
    }

    @Override
    public long getLong(List<? extends T> array, int off) throws IndexOutOfBoundsException {
      return array.get(off).longValue();
    }

    @Override
    public byte getByte(List<? extends T> array, int off) throws IndexOutOfBoundsException {
      return array.get(off).byteValue();
    }
  }
}