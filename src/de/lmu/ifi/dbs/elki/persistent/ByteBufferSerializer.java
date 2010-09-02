package de.lmu.ifi.dbs.elki.persistent;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Class to convert from and to byte arrays (in index structures)
 * 
 * @author Erich Schubert
 *
 * @param <T> Object type processed
 */
public interface ByteBufferSerializer<T> {
  /**
   * Deserialize an object from a byte buffer (e.g. disk)
   * 
   * @param data Data array to process
   * @return Deserialized object
   */
  public T fromByteBuffer(ByteBuffer data) throws IOException, UnsupportedOperationException;

  /**
   * Serialize the object to a byte array (e.g. disk)
   * 
   * @param buffer Buffer to serialize to
   * @param obj Object to serialize
   */
  public void toByteBuffer(ByteBuffer buffer, T obj) throws IOException, UnsupportedOperationException;

  /**
   * Get the size of the object in bytes.
   * 
   * @param object Object to serialize
   * @return maximum size in serialized form
   */
  public int getByteSize(T object) throws IOException, UnsupportedOperationException;
}