package de.lmu.ifi.dbs.elki.utilities.datastructures;

public interface NumberArrayAdapter<N extends Number, A> extends ArrayAdapter<N, A> {
  @Override
  public int size(A array);

  @Override
  public N get(A array, int off) throws IndexOutOfBoundsException;

  /**
   * Get the off'th item from the array as double.
   * 
   * @param array Array to get from
   * @param off Offset
   * @return Item at offset off
   * @throws IndexOutOfBoundsException for an invalid index.
   */
  public double getDouble(A array, int off) throws IndexOutOfBoundsException;

  /**
   * Get the off'th item from the array as float.
   * 
   * @param array Array to get from
   * @param off Offset
   * @return Item at offset off
   * @throws IndexOutOfBoundsException for an invalid index.
   */
  public float getFloat(A array, int off) throws IndexOutOfBoundsException;

  /**
   * Get the off'th item from the array as integer.
   * 
   * @param array Array to get from
   * @param off Offset
   * @return Item at offset off
   * @throws IndexOutOfBoundsException for an invalid index.
   */
  public int getInteger(A array, int off) throws IndexOutOfBoundsException;

  /**
   * Get the off'th item from the array as short.
   * 
   * @param array Array to get from
   * @param off Offset
   * @return Item at offset off
   * @throws IndexOutOfBoundsException for an invalid index.
   */
  public short getShort(A array, int off) throws IndexOutOfBoundsException;

  /**
   * Get the off'th item from the array as long.
   * 
   * @param array Array to get from
   * @param off Offset
   * @return Item at offset off
   * @throws IndexOutOfBoundsException for an invalid index.
   */
  public long getLong(A array, int off) throws IndexOutOfBoundsException;

  /**
   * Get the off'th item from the array as byte.
   * 
   * @param array Array to get from
   * @param off Offset
   * @return Item at offset off
   * @throws IndexOutOfBoundsException for an invalid index.
   */
  public byte getByte(A array, int off) throws IndexOutOfBoundsException;
}