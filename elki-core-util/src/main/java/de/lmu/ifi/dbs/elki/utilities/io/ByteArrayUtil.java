/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.utilities.io;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.security.AccessController;
import java.security.PrivilegedAction;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

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
 * @since 0.2
 *
 * @opt nodefillcolor LemonChiffon
 *
 * @composed - - - ByteSerializer
 * @composed - - - ShortSerializer
 * @composed - - - IntegerSerializer
 * @composed - - - LongSerializer
 * @composed - - - FloatSerializer
 * @composed - - - DoubleSerializer
 * @composed - - - StringSerializer
 * @composed - - - VarintSerializer
 */
public final class ByteArrayUtil {
  /**
   * Fake constructor.
   */
  private ByteArrayUtil() {
    // Do not instantiate
  }

  /**
   * Size of a byte in bytes.
   */
  public static final int SIZE_BYTE = 1;

  /**
   * Size of a short in bytes.
   */
  public static final int SIZE_SHORT = 2;

  /**
   * Size of an integer in bytes.
   */
  public static final int SIZE_INT = 4;

  /**
   * Size of a long in bytes.
   */
  public static final int SIZE_LONG = 8;

  /**
   * Size of a float in bytes.
   */
  public static final int SIZE_FLOAT = 4;

  /**
   * Size of a double in bytes.
   */
  public static final int SIZE_DOUBLE = 8;

