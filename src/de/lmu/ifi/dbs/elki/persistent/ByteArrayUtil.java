package de.lmu.ifi.dbs.elki.persistent;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Class with various utilities for manipulating byte arrays.
 * 
 * If you find a reusable copy of this in the Java API, please tell me. Using a
 * {@link java.io.ByteArrayOutputStream} and {@link java.io.DataInputStream}
 * doesn't seem appropriate.
 * 
 * C.f. {@link java.io.DataOutputStream} and
 * {@link java.io.ByteArrayOutputStream}
 * 
 * @author Erich Schubert
 */
public final class ByteArrayUtil {
  /**
   * Size of a byte in bytes.
   */
  public final static int SIZE_BYTE = 1;

  /**
   * Size of a short in bytes.
   */
  public final static int SIZE_SHORT = 2;

  /**
   * Size of an integer in bytes.
   */
  public final static int SIZE_INT = 4;

  /**
   * Size of a long in bytes.
   */
  public final static int SIZE_LONG = 8;

  /**
   * Size of a float in bytes.
   */
  public final static int SIZE_FLOAT = 4;

  /**
   * Size of a double in bytes.
   */
  public final static int SIZE_DOUBLE = 8;

  /**
   * Write a short to the byte array at the given offset.
   * 
   * @param array Array to write to
   * @param offset Offset to write to
   * @param v data
   * @return number of bytes written
   */
  public final static int writeShort(byte[] array, int offset, int v) {
    array[offset + 0] = (byte) (v >>> 8);
    array[offset + 1] = (byte) (v >>> 0);
    return SIZE_SHORT;
  }

  /**
   * Write an integer to the byte array at the given offset.
   * 
   * @param array Array to write to
   * @param offset Offset to write to
   * @param v data
   * @return number of bytes written
   */
  public final static int writeInt(byte[] array, int offset, int v) {
    array[offset + 0] = (byte) (v >>> 24);
    array[offset + 1] = (byte) (v >>> 16);
    array[offset + 2] = (byte) (v >>> 8);
    array[offset + 3] = (byte) (v >>> 0);
    return SIZE_INT;
  }

  /**
   * Write a long to the byte array at the given offset.
   * 
   * @param array Array to write to
   * @param offset Offset to write to
   * @param v data
   * @return number of bytes written
   */
  public final static int writeLong(byte[] array, int offset, long v) {
    array[offset + 0] = (byte) (v >>> 56);
    array[offset + 1] = (byte) (v >>> 48);
    array[offset + 2] = (byte) (v >>> 40);
    array[offset + 3] = (byte) (v >>> 32);
    array[offset + 4] = (byte) (v >>> 24);
    array[offset + 5] = (byte) (v >>> 16);
    array[offset + 6] = (byte) (v >>> 8);
    array[offset + 7] = (byte) (v >>> 0);
    return SIZE_LONG;
  }

  /**
   * Write a float to the byte array at the given offset.
   * 
   * @param array Array to write to
   * @param offset Offset to write to
   * @param v data
   * @return number of bytes written
   */
  public final static int writeFloat(byte[] array, int offset, float v) {
    return writeInt(array, offset, Float.floatToIntBits(v));
  }

  /**
   * Write a double to the byte array at the given offset.
   * 
   * @param array Array to write to
   * @param offset Offset to write to
   * @param v data
   * @return number of bytes written
   */
  public final static int writeDouble(byte[] array, int offset, double v) {
    return writeLong(array, offset, Double.doubleToLongBits(v));
  }

  /**
   * Read a short from the byte array at the given offset.
   * 
   * @param array Array to read from
   * @param offset Offset to read at
   * @return (signed) short
   */
  public final static short readShort(byte[] array, int offset) {
    // First make integers to resolve signed vs. unsigned issues.
    int b0 = array[offset + 0] & 0xFF;
    int b1 = array[offset + 1] & 0xFF;
    return (short) ((b0 << 8) + (b1 << 0));
  }

  /**
   * Read an unsigned short from the byte array at the given offset.
   * 
   * @param array Array to read from
   * @param offset Offset to read at
   * @return short
   */
  public final static int readUnsignedShort(byte[] array, int offset) {
    // First make integers to resolve signed vs. unsigned issues.
    int b0 = array[offset + 0] & 0xFF;
    int b1 = array[offset + 1] & 0xFF;
    return ((b0 << 8) + (b1 << 0));
  }

  /**
   * Read an integer from the byte array at the given offset.
   * 
   * @param array Array to read from
   * @param offset Offset to read at
   * @return data
   */
  public final static int readInt(byte[] array, int offset) {
    // First make integers to resolve signed vs. unsigned issues.
    int b0 = array[offset + 0] & 0xFF;
    int b1 = array[offset + 1] & 0xFF;
    int b2 = array[offset + 2] & 0xFF;
    int b3 = array[offset + 3] & 0xFF;
    return ((b0 << 24) + (b1 << 16) + (b2 << 8) + (b3 << 0));
  }

  /**
   * Read a long from the byte array at the given offset.
   * 
   * @param array Array to read from
   * @param offset Offset to read at
   * @return data
   */
  public final static long readLong(byte[] array, int offset) {
    // First make integers to resolve signed vs. unsigned issues.
    long b0 = array[offset + 0];
    long b1 = array[offset + 1] & 0xFF;
    long b2 = array[offset + 2] & 0xFF;
    long b3 = array[offset + 3] & 0xFF;
    long b4 = array[offset + 4] & 0xFF;
    int b5 = array[offset + 5] & 0xFF;
    int b6 = array[offset + 6] & 0xFF;
    int b7 = array[offset + 7] & 0xFF;
    return ((b0 << 56) + (b1 << 48) + (b2 << 40) + (b3 << 32) + (b4 << 24) + (b5 << 16) + (b6 << 8) + (b7 << 0));
  }

