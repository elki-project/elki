package de.lmu.ifi.dbs.elki.persistent;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

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
  public static class ByteSerializer implements ByteBufferSerializer<Byte> {
    /**
     * Constructor. Protected: use static instance!
     */
    protected ByteSerializer() {
      super();
    }

    @Override
    public Byte fromByteBuffer(ByteBuffer buffer) {
      return buffer.get();
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, Byte obj) {
      buffer.put(obj);
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
  public static class ShortSerializer implements ByteBufferSerializer<Short> {
    /**
     * Constructor. Protected: use static instance!
     */
    protected ShortSerializer() {
      super();
    }

    @Override
    public Short fromByteBuffer(ByteBuffer buffer) {
      return buffer.getShort();
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, Short obj) {
      buffer.putShort(obj);
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
  public static class IntegerSerializer implements ByteBufferSerializer<Integer> {
    /**
     * Constructor. Protected: use static instance!
     */
    protected IntegerSerializer() {
      super();
    }

    @Override
    public Integer fromByteBuffer(ByteBuffer buffer) {
      return buffer.getInt();
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, Integer obj) {
      buffer.putInt(obj);
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
  public static class LongSerializer implements ByteBufferSerializer<Long> {
    /**
     * Constructor. Protected: use static instance!
     */
    protected LongSerializer() {
      super();
    }

    @Override
    public Long fromByteBuffer(ByteBuffer buffer) {
      return buffer.getLong();
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, Long obj) {
      buffer.putLong(obj);
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
  public static class FloatSerializer implements ByteBufferSerializer<Float> {
    /**
     * Constructor. Protected: use static instance!
     */
    protected FloatSerializer() {
      super();
    }

    @Override
    public Float fromByteBuffer(ByteBuffer buffer) {
      return buffer.getFloat();
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, Float obj) {
      buffer.putFloat(obj);
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
  public static class DoubleSerializer implements ByteBufferSerializer<Double> {
    /**
     * Constructor. Protected: use static instance!
     */
    protected DoubleSerializer() {
      super();
    }

    @Override
    public Double fromByteBuffer(ByteBuffer buffer) {
      return buffer.getDouble();
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, Double obj) {
      buffer.putDouble(obj);
    }

    @Override
    public int getByteSize(@SuppressWarnings("unused") Double object) {
      return SIZE_DOUBLE;
    }
  }

  /**
   * Serializer for String objects
   * 
   * @author Erich Schubert
   */
  public static class StringSerializer implements ByteBufferSerializer<String> {
    /**
     * Character set to use
     */
    Charset charset = Charset.forName("UTF-8");

    /**
     * Encoder
     */
    CharsetEncoder encoder = charset.newEncoder();

    /**
     * Decoder
     */
    CharsetDecoder decoder = charset.newDecoder();

    /**
     * Constructor. Protected: use static instance!
     */
    protected StringSerializer() {
      super();
    }

    @Override
    public String fromByteBuffer(ByteBuffer buffer) {
      int len = buffer.getInt();
      // Create and limit a view
      ByteBuffer subbuffer = buffer.slice();
      subbuffer.limit(len);
      CharBuffer res;
      try {
        res = decoder.decode(subbuffer);
      }
      catch(CharacterCodingException e) {
        throw new AbortException("String not representable as UTF-8.", e);
      }
      // TODO: assert that the decoding did not yet advance the buffer!
      buffer.position(buffer.position() + len);
      return res.toString();
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, String obj) {
      ByteBuffer data;
      try {
        data = encoder.encode(CharBuffer.wrap(obj));
      }
      catch(CharacterCodingException e) {
        throw new AbortException("String not representable as UTF-8.", e);
      }
      buffer.putInt(data.remaining());
      buffer.put(data);
    }

    @Override
    public int getByteSize(String object) {
      try {
        return SIZE_INT + encoder.encode(CharBuffer.wrap(object)).remaining();
      }
      catch(CharacterCodingException e) {
        throw new AbortException("String not representable as UTF-8.", e);
      }
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

  /**
   * Static instance.
   */
  public static final StringSerializer STRING_SERIALIZER = new StringSerializer();
}