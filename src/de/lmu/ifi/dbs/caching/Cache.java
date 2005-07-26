package de.lmu.ifi.dbs.caching;

/**
 * Defines the requirements for a cache that stores objects implementing the page interface.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Cache {

  /**
   * Retrieves a page from this cache.
   *
   * @param pageID the id of the page to be returned.
   * @return the page associated to the pageID or null if no value with this
   *         key exists in the cache.
   */
  public Page get(int pageID);

  /**
   * Adds a page to this cache.
   *
   * @param page the page to be added
   */
  public void put(Page page);

  /**
   * Removes a page from this cache.
   *
   * @param pageID the id of the page to be removed
   * @return the removed page
   */
  public Page remove(int pageID);

  /**
   * Clears the cache.
   */
  public void clear();

}
