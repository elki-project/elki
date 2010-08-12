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
   * @return Deserialized object, and length of data processed
   */
  public Pair<T, Integer> fromByteArray(byte[] data) throws IOException, UnsupportedOperationException;

  /**
   * Serialize the object to a byte array (e.g. disk)
   * 
   * @param obj Object to serialize
   * @param buffer Buffer to serialize to
   * @return number of bytes written
   */
  public int toByteArray(T obj, byte[] buffer) throws IOException, UnsupportedOperationException;

  /**
   * Get the size of the object in bytes.
   * 
   * @param object Object to serialize
   * @return maximum size in serialized form
   */
  public int getByteSize(T object) throws IOException, UnsupportedOperationException;
}