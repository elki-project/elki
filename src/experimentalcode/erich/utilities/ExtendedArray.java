package experimentalcode.erich.utilities;

import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayAdapter;

/**
 * Class to extend an array with a single element virtually.
 * 
 * @author Erich Schubert
 * 
 * @param <T> Object type
 * @param <A> Array-like type
 */
public class ExtendedArray<T> implements ArrayAdapter<T, ExtendedArray<T>> {
  /**
   * The array
   */
  final Object array;

  /**
   * The array adapter
   */
  final ArrayAdapter<T, Object> getter;

  /**
   * The extra element
   */
  final T extra;

  /**
   * Our size
   */
  final int size;

  /**
   * Constructor.
   * 
   * @param array Original array
   * @param getter Adapter for array
   * @param extra Extra element
   */
  protected ExtendedArray(Object array, ArrayAdapter<T, Object> getter, T extra) {
    super();
    this.array = array;
    this.getter = getter;
    this.extra = extra;
    this.size = getter.size(array) + 1;
  }

  @Override
  public int size(ExtendedArray<T> array) {
    assert (array == this);
    return size;
  }

  @Override
  public T get(ExtendedArray<T> array, int off) throws IndexOutOfBoundsException {
    assert (array == this);
    if(off == size - 1) {
      return extra;
    }
    return getter.get(this.array, off);
  }

  /**
   * Static wrapper that has a nicer generics signature.
   * 
   * @param array Array to extend
   * @param getter Getter for array
   * @param extra Extra element
   * @return Extended array
   */
  @SuppressWarnings("unchecked")
  public static <T, A> ExtendedArray<T> extend(A array, ArrayAdapter<T, A> getter, T extra) {
    return new ExtendedArray<T>(array, (ArrayAdapter<T, Object>) getter, extra);
  }
}