  /**
   * Read a float from the byte array at the given offset.
   * 
   * @param array Array to read from
   * @param offset Offset to read at
   * @return data
   */
  public final static float readFloat(byte[] array, int offset) {
    return Float.intBitsToFloat(readInt(array, offset));
  }

  /**
   * Read a double from the byte array at the given offset.
   * 
   * @param array Array to read from
   * @param offset Offset to read at
   * @return data
   */
  public final static double readDouble(byte[] array, int offset) {
    return Double.longBitsToDouble(readLong(array, offset));
  }

  /**
   * Serializer for byte objects
   * 
   * @author Erich Schubert
   */
  public static class ByteSerializer implements ByteArraySerializer<Byte> {
    /**
     * Constructor. Protected: use static instance!
     */
    protected ByteSerializer() {
      super();
    }

    @Override
    public Pair<Byte, Integer> fromByteArray(byte[] data, int offset) {
      return new Pair<Byte, Integer>(data[offset], SIZE_BYTE);
    }

    @Override
    public int toByteArray(byte[] buffer, int offset, Byte obj) {
      return buffer[offset] = obj;
    }

    @Override
    public int getByteSize(@SuppressWarnings("unused") Byte object) {
      return SIZE_BYTE;
    }
  }

  /**
   * Serializer for short objects
   * 
   * @author Erich Schubert
   */
  public static class ShortSerializer implements ByteArraySerializer<Short> {
    /**
     * Constructor. Protected: use static instance!
     */
    protected ShortSerializer() {
      super();
    }

    @Override
    public Pair<Short, Integer> fromByteArray(byte[] data, int offset) {
      return new Pair<Short, Integer>(readShort(data, offset), SIZE_SHORT);
    }

    @Override
    public int toByteArray(byte[] buffer, int offset, Short obj) {
      return writeShort(buffer, offset, obj);
    }

    @Override
    public int getByteSize(@SuppressWarnings("unused") Short object) {
      return SIZE_SHORT;
    }
  }

  /**
   * Serializer for integer objects
   * 
   * @author Erich Schubert
   */
  public static class IntegerSerializer implements ByteArraySerializer<Integer> {
    /**
     * Constructor. Protected: use static instance!
     */
    protected IntegerSerializer() {
      super();
    }

    @Override
    public Pair<Integer, Integer> fromByteArray(byte[] data, int offset) {
      return new Pair<Integer, Integer>(readInt(data, offset), SIZE_INT);
    }

    @Override
    public int toByteArray(byte[] buffer, int offset, Integer obj) {
      return writeInt(buffer, offset, obj);
    }

    @Override
    public int getByteSize(@SuppressWarnings("unused") Integer object) {
      return SIZE_INT;
    }
  }

  /**
   * Serializer for long objects
   * 
   * @author Erich Schubert
   */
  public static class LongSerializer implements ByteArraySerializer<Long> {
    /**
     * Constructor. Protected: use static instance!
     */
    protected LongSerializer() {
      super();
    }

    @Override
    public Pair<Long, Integer> fromByteArray(byte[] data, int offset) {
      return new Pair<Long, Integer>(readLong(data, offset), SIZE_LONG);
    }

    @Override
    public int toByteArray(byte[] buffer, int offset, Long obj) {
      return writeLong(buffer, offset, obj);
    }

    @Override
    public int getByteSize(@SuppressWarnings("unused") Long object) {
      return SIZE_LONG;
    }
  }

  /**
   * Serializer for float objects
   * 
   * @author Erich Schubert
   */
  public static class FloatSerializer implements ByteArraySerializer<Float> {
    /**
     * Constructor. Protected: use static instance!
     */
    protected FloatSerializer() {
      super();
    }

    @Override
    public Pair<Float, Integer> fromByteArray(byte[] data, int offset) {
      return new Pair<Float, Integer>(readFloat(data, offset), SIZE_FLOAT);
    }

    @Override
    public int toByteArray(byte[] buffer, int offset, Float obj) {
      return writeFloat(buffer, offset, obj);
    }

    @Override
    public int getByteSize(@SuppressWarnings("unused") Float object) {
      return SIZE_FLOAT;
    }
  }

  /**
   * Serializer for double objects
   * 
   * @author Erich Schubert
   */
  public static class DoubleSerializer implements ByteArraySerializer<Double> {
    /**
     * Constructor. Protected: use static instance!
     */
    protected DoubleSerializer() {
      super();
    }

    @Override
    public Pair<Double, Integer> fromByteArray(byte[] data, int offset) {
      return new Pair<Double, Integer>(readDouble(data, offset), SIZE_DOUBLE);
    }

    @Override
    public int toByteArray(byte[] buffer, int offset, Double obj) {
      return writeDouble(buffer, offset, obj);
    }

    @Override
    public int getByteSize(@SuppressWarnings("unused") Double object) {
      return SIZE_DOUBLE;
    }
  }

  /**
   * Static instance.
   */
  public static final ByteSerializer BYTE_SERIALIZER = new ByteSerializer();

  /**
   * Static instance.
   */
  public static final ShortSerializer SHORT_SERIALIZER = new ShortSerializer();

  /**
   * Static instance.
   */
  public static final IntegerSerializer INT_SERIALIZER = new IntegerSerializer();

  /**
   * Static instance.
   */
  public static final LongSerializer LONG_SERIALIZER = new LongSerializer();

  /**
   * Static instance.
   */
  public static final FloatSerializer FLOAT_SERIALIZER = new FloatSerializer();

  /**
   * Static instance.
   */
  public static final DoubleSerializer DOUBLE_SERIALIZER = new DoubleSerializer();
}