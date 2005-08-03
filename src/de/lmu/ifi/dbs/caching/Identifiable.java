package de.lmu.ifi.dbs.caching;

/**
 * Defines the requirements for an object that can be stored in a cache.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Identifiable {

  /**
   * Returns the unique id of this Identifiable.
   * @return the unique id of this Identifiable
   */
  int getID();
}
