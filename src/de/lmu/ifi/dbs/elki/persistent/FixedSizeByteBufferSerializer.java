package de.lmu.ifi.dbs.elki.persistent;

/**
 * Serializers with a fixed length serialization.
 * 
 * @author Erich Schubert
 * 
 * @param <T> Type
 */
public interface FixedSizeByteBufferSerializer<T> extends ByteBufferSerializer<T> {
  /**
   * Get the fixed size needed by this serializer.
   * 
   * @return Size
   */
  public int getFixedByteSize();
}