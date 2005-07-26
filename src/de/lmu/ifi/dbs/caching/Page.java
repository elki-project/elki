package de.lmu.ifi.dbs.caching;

/**
 * Defines the requirements for an object that can be stored in a cache.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Page {

  /**
   * Returns the unique id of this page.
   * @return the unique id of this page
   */
  int getPageID();
}
