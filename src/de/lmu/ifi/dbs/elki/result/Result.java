package de.lmu.ifi.dbs.elki.result;

/**
 * Interface for all classes that are considered a result.
 * 
 * Note that this interface is currently empty; there are no requirements
 * for objects to be considered a result. At some point there might come
 * requirements such as serializability for storing results.
 * 
 * @author Erich Schubert
 *
 */
public interface Result {
  /**
   * Get a user-understandable name for this result.
   * Defaults may be returned such as "list" for a list result.
   * 
   * @return result name
   */
  public String getName();
}