  /**
   * Write a short to the byte array at the given offset.
   *
   * @param array Array to write to
   * @param offset Offset to write to
   * @param v data
   * @return number of bytes written
   */
  public static int writeShort(byte[] array, int offset, int v) {
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
  public static int writeInt(byte[] array, int offset, int v) {
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
  public static int writeLong(byte[] array, int offset, long v) {
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
  public static int writeFloat(byte[] array, int offset, float v) {
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
  public static int writeDouble(byte[] array, int offset, double v) {
    return writeLong(array, offset, Double.doubleToLongBits(v));
  }

  /**
   * Read a short from the byte array at the given offset.
   *
   * @param array Array to read from
   * @param offset Offset to read at
   * @return (signed) short
   */
  public static short readShort(byte[] array, int offset) {
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
  public static int readUnsignedShort(byte[] array, int offset) {
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
  public static int readInt(byte[] array, int offset) {
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
  public static long readLong(byte[] array, int offset) {
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
  public static float readFloat(byte[] array, int offset) {
    return Float.intBitsToFloat(readInt(array, offset));
  }

  /**
   * Read a double from the byte array at the given offset.
   *
   * @param array Array to read from
   * @param offset Offset to read at
   * @return data
   */
  public static double readDouble(byte[] array, int offset) {
    return Double.longBitsToDouble(readLong(array, offset));
  }

  /**
   * Serializer for byte objects.
   *
   * @author Erich Schubert
   */
  public static final class ByteSerializer implements FixedSizeByteBufferSerializer<Byte> {
    /**
     * Constructor. Protected: use static instance!
     */
    private ByteSerializer() {
      super();
    }

    @Deprecated
    @Override
    public Byte fromByteBuffer(ByteBuffer buffer) {
      return buffer.get();
    }

    @Deprecated
    @Override
    public void toByteBuffer(ByteBuffer buffer, Byte obj) {
      buffer.put(obj);
    }

    @Deprecated
    @Override
    public int getByteSize(Byte object) {
      return getFixedByteSize();
    }

    @Override
    public int getFixedByteSize() {
      return SIZE_BYTE;
    }
  }

  /**
   * Serializer for short objects.
   *
   * @author Erich Schubert
   */
  public static final class ShortSerializer implements FixedSizeByteBufferSerializer<Short> {
    /**
     * Constructor. Protected: use static instance!
     */
    private ShortSerializer() {
      super();
    }

    @Deprecated
    @Override
    public Short fromByteBuffer(ByteBuffer buffer) {
      return buffer.getShort();
    }

    @Deprecated
    @Override
    public void toByteBuffer(ByteBuffer buffer, Short obj) {
      buffer.putShort(obj);
    }

    @Deprecated
    @Override
    public int getByteSize(Short object) {
      return getFixedByteSize();
    }

    @Override
    public int getFixedByteSize() {
      return SIZE_SHORT;
    }
  }

  /**
   * Serializer for integer objects.
   *
   * @author Erich Schubert
   */
  public static final class IntegerSerializer implements FixedSizeByteBufferSerializer<Integer> {
    /**
     * Constructor. Protected: use static instance!
     */
    private IntegerSerializer() {
      super();
    }

    @Deprecated
    @Override
    public Integer fromByteBuffer(ByteBuffer buffer) {
      return buffer.getInt();
    }

    @Deprecated
    @Override
    public void toByteBuffer(ByteBuffer buffer, Integer obj) {
      buffer.putInt(obj);
    }

    @Deprecated
    @Override
    public int getByteSize(Integer object) {
      return getFixedByteSize();
    }

    @Override
    public int getFixedByteSize() {
      return SIZE_INT;
    }
  }

  /**
   * Serializer for long objects.
   *
   * @author Erich Schubert
   */
  public static final class LongSerializer implements FixedSizeByteBufferSerializer<Long> {
    /**
     * Constructor. Protected: use static instance!
     */
    private LongSerializer() {
      super();
    }

    @Deprecated
    @Override
    public Long fromByteBuffer(ByteBuffer buffer) {
      return buffer.getLong();
    }

    @Deprecated
    @Override
    public void toByteBuffer(ByteBuffer buffer, Long obj) {
      buffer.putLong(obj);
    }

    @Deprecated
    @Override
    public int getByteSize(Long object) {
      return getFixedByteSize();
    }

    @Override
    public int getFixedByteSize() {
      return SIZE_LONG;
    }
  }

  /**
   * Serializer for float objects.
   *
   * @author Erich Schubert
   */
  public static final class FloatSerializer implements FixedSizeByteBufferSerializer<Float> {
    /**
     * Constructor. Protected: use static instance!
     */
    private FloatSerializer() {
      super();
    }

    @Deprecated
    @Override
    public Float fromByteBuffer(ByteBuffer buffer) {
      return buffer.getFloat();
    }

    @Deprecated
    @Override
    public void toByteBuffer(ByteBuffer buffer, Float obj) {
      buffer.putFloat(obj);
    }

    @Deprecated
    @Override
    public int getByteSize(Float object) {
      return getFixedByteSize();
    }

    @Override
    public int getFixedByteSize() {
      return SIZE_FLOAT;
    }
  }

  /**
   * Serializer for double objects.
   *
   * @author Erich Schubert
   */
  public static final class DoubleSerializer implements FixedSizeByteBufferSerializer<Double> {
    /**
     * Constructor. Protected: use static instance!
     */
    private DoubleSerializer() {
      super();
    }

    @Deprecated
    @Override
    public Double fromByteBuffer(ByteBuffer buffer) {
      return buffer.getDouble();
    }

    @Deprecated
    @Override
    public void toByteBuffer(ByteBuffer buffer, Double obj) {
      buffer.putDouble(obj);
    }

    @Deprecated
    @Override
    public int getByteSize(Double object) {
      return getFixedByteSize();
    }

    @Override
    public int getFixedByteSize() {
      return SIZE_DOUBLE;
    }
  }

  /**
   * Serializer for String objects.
   *
   * @author Erich Schubert
   */
  public static final class StringSerializer implements ByteBufferSerializer<String> {
    /**
     * Character set to use.
     */
    Charset charset = Charset.forName("UTF-8");

    /**
     * Encoder.
     */
    CharsetEncoder encoder = charset.newEncoder();

    /**
     * Decoder.
     */
    CharsetDecoder decoder = charset.newDecoder();

    /**
     * Constructor. Protected: use static instance!
     */
    private StringSerializer() {
      super();
    }

    @Override
    public String fromByteBuffer(ByteBuffer buffer) throws IOException {
      int len = readUnsignedVarint(buffer);
      // Create and limit a view
      ByteBuffer subbuffer = buffer.slice();
      subbuffer.limit(len);
      CharBuffer res = decoder.decode(subbuffer);
      // TODO: assert that the decoding did not yet advance the buffer!
      buffer.position(buffer.position() + len);
      return res.toString();
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer, String obj) throws IOException {
      ByteBuffer data = encoder.encode(CharBuffer.wrap(obj));
      writeUnsignedVarint(buffer, data.remaining());
      buffer.put(data);
    }

    @Override
    public int getByteSize(String object) throws IOException {
      final int len = encoder.encode(CharBuffer.wrap(object)).remaining();
      return getUnsignedVarintSize(len) + len;
    }
  }

  /**
   * Serializer for Integer objects using a variable size encoding.
   *
   * @author Erich Schubert
   */
  public static final class VarintSerializer implements ByteBufferSerializer<Integer> {
    /**
     * Constructor. Protected: use static instance!
     */
    private VarintSerializer() {
      super();
    }

    @Deprecated
    @Override
    public Integer fromByteBuffer(ByteBuffer buffer) throws IOException {
      return readSignedVarint(buffer);
    }

    @Deprecated
    @Override
    public void toByteBuffer(ByteBuffer buffer, Integer obj) {
      writeSignedVarint(buffer, obj);
    }

    @Deprecated
    @Override
    public int getByteSize(Integer object) {
      return getSignedVarintSize(object);
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

  /**
   * Static instance.
   */
  public static final VarintSerializer VARINT_SERIALIZER = new VarintSerializer();

  /**
   * Write an signed integer using a variable-length encoding.
   *
   * The sign bit is moved to bit 0.
   *
   * Data is always written in 7-bit little-endian, where the 8th bit is the
   * continuation flag.
   *
   * @param buffer Buffer to write to
   * @param val number to write
   */
  public static void writeSignedVarint(ByteBuffer buffer, int val) {
    // Move sign to lowest bit
    writeUnsignedVarint(buffer, (val << 1) ^ (val >> 31));
  }

  /**
   * Write a signed long using a variable-length encoding.
   *
   * The sign bit is moved to bit 0.
   *
   * Data is always written in 7-bit little-endian, where the 8th bit is the
   * continuation flag.
   *
   * @param buffer Buffer to write to
   * @param val number to write
   */
  public static void writeSignedVarintLong(ByteBuffer buffer, long val) {
    // Move sign to lowest bit
    writeUnsignedVarintLong(buffer, (val << 1) ^ (val >> 63));
  }

  /**
   * Write an unsigned integer using a variable-length encoding.
   *
   * Data is always written in 7-bit little-endian, where the 8th bit is the
   * continuation flag.
   *
   * @param buffer Buffer to write to
   * @param val number to write
   */
  public static void writeUnsignedVarint(ByteBuffer buffer, int val) {
    // Extra bytes have the high bit set
    while((val & 0x7F) != val) {
      buffer.put((byte) ((val & 0x7F) | 0x80));
      val >>>= 7;
    }
    // Last byte doesn't have high bit set
    buffer.put((byte) (val & 0x7F));
  }

  /**
   * Write an unsigned long using a variable-length encoding.
   *
   * Data is always written in 7-bit little-endian, where the 8th bit is the
   * continuation flag.
   *
   * Note that for integers, this will result in the same encoding as
   * {@link #writeUnsignedVarint}
   *
   * @param buffer Buffer to write to
   * @param val number to write
   */
  public static void writeUnsignedVarintLong(ByteBuffer buffer, long val) {
    // Extra bytes have the high bit set
    while((val & 0x7F) != val) {
      buffer.put((byte) ((val & 0x7F) | 0x80));
      val >>>= 7;
    }
    // Last byte doesn't have high bit set
    buffer.put((byte) (val & 0x7F));
  }

  /**
   * Write a string to the buffer.
   *
   * See {@link StringSerializer} for details.
   *
   * @param buffer Buffer to write to
   * @param s String to write
   */
  public static void writeString(ByteBuffer buffer, String s) throws IOException {
    if(s == null) {
      s = ""; // Which will be written as Varint 0 = single byte 0.
    }
    ByteArrayUtil.STRING_SERIALIZER.toByteBuffer(buffer, s);
  }

  /**
   * Compute the size of the varint encoding for this signed integer.
   *
   * @param val integer to write
   * @return Encoding size of this integer
   */
  public static int getSignedVarintSize(int val) {
    // Move sign to lowest bit
    return getUnsignedVarintSize((val << 1) ^ (val >> 31));
  }

  /**
   * Compute the size of the varint encoding for this unsigned integer.
   *
   * @param obj integer to write
   * @return Encoding size of this integer
   */
  public static int getUnsignedVarintSize(int obj) {
    int bytes = 1;
    // Extra bytes have the high bit set
    while((obj & 0x7F) != obj) {
      bytes++;
      obj >>>= 7;
    }
    return bytes;
  }

  /**
   * Compute the size of the varint encoding for this signed integer.
   *
   * @param val integer to write
   * @return Encoding size of this integer
   */
  public static int getSignedVarintLongSize(long val) {
    // Move sign to lowest bit
    return getUnsignedVarintLongSize((val << 1) ^ (val >> 31));
  }

  /**
   * Compute the size of the varint encoding for this unsigned integer.
   *
   * @param obj integer to write
   * @return Encoding size of this integer
   */
  public static int getUnsignedVarintLongSize(long obj) {
    int bytes = 1;
    // Extra bytes have the high bit set
    while((obj & 0x7F) != obj) {
      bytes++;
      obj >>>= 7;
    }
    return bytes;
  }

  /**
   * Compute the size of the string after encoding.
   *
   * @param s String to encode
   * @return Byte size
   */
  public static int getStringSize(String s) throws IOException {
    return STRING_SERIALIZER.getByteSize(s);
  }

  /**
   * Read a signed integer.
   *
   * @param buffer Buffer to read from
   * @return Integer value
   */
  public static int readSignedVarint(ByteBuffer buffer) throws IOException {
    final int raw = readUnsignedVarint(buffer);
    return (raw >>> 1) ^ -(raw & 1);
  }

  /**
   * Read an unsigned integer.
   *
   * @param buffer Buffer to read from
   * @return Integer value
   */
  public static int readUnsignedVarint(ByteBuffer buffer) throws IOException {
    int val = 0;
    int bits = 0;
    while(true) {
      final int data = buffer.get();
      val |= (data & 0x7F) << bits;
      if((data & 0x80) == 0) {
        return val;
      }
      bits += 7;
      if(bits > 35) {
        throw new IOException("Variable length quantity is too long for expected integer.");
      }
    }
  }

  /**
   * Read a signed long.
   *
   * @param buffer Buffer to read from
   * @return long value
   */
  public static long readSignedVarintLong(ByteBuffer buffer) throws IOException {
    final long raw = readUnsignedVarintLong(buffer);
    return (raw >>> 1) ^ -(raw & 1);
  }

  /**
   * Read an unsigned long.
   *
   * @param buffer Buffer to read from
   * @return long value
   */
  public static long readUnsignedVarintLong(ByteBuffer buffer) throws IOException {
    long val = 0;
    int bits = 0;
    while(true) {
      final int data = buffer.get();
      val |= (data & 0x7F) << bits;
      if((data & 0x80) == 0) {
        return val;
      }
      bits += 7;
      if(bits > 63) {
        throw new IOException("Variable length quantity is too long for expected integer.");
      }
    }
  }

  /**
   * Read a string from the buffer.
   *
   * Note: this is not 100% symmetric to writeString, as a {@code null} value
   * and the empty string are encoded the same way.
   *
   * @param buffer Buffer to read from.
   * @return Deserialized string
   */
  public static String readString(ByteBuffer buffer) throws IOException {
    return STRING_SERIALIZER.fromByteBuffer(buffer);
  }

  /**
   * Unmap a byte buffer.
   *
   * @param map Byte buffer to unmap.
   */
  public static void unmapByteBuffer(final MappedByteBuffer map) {
    if(map == null) {
      return;
    }
    map.force();
    try {
      if(Runtime.class.getDeclaredMethod("version") != null)
        return; // At later Java, the hack below will not work anymore.
    }
    catch(NoSuchMethodException e) {
      // This is an ugly hack, but all that Java <8 offers to help freeing
      // memory allocated using such buffers.
      // See also: http://bugs.sun.com/view_bug.do?bug_id=4724038
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
        @Override
        public Object run() {
          try {
            Method getCleanerMethod = map.getClass().getMethod("cleaner", new Class[0]);
            if(getCleanerMethod == null) {
              return null;
            }
            getCleanerMethod.setAccessible(true);
            Object cleaner = getCleanerMethod.invoke(map, new Object[0]);
            Method cleanMethod = cleaner.getClass().getMethod("clean");
            if(cleanMethod == null) {
              return null;
            }
            cleanMethod.invoke(cleaner);
          }
          catch(Exception e) {
            LoggingUtil.exception(e);
          }
          return null;
        }
      });
    }
    catch(SecurityException e1) {
      // Ignore.
    }
  }
}
