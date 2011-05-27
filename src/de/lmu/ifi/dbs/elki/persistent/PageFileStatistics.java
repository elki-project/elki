package de.lmu.ifi.dbs.elki.persistent;

/**
 * Statistics API for a Page File.
 * 
 * See {@link PageFileUtil} for related utility functions for analysing this
 * data!
 * 
 * @author Erich Schubert
 */
public interface PageFileStatistics {
  /**
   * Returns the read I/O-Accesses of this file.
   * 
   * @return Number of physical read I/O accesses
   */
  public long getReadOperations();

  /**
   * Returns the write I/O-Accesses of this file.
   * 
   * @return Number of physical write I/O accesses
   */
  public long getWriteOperations();

  /**
   * Resets the counters for page accesses of this file and flushes the cache.
   */
  public void resetPageAccess();

  /**
   * Get statistics for the inner page file, if present.
   * 
   * @return Inner page file statistics, or null.
   */
  public PageFileStatistics getInnerStatistics();
}
