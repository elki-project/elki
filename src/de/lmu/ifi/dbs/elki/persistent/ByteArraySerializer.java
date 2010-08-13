package de.lmu.ifi.dbs.elki.persistent;

import java.io.IOException;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Class to convert from and to byte arrays (in index structures)
 * 
 * @author Erich Schubert
 *
 * @param <T> Object type processed
 */
public interface ByteArraySerializer<T> {
  /**
   * Deserialize an object from a byte array (e.g. disk)
   * 
   * @param data Data array to process
   * @param offset Offset to start reading at
   * @return Deserialized object, and length of data processed
   */
  public Pair<T, Integer> fromByteArray(byte[] data, int offset) throws IOException, UnsupportedOperationException;

  /**
   * Serialize the object to a byte array (e.g. disk)
   * @param buffer Buffer to serialize to
   * @param offset Starting offset
   * @param obj Object to serialize
   * 
   * @return number of bytes written
   */
  public int toByteArray(byte[] buffer, int offset, T obj) throws IOException, UnsupportedOperationException;

  /**
   * Get the size of the object in bytes.
   * 
   * @param object Object to serialize
   * @return maximum size in serialized form
   */
  public int getByteSize(T object) throws IOException, UnsupportedOperationException;
}