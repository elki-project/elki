package de.lmu.ifi.dbs.elki.persistent;

import java.util.Stack;

/**
 * Abstract class implementing general methods of a PageFile. A PageFile stores
 * objects that implement the <code>Page</code> interface.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has Page
 * @apiviz.has PageFileStatistics
 * 
 * @param <P> Page type
 */
public abstract class AbstractStoringPageFile<P extends Page<P>> extends AbstractPageFile<P> {
  /**
   * A stack holding the empty page ids.
   */
  protected Stack<Integer> emptyPages;

  /**
   * The last page ID.
   */
  protected int nextPageID;

  /**
   * The size of a page in Bytes.
   */
  protected int pageSize;

  /**
   * Creates a new PageFile.
   */
  protected AbstractStoringPageFile(int pageSize) {
    this.emptyPages = new Stack<Integer>();
    this.nextPageID = 0;
    this.pageSize = pageSize;
  }

  /**
   * Sets the id of the given page.
   * 
   * @param page the page to set the id
   */
  @Override
  public Integer setPageID(P page) {
    Integer pageID = page.getPageID();
    if(pageID == null) {
      pageID = getNextEmptyPageID();
      if(pageID == null) {
        pageID = nextPageID++;
      }
      page.setPageID(pageID);
    }
    return pageID;
  }

  /**
   * Deletes the node with the specified id from this file.
   * 
   * @param pageID the id of the node to be deleted
   */
  @Override
  public void deletePage(int pageID) {
    // put id to empty nodes
    emptyPages.push(pageID);
  }

  /**
   * Returns the next empty page id.
   * 
   * @return the next empty page id
   */
  private Integer getNextEmptyPageID() {
    if(!emptyPages.empty()) {
      return emptyPages.pop();
    }
    else {
      return null;
    }
  }

  /**
   * Returns the next page id.
   * 
   * @return the next page id
   */
  @Override
  public int getNextPageID() {
    return nextPageID;
  }

  /**
   * Sets the next page id.
   * 
   * @param nextPageID the next page id to be set
   */
  @Override
  public void setNextPageID(int nextPageID) {
    this.nextPageID = nextPageID;
  }

  /**
   * Get the page size of this page file.
   * 
   * @return page size
   */
  @Override
  public int getPageSize() {
    return pageSize;
  }

  /**
   * Initialize the page file with the given header - return "true" if the file
   * already existed.
   * 
   * @param header Header
   * @return true when the file already existed.
   */
  @Override
  public boolean initialize(PageHeader header) {
    this.pageSize = header.getPageSize();
    return false;
  }

  @Override
  public PageFileStatistics getInnerStatistics() {
    // Default: no nested page file.
    return null;
  }
}