package de.lmu.ifi.dbs.elki.persistent;

/**
 * Page file interface.
 * 
 * @author Erich Schubert
 * 
 * @param <P> Page file
 */
public interface PageFile<P extends Page<P>> extends PageFileStatistics {
  /**
   * Sets the id of the given page.
   * 
   * @param page the page to set the id
   * @return the page id
   */
  public Integer setPageID(P page);

  /**
   * Writes a page into this file. The method tests if the page has already an
   * id, otherwise a new id is assigned and returned.
   * 
   * @param page the page to be written
   * @return the id of the page
   */
  public int writePage(P page);

  /**
   * Reads the page with the given id from this file.
   * 
   * @param pageID the id of the page to be returned
   * @return the page with the given pageId
   */
  public P readPage(int pageID);

  /**
   * Deletes the node with the specified id from this file.
   * 
   * @param pageID the id of the node to be deleted
   */
  public void deletePage(int pageID);

  /**
   * Closes this file.
   */
  public void close();

  /**
   * Clears this PageFile.
   */
  public void clear();

  /**
   * Returns the next page id.
   * 
   * @return the next page id
   */
  public int getNextPageID();

  /**
   * Sets the next page id.
   * 
   * @param nextPageID the next page id to be set
   */
  public void setNextPageID(int nextPageID);

  /**
   * Get the page size of this page file.
   * 
   * @return page size
   */
  public int getPageSize();

  /**
   * Initialize the page file with the given header - return "true" if the file
   * already existed.
   * 
   * @param header Header
   * @return true when the file already existed.
   */
  public boolean initialize(PageHeader header);
}