package experimentalcode.erich.cache;

/**
 * Class with various utilities for manipulating byte arrays.
 * 
 * If you find a reusable copy of this in the Java API, please tell me.
 * Using a ByteArrayOutputStream doesn't seem appropriate.
 * 
 * @author Erich Schubert
 */
public final class ByteArrayUtil {
  /**
   * Write a short to the byte array at the given offset.
   * 
   * @param array Array to write to
   * @param offset Offset to write to
   * @param v data
   */
  public final static void writeShort(byte[] array, int offset, int v) {
    array[offset] = (byte) ((v >>> 8) & 0xFF);
    array[offset + 1] = (byte) ((v >>> 0) & 0xFF);
  }

  /**
   * Write an integer to the byte array at the given offset.
   * 
   * @param array Array to write to
   * @param offset Offset to write to
   * @param v data
   */
  public final static void writeInt(byte[] array, int offset, int v) {
    array[offset] = (byte) ((v >>> 24) & 0xFF);
    array[offset + 1] = (byte) ((v >>> 16) & 0xFF);
    array[offset + 2] = (byte) ((v >>> 8) & 0xFF);
    array[offset + 3] = (byte) ((v >>> 0) & 0xFF);
  }

  /**
   * Write a long to the byte array at the given offset.
   * 
   * @param array Array to write to
   * @param offset Offset to write to
   * @param v data
   */
  public final static void writeLong(byte[] array, int offset, long v) {
    array[offset] = (byte) ((v >>> 56) & 0xFF);
    array[offset + 1] = (byte) ((v >>> 48) & 0xFF);
    array[offset + 2] = (byte) ((v >>> 40) & 0xFF);
    array[offset + 3] = (byte) ((v >>> 32) & 0xFF);
    array[offset + 4] = (byte) ((v >>> 24) & 0xFF);
    array[offset + 5] = (byte) ((v >>> 16) & 0xFF);
    array[offset + 6] = (byte) ((v >>> 8) & 0xFF);
    array[offset + 7] = (byte) ((v >>> 0) & 0xFF);
  }

  /**
   * Write a float to the byte array at the given offset.
   * 
   * @param array Array to write to
   * @param offset Offset to write to
   * @param v data
   */
  public final static void writeFloat(byte[] array, int offset, float v) {
    writeInt(array, offset, Float.floatToIntBits(v));
  }

  /**
   * Write a double to the byte array at the given offset.
   * 
   * @param array Array to write to
   * @param offset Offset to write to
   * @param v data
   */
  public final static void writeDouble(byte[] array, int offset, double v) {
    writeLong(array, offset, Double.doubleToLongBits(v));
  }

  /**
   * Read a short from the byte array at the given offset.
   * 
   * @param array Array to read from
   * @param offset Offset to read at
   * @return (signed) short
   */
  public final static short readShort(byte[] array, int offset) {
    return (short) ((array[offset] << 8) + (array[offset + 1] << 0));
  }

  /**
   * Read an unsigned short from the byte array at the given offset.
   * 
   * @param array Array to read from
   * @param offset Offset to read at
   * @return short
   */
  public final static int readUnsignedShort(byte[] array, int offset) {
    return ((array[offset] << 8) + (array[offset + 1] << 0));
  }

  /**
   * Read an integer from the byte array at the given offset.
   * 
   * @param array Array to read from
   * @param offset Offset to read at
   * @return data
   */
  public final static int readInt(byte[] array, int offset) {
    return ((array[offset] << 24) + (array[offset + 1] << 16) + (array[offset + 2] << 8) + (array[offset + 3] << 0));
  }

  /**
   * Read a long from the byte array at the given offset.
   * 
   * @param array Array to read from
   * @param offset Offset to read at
   * @return data
   */
  public final static long readLong(byte[] array, int offset) {
    return ((array[offset] << 56) + (array[offset + 1] << 48) + (array[offset + 2] << 40) + (array[offset + 3] << 32) + (array[offset + 4] << 24) + (array[offset + 5] << 16) + (array[offset + 6] << 8) + (array[offset + 7] << 0));
  }

  /**
   * Read a float from the byte array at the given offset.
   * 
   * @param array Array to read from
   * @param offset Offset to read at
   * @return data
   */
  public final float readFloat(byte[] array, int offset) {
    return Float.intBitsToFloat(readInt(array, offset));
  }

  /**
   * Read a double from the byte array at the given offset.
   * 
   * @param array Array to read from
   * @param offset Offset to read at
   * @return data
   */
  public final double readDouble(byte[] array, int offset) {
    return Double.longBitsToDouble(readLong(array, offset));
  }
}
