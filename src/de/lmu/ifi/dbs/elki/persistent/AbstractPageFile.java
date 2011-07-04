package de.lmu.ifi.dbs.elki.persistent;

/**
 * Abstract base class for the page file API for both caches and true page files
 * (in-memory and on-disk).
 * 
 * @author Erich Schubert
 * 
 * @param <P> page type
 */
public abstract class AbstractPageFile<P extends Page> implements PageFile<P> {
  /**
   * The read I/O-Access of this file.
   */
  protected long readAccess;

  /**
   * The write I/O-Access of this file.
   */
  protected long writeAccess;

  /**
   * Constructor.
   */
  public AbstractPageFile() {
    super();
    this.readAccess = 0;
    this.writeAccess = 0;
  }

  /**
   * Writes a page into this file. The method tests if the page has already an
   * id, otherwise a new id is assigned and returned.
   * 
   * @param page the page to be written
   * @return the id of the page
   */
  @Override
  public final synchronized int writePage(P page) {
    Integer pageid = setPageID(page);
    writePage(pageid, page);
    return pageid;
  }

  /**
   * Perform the actual page write operation.
   * 
   * @param pageid Page id
   * @param page Page to write
   */
  protected abstract void writePage(Integer pageid, P page);

  @Override
  public void close() {
    clear();
  }

  @Override
  public final long getReadOperations() {
    return readAccess;
  }

  @Override
  public final long getWriteOperations() {
    return writeAccess;
  }

  @Override
  public final void resetPageAccess() {
    this.readAccess = 0;
    this.writeAccess = 0;
  }
}